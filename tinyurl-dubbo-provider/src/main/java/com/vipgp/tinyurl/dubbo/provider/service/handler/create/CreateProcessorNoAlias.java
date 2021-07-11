package com.vipgp.tinyurl.dubbo.provider.service.handler.create;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.id.generator.Generatable;
import com.vipgp.tinyurl.dubbo.provider.mq.message.CacheEvent;
import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;
import com.vipgp.tinyurl.dubbo.provider.tasks.AwaitStatus;
import com.vipgp.tinyurl.dubbo.provider.tasks.Checkpoint;
import com.vipgp.tinyurl.dubbo.provider.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 11:28
 */
@Slf4j
@Component("createProcessorNoAlias")
public class CreateProcessorNoAlias extends AbstractCreateHandler {

    @Autowired
    Generatable generator;

    @NacosValue(value = "${tiny.url.id.duplicate.validate.enable}",autoRefreshed = true)
    boolean enableDuplicateIdValidate;

    /**
     * id for raw url
     *
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public Long generateId(String baseUrl, String aliasCode) {
        /**
         * check if id has been used
         */
        // baseurl key, e.g. t.vipgp88.com -> t
        String baseUrlKey =lookupManager.getValue(baseUrl);
        Long id = -1L;
        for(;;) {
            id = generator.generate(baseUrl);

            if(enableDuplicateIdValidate) {
                long start = System.nanoTime();
                String bitKey = BitMapShardingUtil.getBitKey(id, baseUrlKey);
                long index = BitMapShardingUtil.calcIndex(id);
                // system generate alias code, so the input aliasCode will be null, we set it
                aliasCode = Base62Util.encode(id, codeLength);
                boolean isTouched = redisManager.getbit(bitKey, index, baseUrlKey, aliasCode);
                long end = System.nanoTime();
                log.info("id {}, get bit check touch, cost {}ns {}ms", id, end - start, (end - start) / 1000000);

                if (!isTouched) {
                    log.info("id {} untouched", id);
                    return id;
                } else {
                    log.info("id {} touched", id);
                    continue;
                }
            }else {
                log.info("id duplicated validation disabled");
                return id;
            }
        }

    }

    /**
     * format tiny url, keep the alias code which premium user input
     *
     * @param baseUrl
     * @param aliasCodeInput
     * @param aliasCodeEncode
     * @return
     */
    @Override
    public String formatTinyUrl(String baseUrl, String aliasCodeInput, String aliasCodeEncode) {
        return  CommonUtil.appendTinyUrl(baseUrl, aliasCodeEncode);
    }

    /**
     * insert id<->url relation to db
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    @Override
    protected void syncDbCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        // no delay, commit instantly
        tinyManager.addAndRefreshCacheInTransaction(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
        log.info("no delay, commit instantly");
    }



    @Override
    protected ErrorCode doDelayLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        try {
            /**
             * prepare - commit
             * two phase commit
             */
            /**
             * Step 0: commit prepare log
             */
            log.info("do delay log commit begin, id {}", id);
            TxnLogModel model = TxnLogModel.builder(id, baseUrlKey, baseUrl, aliasCode, rawUrl);
            // prepare
            long start = System.nanoTime();
            cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_AWAIT);
            AwaitStatus result = logDelayProcessor.await(model);
            cacheUtil.putCheckpoint(id, Checkpoint.AFTER_AWAIT);
            if (AwaitStatus.TIME_OUT.equals(result)) {
                log.info("id {}, barrier await time out, switch to sync prepare log", id);
                long from = System.nanoTime();
                fileTxnSnapLog.prepare(Arrays.asList(model));
                long to = System.nanoTime();
                log.info("id {}, barrier await time out, switch to sync prepare log, cost {}ns {}ms", id, to - from, (to - from) / 1000000);
            } else if (AwaitStatus.FAIL.equals(result)) {
                log.info("id {}, barrier await exception", id);
                cacheUtil.putCheckpoint(id, Checkpoint.FAIL);
                return ErrorCode.CYCLIC_BARRIER_AWAIT_EXCEPTION;
            } else{
                log.info("id {}, barrier await success", id);
            }
            long end = System.nanoTime();
            log.info("id {}, await commit prepare log, cost {}ns {}ms", id, end - start, (end - start) / 1000000);
            // get xid and offset
            long xid = model.getXid();
            LogCommitEvent event = model.getLogCommitEvent();
            if (xid <= 0 || event == null || event.getOffset() < 0) {
                log.info("commit prepare log fail, id {} xid {} offset {}", model.getId(), xid, event == null ? "null" : event.toString());
                cacheUtil.putCheckpoint(id, Checkpoint.FAIL);
                return ErrorCode.WRITE_AHEAD_PREPARE_LOG_EXCEPTION;
            } else {
                log.info("commit prepare log success, id {}, xid {} offset {}", id, xid, event.toString());
            }

            /**
             * Step 1: set bitmap, xid, tinyurl->rawurl
             */
            start = System.nanoTime();
            cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_REDIS);
            CacheEvent cacheEvent = new CacheEvent();
            String randomValue = String.valueOf(CommonUtil.random6());
            cacheEvent.setLockValue(randomValue);
            cacheEvent = populateCacheEvent(cacheEvent, id, xid, baseUrlKey, aliasCode, rawUrl,
                    newlyTinyUrlKeyExpiredSecond, cacheUtil.getWorkerId());
            // add to redis
            addTinyUrlToCache(cacheEvent);
            cacheUtil.putCheckpoint(id, Checkpoint.AFTER_REDIS);
            end = System.nanoTime();
            log.info("id {}, delay log, add tiny url to cache, cost {}ns {}ms", id, end - start, (end - start) / 1000000);


            /**
             * Step 2: commit log
             */
            // commit, then the transaction should be success
            start = System.nanoTime();
            boolean success = fileTxnSnapLog.commit(event);
            end = System.nanoTime();
            log.info("id {}, delay log, commit C log, cost {}ns {}ms", id, end - start, (end - start) / 1000000);
            if (!success) {
                log.info("id {}, commit log fail", id);
                return ErrorCode.WRITE_AHEAD_COMMIT_LOG_EXCEPTION;
            }

            // async flush data to db based on txn logs
            return ErrorCode.OK;
        }catch (Exception ex){
            log.error("no alias do delay log commit  exception, id {} aliasCode {}",id,aliasCode,ex);
            return ErrorCode.CREATE_TINY_URL_EXCEPTION;
        }
    }

    @Override
    protected void syncLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        /**
         * prepare - commit
         * two phase commit
         */
        log.info("id {}, sync log, commit begin", id);
        TxnLogModel model = TxnLogModel.builder(id, baseUrlKey, baseUrl, aliasCode, rawUrl);
        // prepare
        long start = System.currentTimeMillis();
        cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_PREPARE);
        fileTxnSnapLog.prepare(Arrays.asList(model));
        cacheUtil.putCheckpoint(id, Checkpoint.AFTER_PREPARE);
        long end = System.currentTimeMillis();
        log.info("id {}, sync log, commit P log, cost {}ns {}ms", id, end - start,(end - start)/1000000);
        long xid = model.getXid();
        LogCommitEvent event = model.getLogCommitEvent();
        if (xid <= 0 || event == null || event.getOffset() < 0) {
            cacheUtil.putCheckpoint(id, Checkpoint.FAIL);
            throw new BaseAppException(ErrorCode.WRITE_AHEAD_PREPARE_LOG_EXCEPTION);
        }

        /**
         * Step 0: set bitmap, xid, tinyurl->rawurl
         */
        start = System.currentTimeMillis();
        cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_REDIS);
        CacheEvent cacheEvent = new CacheEvent();
        String randomValue = String.valueOf(CommonUtil.random6());
        cacheEvent.setLockValue(randomValue);
        cacheEvent = populateCacheEvent(cacheEvent, id, xid, baseUrlKey, aliasCode, rawUrl,
                newlyTinyUrlKeyExpiredSecond, cacheUtil.getWorkerId());
        addTinyUrlToCache(cacheEvent);
        cacheUtil.putCheckpoint(id, Checkpoint.AFTER_REDIS);
        end = System.currentTimeMillis();
        log.info("id {}, sync log, add tiny url to cache, cost {}ns {}ms", id, end - start,(end - start)/1000000);

        // commit, then the transaction should be success
        start = System.currentTimeMillis();
        boolean success = fileTxnSnapLog.commit(model.getLogCommitEvent());
        end = System.currentTimeMillis();
        log.info("id {}, sync log, commit C log, cost {}ns {}ms", id, end - start,(end - start)/1000000);
        if (!success) {
            throw new BaseAppException(ErrorCode.WRITE_AHEAD_COMMIT_LOG_EXCEPTION);
        }
        log.info("id {}, sync log, commit end", id);

    }
}
