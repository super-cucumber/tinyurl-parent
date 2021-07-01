package com.vipgp.tinyurl.dubbo.provider.service.handler.create;

import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.mq.message.CacheEvent;
import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;
import com.vipgp.tinyurl.dubbo.provider.service.handler.precreate.DuplicateAliasValidation;
import com.vipgp.tinyurl.dubbo.provider.tasks.AwaitStatus;
import com.vipgp.tinyurl.dubbo.provider.tasks.Checkpoint;
import com.vipgp.tinyurl.dubbo.provider.util.Base62Util;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 11:28
 */
@Slf4j
@Component("createProcessorWithAlias")
public class CreateProcessorWithAlias extends AbstractCreateHandler {

    @Autowired
    private DuplicateAliasValidation duplicateAliasValidation;

    /**
     * id for raw url
     *
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public Long generateId(String baseUrl, String aliasCode) {
        long id = Base62Util.decode(aliasCode);
        log.info("alias code: {}, transfer to id: {}" ,aliasCode,id);

        return id;
    }

    /**
     * format tiny url, keep the alias code which premium user inputted
     * e.g. 'weibo' user inputted code, the aliasCodeEncode will be '00weibo', it is not good ux
     *
     * @param baseUrl
     * @param aliasCodeInput
     * @param aliasCodeEncode
     * @return
     */
    @Override
    public String formatTinyUrl(String baseUrl, String aliasCodeInput, String aliasCodeEncode) {
        return  CommonUtil.appendTinyUrl(baseUrl, aliasCodeInput);
    }

    /**
     * optimizing distributed lock to local lock, should meet the below two conditions:
     * 1. the alias code user inputted should be in different range id with the auto generate id by system,
     * e.g. alias code inputted in [1-5] length, auto id generated in [1-7] length,exclude [1-5]
     * 2. shard the same alias code to the same server
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    @Override
    protected void syncDbCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        log.info("store to db with alias use lock begin");
        BaseResult result= duplicateAliasValidation.doProcess(baseUrl, rawUrl, aliasCode);
        if(result!=null){
            log.info("store to db with alias before use lock, id touched");
            throw new BaseAppException(result.getErrorCode(),result.getErrorMessage());
        }

        String lockKey=CommonUtil.getLockKey(baseUrlKey, aliasCode);
        // lock anytime, no matter what way to run, sync or async
        String randomValue=String.valueOf(CommonUtil.random6());
        try {
            boolean isSuccess = redisManager.lock(lockKey, randomValue, tinyUrlLockExpired);
            if (isSuccess) {
                log.info("lock touched aliasCode {} randomValue {}",aliasCode,randomValue);
                result = duplicateAliasValidation.doProcess(baseUrl, rawUrl, aliasCode);
                if (result != null) {
                    log.info("store to db with alias after use lock, id touched");
                    throw new BaseAppException(result.getErrorCode(), result.getErrorMessage());
                }
                // no delay, commit instantly
                tinyManager.addAndRefreshCacheInTransaction(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
                log.info("no delay, commit instantly");
            }else {
                log.info("lock missed aliasCode {} randomValue {}",aliasCode,randomValue);
                throw new BaseAppException(ErrorCode.LOCK_IS_BUSY);
            }
        }finally {
            redisManager.unlock(lockKey,randomValue);
        }

        log.info("store to db with alias use lock end");
    }


    @Override
    protected void syncLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        log.info("asynDbCommit -> syncLogCommit -> begin, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
        BaseResult result = duplicateAliasValidation.doProcess(baseUrl, rawUrl, aliasCode);
        if (result != null) {
            log.info("asynDbCommit -> syncLogCommit -> id already used, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
            throw new BaseAppException(result.getErrorCode(), result.getErrorMessage());
        }
        /**
         * Step 0: lock xid in local env, xid should be putted in sequence
         */
        boolean isLockBusy = false;
        String lockKey = CommonUtil.getLockKey(baseUrlKey, aliasCode);
        CacheEvent cacheEvent = new CacheEvent();
        cacheEvent.setLockRelease(true);
        String randomValue = String.valueOf(CommonUtil.random6());
        cacheEvent.setLockValue(randomValue);
        try {
            // lock alias code, keep off duplicate in distributed env
            boolean isSuccess = redisManager.lock(lockKey, randomValue, tinyUrlLockExpired);
            if (isSuccess) {
                log.info("asynDbCommit -> syncLogCommit -> lock get, aliasCode {} baseUrlKey {} lockValue {}", aliasCode, baseUrlKey, randomValue);
                result = duplicateAliasValidation.doProcess(baseUrl, rawUrl, aliasCode);
                if (result != null) {
                    log.info("asynDbCommit -> syncLogCommit -> check again, id already used, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
                    throw new BaseAppException(result.getErrorCode(), result.getErrorMessage());
                }
                // two phase commit
                // prepare
                TxnLogModel model = TxnLogModel.builder(id, baseUrlKey, baseUrl, aliasCode, rawUrl);
                cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_PREPARE);
                fileTxnSnapLog.prepare(Arrays.asList(model));
                cacheUtil.putCheckpoint(id, Checkpoint.AFTER_PREPARE);
                long xid = model.getXid();
                LogCommitEvent event = model.getLogCommitEvent();
                if (xid <= 0 || event == null || event.getOffset() < 0) {
                    cacheUtil.putCheckpoint(id, Checkpoint.FAIL);
                    throw new BaseAppException(ErrorCode.WRITE_AHEAD_PREPARE_LOG_EXCEPTION);
                }

                /**
                 * Step 0: set bitmap, xid, tinyurl->rawurl
                 */
                cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_REDIS);
                cacheEvent = populateCacheEvent(cacheEvent, id, xid, baseUrlKey, aliasCode, rawUrl,
                        newlyTinyUrlKeyExpiredSecond, cacheUtil.getWorkerId());
                addTinyUrlToCache(cacheEvent);
                cacheUtil.putCheckpoint(id, Checkpoint.AFTER_REDIS);

                // commit
                boolean success = fileTxnSnapLog.commit(model.getLogCommitEvent());
                if (!success) {
                    throw new BaseAppException(ErrorCode.WRITE_AHEAD_COMMIT_LOG_EXCEPTION);
                }
            } else {
                log.info("asynDbCommit -> syncLogCommit -> lock miss, race fail, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
                isLockBusy = true;
                throw new BaseAppException(ErrorCode.LOCK_IS_BUSY);
            }
        } finally {
            log.info("asynDbCommit -> syncLogCommit -> finally, aliasCode {} baseUrlKey {} isLockBusy {} isLockRelease {}",
                    aliasCode, baseUrlKey, isLockBusy, cacheEvent.isLockRelease());
            if (!isLockBusy && cacheEvent.isLockRelease()) {
                log.info("asynDbCommit -> syncLogCommit -> finally, lock released, aliasCode {} baseUrlKey {} lockKey {} lockValue {}",
                        aliasCode, baseUrlKey, lockKey, randomValue);
                redisManager.unlock(lockKey, randomValue);
            } else if (!cacheEvent.isLockRelease()) {
                redisManager.extendLock(lockKey, randomValue, tinyUrlLockExtendExpired);
                log.info("asynDbCommit -> syncLogCommit -> finally, lock unreleased, aliasCode {} baseUrlKey {} lockKey {} lockValue {}",
                        aliasCode, baseUrlKey, lockKey, randomValue);
            } else {
                log.info("asynDbCommit -> syncLogCommit -> finally, lock no need to released, lock key {} lock value {}", lockKey, randomValue);
            }
        }


    }


    @Override
    protected ErrorCode doDelayLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        log.info("asynDbCommit -> doDelayLogCommit -> begin, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
        BaseResult result= duplicateAliasValidation.doProcess(baseUrl, rawUrl, aliasCode);
        if(result!=null){
            log.info("asynDbCommit -> doDelayLogCommit -> id already used, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
            return ErrorCode.getErrorCode(result.getErrorCode());
        }

        boolean isLockBusy=false;
        String lockKey=CommonUtil.getLockKey(baseUrlKey, aliasCode);
        // roll back event
        CacheEvent cacheEvent=new CacheEvent();
        cacheEvent.setLockRelease(true);
        String randomValue = String.valueOf(CommonUtil.random6());
        cacheEvent.setLockValue(randomValue);

        try {
            // lock alias code, keep off duplicate in distributed env
            boolean isSuccess = redisManager.lock(lockKey, randomValue, tinyUrlLockExpired);
            if (isSuccess) {
                log.info("asynDbCommit -> doDelayLogCommit -> lock get, aliasCode {} baseUrlKey {} lockValue {}", aliasCode, baseUrlKey, randomValue);
                result = duplicateAliasValidation.doProcess(baseUrl, rawUrl, aliasCode);
                if (result != null) {
                    log.info("asynDbCommit -> doDelayLogCommit -> check again, id already used, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
                    return ErrorCode.getErrorCode(result.getErrorCode());
                }

                /**
                 * Step 0: two phase commit, prepare
                 */
                TxnLogModel model = TxnLogModel.builder(id, baseUrlKey, baseUrl, aliasCode, rawUrl);
                cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_AWAIT);
                AwaitStatus awaitStatus = logDelayProcessor.await(model);
                cacheUtil.putCheckpoint(id, Checkpoint.AFTER_AWAIT);
                if (AwaitStatus.TIME_OUT.equals(awaitStatus)) {
                    log.info("asynDbCommit -> doDelayLogCommit ->cyclic barrier await time out, switch to sync prepare log");
                    fileTxnSnapLog.prepare(Arrays.asList(model));
                } else if (AwaitStatus.FAIL.equals(awaitStatus)) {
                    log.info("asynDbCommit -> doDelayLogCommit ->cyclic barrier await exception");
                    return ErrorCode.CYCLIC_BARRIER_AWAIT_EXCEPTION;
                } else {
                    log.info("asynDbCommit -> doDelayLogCommit ->cyclic barrier await success");
                }

                // get xid and offset
                long xid = model.getXid();
                LogCommitEvent event = model.getLogCommitEvent();
                if (xid <= 0 || event == null || event.getOffset() < 0) {
                    log.info("commit prepare log fail, xid {} offset {}", xid, event == null ? "null" : event.toString());
                    return ErrorCode.WRITE_AHEAD_PREPARE_LOG_EXCEPTION;
                } else {
                    log.info("commit prepare log success, xid {} offset {}", xid, event.toString());
                }

                /**
                 * Step 1: set bitmap, xid, tinyurl->rawurl
                 */
                cacheUtil.putCheckpoint(id, Checkpoint.BEFORE_REDIS);
                cacheEvent = populateCacheEvent(cacheEvent, id, xid, baseUrlKey, aliasCode, rawUrl,
                        newlyTinyUrlKeyExpiredSecond, cacheUtil.getWorkerId());
                addTinyUrlToCache(cacheEvent);
                cacheUtil.putCheckpoint(id, Checkpoint.AFTER_REDIS);

                /**
                 * Step 2: commit log
                 */
                boolean success = fileTxnSnapLog.commit(model.getLogCommitEvent());
                if (!success) {
                    return ErrorCode.WRITE_AHEAD_COMMIT_LOG_EXCEPTION;
                }

            } else {
                log.info("asynDbCommit -> doDelayLogCommit -> lock miss, race fail, aliasCode {} baseUrlKey {}", aliasCode, baseUrlKey);
                isLockBusy = true;
                return ErrorCode.LOCK_IS_BUSY;
            }
        }catch (Exception ex){
            log.error("with alias do delay log commit  exception, id {} aliasCode {}",id,aliasCode,ex);
            return ErrorCode.CREATE_TINY_URL_EXCEPTION;
        } finally {
            log.info("asynDbCommit -> doDelayLogCommit -> finally, aliasCode {} baseUrlKey {} isLockBusy {} isLockRelease {}",
                    aliasCode, baseUrlKey, isLockBusy, cacheEvent.isLockRelease());
            if (!isLockBusy && cacheEvent.isLockRelease()) {
                log.info("asynDbCommit -> doDelayLogCommit -> finally, lock released, aliasCode {} baseUrlKey {} lockKey {} lockValue {}",
                        aliasCode, baseUrlKey, lockKey, randomValue);
                redisManager.unlock(lockKey, randomValue);
            } else if(!cacheEvent.isLockRelease()) {
                // extend the lock ensure the consumer has the time to consume
                redisManager.extendLock(lockKey,randomValue,tinyUrlLockExtendExpired);
                log.info("asynDbCommit -> doDelayLogCommit -> finally, lock unreleased, aliasCode {} baseUrlKey {} lockKey {} lockValue {}",
                        aliasCode, baseUrlKey, lockKey, randomValue);
            }else {
                log.info("asynDbCommit -> doDelayLogCommit -> finally, lock no need to released, lock key {} lock value {}", lockKey, randomValue);
            }
        }

        return ErrorCode.OK;

    }
}
