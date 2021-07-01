package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.persistence.Const;
import com.vipgp.tinyurl.dubbo.provider.persistence.FileTxnSnapLog;
import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;
import com.vipgp.tinyurl.dubbo.provider.system.HealthMonitor;
import com.vipgp.tinyurl.dubbo.provider.util.BizUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/8 18:38
 */
@Slf4j
@Component
public class LogsFlushToDbTask {

    @Autowired
    FileTxnSnapLog txnSnapLog;
    @Autowired
    TinyManager tinyManager;

    @Autowired
    RedisManager redisManager;
    @Autowired
    CacheUtil cacheUtil;

    @Autowired
    HealthMonitor healthMonitor;

    /**
     * delay ms insert tinyurl to db, if 0, then insert to db instant
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.delay:1000}",autoRefreshed = true)
    private Integer syncDelayMillis;

    @NacosValue(value = "${tiny.url.db.commit.sync}",autoRefreshed = true)
    private boolean dbCommitSync;

    /**
     * half random strategy
     */
    private Integer actualSyncDelayMillis;


    /**
     * flush db
     */
    private ScheduledThreadPoolExecutor executor;

    @PostConstruct
    private void init() {
        if(!dbCommitSync) {
            if (syncDelayMillis == null || syncDelayMillis <= 0) {
                syncDelayMillis = 3000;
            }
            actualSyncDelayMillis = syncDelayMillis / 2 + new Random().nextInt(syncDelayMillis / 2);
            executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Logs-Flush-To-Db"));
            executor.scheduleAtFixedRate(new LogsFlushToDb(), 10000, actualSyncDelayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private class LogsFlushToDb implements Runnable{

        @Override
        public synchronized void run() {
            try {

                if(!healthMonitor.isRecoveredAlready()){
                    log.info("the server is recovering, do wait");
                    return;
                }

                List<String> txns = txnSnapLog.getTxnsToFlush();
                if (CollectionUtils.isEmpty(txns)) {
                    log.info("no txn log files wait to flush to db");
                    return;
                } else {
                    log.info("there are {} txn log files wait to flush to db", txns.size());
                }

                // lastXid in snapshot
                long lastXid = txnSnapLog.getLastXidInSnapShot();
                // get first
                String fileName = txns.get(0);
                int preSize=txns.size();
                List<TxnLogModel> logs = txnSnapLog.extractTxnLogsAfter(fileName, lastXid);
                FilterResult result=new FilterResult();
                if (CollectionUtils.isEmpty(logs)) {
                    log.info("no txn logs wait to flush to db in {}, lastXid {}", fileName, lastXid);
                } else {
                    // filter valid txn
                    result=filterConclusiveLogs(logs);
                    List<TxnLogModel> validTxnLogs =result.validTxnLogs;
                    if (CollectionUtils.isEmpty(validTxnLogs)) {
                        log.info("{} txn logs, but no txn valid logs wait to flush to db in {}", logs.size(), fileName);
                    } else {
                        log.info("there are {} txn logs need flush to db", validTxnLogs.size());
                        List<TinyRawUrlRelDO> target = BizUtil.copyFrom(validTxnLogs);
                        // the last one is the max one
                        long maxXid = validTxnLogs.get(validTxnLogs.size() - 1).getXid();
                        tinyManager.batchInsert(target, maxXid);
                        log.info("{} records in {} have been flushed to db, maxXid {}, waiting flush file count {}",
                                target.size(), fileName, maxXid, txns.size());
                    }
                }

                // it is possible that the file which has been extracted is written to end,
                // but that time when calling extractTxnLogsAfter the file is still writable,
                // so extractTxnLogsAfter has not fetched all the logs at that time,
                // in this case we can not remove the file from waiting flush list
                int currentSize=txns.size();
                boolean isOutstanding=currentSize > preSize;
                // next file has been touched, so it indicates that the last file is finished, there is no more writing
                if (txns.size() > 1 && !isOutstanding && !result.isOngoing) {
                    // remove the first one which has been flushed to db completely
                    txnSnapLog.removeTxnFromFlushList(0);
                    log.info("remove the first txn file, total txn files {}", txns.size());
                }
            }catch (Exception ex){
                log.error("logs flush to db exception", ex);
            }
        }
    }


    /**
     * extract conclusive logs, e.g. success/fail/rollback
     * stop and return when reach uncertain log, e.g. ongoing
     * @param source
     * @return
     */
    private FilterResult filterConclusiveLogs(List<TxnLogModel> source){
        List<TxnLogModel> validTxnLogs=new ArrayList<>(source.size());
        FilterResult result=new FilterResult();
        result.validTxnLogs=validTxnLogs;
        for (TxnLogModel item : source) {
            if(item.getStatus() == Const.STATUS_COMMIT){
                log.info("id {}, xid {}, C status, add to valid list", item.getId(),item.getXid());
                validTxnLogs.add(item);
            }else if(item.getStatus() == Const.STATUS_PREPARE) {
                String key= CommonUtil.getTxnLogEndKey(item.getBaseUrlKey(), item.getAliasCode(), cacheUtil.getWorkerId(),item.getXid());
                String value=redisManager.queryXid(key,item.getBaseUrlKey(),item.getAliasCode());
                // redis has been updated, then the txn is valid
                if(!StringUtils.isEmpty(value)){
                    log.info("id {}, xid {}, P status, add to valid list", item.getId(), item.getXid());
                    validTxnLogs.add(item);
                }else {
                    String checkpoint= cacheUtil.getCheckpoint(item.getId());
                    if(checkOngoing(checkpoint)){
                        //halt on redis operations, put away and return
                        log.info("checkpoint {} xid {} halt on redis operations, put away and return", checkpoint, item.getXid());
                        result.isOngoing=true;
                       return result;
                    }else {
                        log.info("checkpoint {} data {} redis has not updated, should be rollback", checkpoint, item.toString());
                    }
                }
            }else {
                log.error("exception: unknown status, data {}",item.toString());
            }
        }

        return result;
    }

    private boolean checkOngoing(String checkpoint){
        if(String.valueOf(Checkpoint.FAIL.ordinal()).equals(checkpoint)){
            return false;
        }

        if(String.valueOf(Checkpoint.BEFORE_AWAIT.ordinal()).equals(checkpoint)
                ||String.valueOf(Checkpoint.AFTER_AWAIT.ordinal()).equals(checkpoint)
                ||String.valueOf(Checkpoint.BEFORE_PREPARE.ordinal()).equals(checkpoint)
                ||String.valueOf(Checkpoint.AFTER_PREPARE.ordinal()).equals(checkpoint)
                ||String.valueOf(Checkpoint.BEFORE_REDIS.ordinal()).equals(checkpoint)){
            return true;
        }

        return false;
    }

    private class FilterResult{
        List<TxnLogModel> validTxnLogs;
        boolean isOngoing;
    }
}
