package com.vipgp.tinyurl.dubbo.provider.manager.impl;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;
import com.vipgp.tinyurl.dubbo.provider.exception.RecoverException;
import com.vipgp.tinyurl.dubbo.provider.manager.RecoverManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.mq.RocketMQProcessor;
import com.vipgp.tinyurl.dubbo.provider.persistence.Const;
import com.vipgp.tinyurl.dubbo.provider.persistence.FileTxnSnapLog;
import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;
import com.vipgp.tinyurl.dubbo.provider.system.HealthMonitor;
import com.vipgp.tinyurl.dubbo.provider.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/21 16:11
 */
@Slf4j
@Component
public class RecoverManagerImpl implements RecoverManager {

    @Autowired
    private TinyManager tinyManager;
    @Autowired
    private FileTxnSnapLog txnSnapLog;
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private RocketMQProcessor rocketMQProcessor;
    @Autowired
    private HealthMonitor healthMonitor;

    @NacosValue(value = "${tiny.url.length}",autoRefreshed = true)
    int codeLength;


    /**
     * crash safe base on txn and snapshot
     */
    @Override
    public synchronized int recover() throws IOException{
        /**
         * Step 0: the server is not ready yet
         */
        healthMonitor.unhealth(Constants.CACHE_KEY_RECOVER,"be recovering, the server is not ready yet");

        /**
         * Step 1: ensure all rollback rocketmq message have been consumed, this is the key step to keep data consistency
         */
        if(rocketMQProcessor.isNeedWaitMessageConsumed()){
            CommonUtil.sleep(1000*5);
        }

        /**
         * Step 2: compare lastXid in snapshot and max xid in txn，
         *  case 1: txn.xid > snapshot.lastXid, then there are records that have not been flushed to db should been recovered
         *  case 2: txn.xid = snapshot.lastXid, then all records had been flushed, do nothing
         *  case 3: txn.xid < snapshot.lastXid, then there are exceptions, throw exception, the server start failed
         */
        // lastXid in snapshot
        long lastXid = txnSnapLog.getLastXidInSnapShot();
        // logged xid in txn log
        long maxXid = txnSnapLog.getLastXidInTxn();
        // set base xid, no duplicate xid in one server, reentrant
        txnSnapLog.setBaseXid(maxXid);

        if (lastXid == maxXid) {
            log.info("all are flush to db, no need recover");
        }else {
            if (maxXid < lastXid) {
                log.error("lastXid {} is bigger than maxXid {}, there is exception in some way, start up failed", lastXid, maxXid);
                throw new RecoverException("lastXid is bigger than maxXid");
            }

            // recover
            if (maxXid > lastXid) {
               return replay(lastXid,maxXid);
            }
        }

        healthMonitor.getwell(Constants.CACHE_KEY_RECOVER);
        return 0;
    }


    private int replay(long lastXid, long maxXid) {
        log.info("replay begin, lastXid {} maxXid {}", lastXid,maxXid);
        /**
         * Step 1: find add the txn logs which xid is larger than lastXid
         */
        List<TxnLogModel> source = txnSnapLog.extractTxnLogsAfter(lastXid);
        if (CollectionUtils.isEmpty(source)) {
            log.error("there should have txn logs as lastXid is smaller than maxXid");
            throw new RecoverException("there should have txn logs");
        }

        /**
         * Step 2: rollback the txn logs which no commit status and redis has not updated
         */
        List<TxnLogModel> validTxnLogs = filterActiveLogs(source);
        if (!CollectionUtils.isEmpty(validTxnLogs)) {
            /**
             * Step 3: sync batch insert and take snapshot, should be in one transaction, then can be reentrant
             */
            log.info("there are {} txn logs need flush to db", validTxnLogs.size());
            List<TinyRawUrlRelDO> target = BizUtil.copyFrom(validTxnLogs);
            tinyManager.batchInsert(target, maxXid);

            healthMonitor.getwell(Constants.CACHE_KEY_RECOVER);
            return target.size();
        } else {
            log.info("there are no valid txn logs");
        }

        log.info("replay end");

        healthMonitor.getwell(Constants.CACHE_KEY_RECOVER);
        return 0;
    }


    private List<TxnLogModel> filterActiveLogs(List<TxnLogModel> source){
        List<TxnLogModel> validTxnLogs=new ArrayList<>(source.size());
        for (TxnLogModel item : source) {
            if(item.getStatus() == Const.STATUS_COMMIT){
                validTxnLogs.add(item);
            }else if(item.getStatus() == Const.STATUS_PREPARE) {
                String key= CommonUtil.getTxnLogKey(item.getBaseUrlKey(), item.getAliasCode(), cacheUtil.getWorkerId(),item.getXid());
                String value=redisManager.queryXid(key,item.getBaseUrlKey(),item.getAliasCode());
                // redis has been updated, then the txn is valid
                if(!StringUtils.isEmpty(value)){
                    validTxnLogs.add(item);
                    setbitIfAbsent(item);
                }else {
                    log.info("data {} redis has not updated, should be rollback",item.toString());
                }
            }
        }

        return validTxnLogs;
    }

    /**
     * set bitmap if it is absent, this scenario only can happen in below:
     * it is crashed when push item to rocketmq
     * @param item
     */
    private void setbitIfAbsent(TxnLogModel item){

        String bitKey = BitMapShardingUtil.getBitKey(item.getId(), item.getBaseUrlKey());
        long index = BitMapShardingUtil.calcIndex(item.getId());
        // system generate alias code, so the input aliasCode will be null, we set it
        String aliasCode = Base62Util.encode(item.getId(), codeLength);
        boolean isTouched = redisManager.getbit(bitKey, index, item.getBaseUrlKey(), aliasCode);
        if(!isTouched){
            redisManager.setbit(bitKey, index, true);
        }

    }

}
