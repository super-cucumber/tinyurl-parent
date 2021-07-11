package com.vipgp.tinyurl.dubbo.provider.service.handler.create;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.mq.RocketMQProcessor;
import com.vipgp.tinyurl.dubbo.provider.mq.message.CacheEvent;
import com.vipgp.tinyurl.dubbo.provider.persistence.FileTxnSnapLog;
import com.vipgp.tinyurl.dubbo.provider.system.HealthMonitor;
import com.vipgp.tinyurl.dubbo.provider.tasks.Checkpoint;
import com.vipgp.tinyurl.dubbo.provider.tasks.DbCommitProcessor;
import com.vipgp.tinyurl.dubbo.provider.tasks.PLogDelayProcessor;
import com.vipgp.tinyurl.dubbo.provider.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.skywalking.apm.toolkit.trace.SupplierWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 11:07
 */
@Slf4j
@Component
public abstract class AbstractCreateHandler implements CreateProcessor{

    @Autowired
    protected RedisManager redisManager;

    @Autowired
    protected CacheUtil cacheUtil;

    @Autowired
    protected TinyManager tinyManager;
    @Autowired
    private DbCommitProcessor delayTinyManager;

    @Autowired
    protected LookupManager lookupManager;
    @Autowired
    protected DefaultMQProducer producer;
    @Autowired
    protected PLogDelayProcessor logDelayProcessor;
    @Autowired
    protected HealthMonitor healthMonitor;

    @Autowired
    protected RocketMQProcessor rocketMQProcessor;

    @NacosValue(value = "${tiny.url.length}", autoRefreshed = true)
    protected int codeLength;

    @NacosValue(value = "${redis.identical.raw.url.expired}",autoRefreshed = true)
    protected long rawUrlKeyExpiredSecond;

    @NacosValue(value = "${redis.newly.tiny.url.expired}",autoRefreshed = true)
    protected long newlyTinyUrlKeyExpiredSecond;

    @NacosValue(value = "${redis.tiny.url.lock.expired:3}",autoRefreshed = true)
    protected long tinyUrlLockExpired;
    @NacosValue(value = "${redis.tiny.url.lock.extend.expired:4320}",autoRefreshed = true)
    protected long tinyUrlLockExtendExpired;

    /**
     * 延迟多少毫秒后才批量插入
     * 这两个条件是或的关系
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.delay}",autoRefreshed = true)
    private Integer groupCommitSyncDelay;

    @NacosValue(value = "${tiny.url.db.commit.sync}",autoRefreshed = true)
    private boolean dbCommitSync;

    /**
     * 累积多少次以后才批量插入
     * 这两个条件是或的关系
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.no.delay.count}",autoRefreshed = true)
    private Integer groupCommitSyncNoDelayCount;

    @NacosValue(value = "${rocketmq.send.fail.retry.times}",autoRefreshed = true)
    private Integer retryTimes;

    @NacosValue(value = "${txn.prepare.log.delay.commit}",autoRefreshed = true)
    private boolean logDelayCommit;

    @Autowired
    protected FileTxnSnapLog fileTxnSnapLog;


    /**
     * id for raw url
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    protected abstract Long generateId(String baseUrl, String aliasCode);

    /**
     * format tiny url, keep the alias code which premium user input
     *
     * @param baseUrl
     * @param aliasCodeInput
     * @param aliasCodeEncode
     * @return
     */
    protected abstract String formatTinyUrl(String baseUrl, String aliasCodeInput, String aliasCodeEncode);

    /**
     * insert id<->url relation to db
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    protected abstract void syncDbCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode);

    protected abstract void syncLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode);

    protected abstract ErrorCode doDelayLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode);

    protected CacheEvent populateCacheEvent(CacheEvent cacheEvent, Long id,long xid,String baseUrlKey, String aliasCodeEncode,
            String rawUrl, long newlyTinyUrlKeyExpiredSecond,String workerId){
        cacheEvent.setId(id);
        cacheEvent.setXid(xid);
        cacheEvent.setBaseUrlKey(baseUrlKey);
        cacheEvent.setAliasCode(aliasCodeEncode);
        cacheEvent.setRawUrl(rawUrl);
        cacheEvent.setNewlyTinyUrlKeyExpiredSecond(newlyTinyUrlKeyExpiredSecond);
        cacheEvent.setWorkerId(workerId);

        return cacheEvent;
    }

    protected void addTinyUrlToCache(CacheEvent cacheEvent) {
        try {
            redisManager.addTinyUrlToCache(cacheEvent.getId(), cacheEvent.getXid(), cacheEvent.getBaseUrlKey(), cacheEvent.getAliasCode(),
                    cacheEvent.getRawUrl(), cacheEvent.getNewlyTinyUrlKeyExpiredSecond(), cacheEvent.getLockValue());
        } catch (Exception ex) {
            log.info("produce rollback message {}", cacheEvent.toString());
            cacheUtil.putCheckpoint(cacheEvent.getId(), Checkpoint.FAIL, true);
            boolean isSuccess = rocketMQProcessor.rollback(cacheEvent);
            int count = 0;
            while (!isSuccess && count <= retryTimes) {
                isSuccess =rocketMQProcessor.rollback(cacheEvent);
                count = count + 1;
            }
            // retry fail, stop the application from creating
            if (count > retryTimes) {
                healthMonitor.unhealth(Constants.CACHE_KEY_ROCKETMQ,"push rocketmq rollback event fail, should stop");
            }
            // if push to queue success, then the consumer will release the lock, else release the lock by producer
            cacheEvent.setLockRelease(isSuccess ? false : true);
            throw ex;
        }
    }



    /**
     * push to queue, group commit
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    protected void offerToQueue(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode, Long xid) {
        while (true) {
            try {
                // delay insert, group commit
                delayTinyManager.offerToQueue(id, rawUrl, baseUrlKey, baseUrl, aliasCode, xid);
                log.info("delay insert, group commit");
                break;
            }catch (Exception ex){
                log.error("push to queue exception id {} rawUrl {} baseUrl {} aliasCode {}",id,rawUrl,baseUrl,aliasCode,ex);
                CommonUtil.sleep(1000);
            }
        }
    }


    /**
     * create tiny url, data should be consistency
     * sync commit: data consistency is ensured by db transaction
     * group commit: data consistency is ensured by WAL
     * @param rawUrl
     * @param baseUrl
     * @param aliasCodeInput
     * @return
     */
    @Override
    public String doCreate(String rawUrl, String baseUrl, String aliasCodeInput) {
        /**
         * Step 0: prepare the variables
         */
        long from=System.nanoTime();

        long start=System.nanoTime();
        Long id = generateId(baseUrl, aliasCodeInput);
        long end=System.nanoTime();
        log.info("id {} generate, cost {}ns {}ms", id, end-start,(end-start)/1000000);

        start=System.nanoTime();
        // padding the code if need
        String aliasCodeEncode = Base62Util.encode(id, codeLength);
        end=System.nanoTime();
        log.info("id {} encode base62, cost {}ns {}ms", id, end-start,(end-start)/1000000);

        // baseurl key, e.g. t.vipgp88.com -> t
        String baseUrlKey = lookupManager.getValue(baseUrl);
        // append tiny url
        String tinyUrl = formatTinyUrl(baseUrl, aliasCodeInput, aliasCodeEncode);
        long to=System.nanoTime();
        log.info("prepare the variables when creating, cost {}ns {}ms", to-from,(to-from)/1000000);

        // db group commit or not
        if (dbCommitSync) {
            log.info("sync db commit, id {}", id);

            start=System.nanoTime();
            /**
             * Step 1: sync commit to db
             */
            syncDbCommit(id, rawUrl, baseUrlKey, baseUrl, aliasCodeEncode);
            end=System.nanoTime();
            log.info("id {} sync db commit, cost {}ns {}ms", id, end-start,(end-start)/1000000);
        } else {
            log.info("async db commit, id {}", id);

            start=System.nanoTime();
            /**
             * Step 1: delay group commit
             */
            asyncDbCommit(id,rawUrl,baseUrlKey,baseUrl,aliasCodeEncode);
            end=System.nanoTime();
            log.info("id {} async db commit, cost {}ns {}ms", id, end-start,(end-start)/1000000);
        }

        start=System.nanoTime();
        /**
         * Step 2: add the key of rawurl
         */
        // raw url -> tiny url
        // the same raw url will return the same tiny url over a period of time
        redisManager.refreshRawUrlToCache(rawUrl, baseUrlKey, tinyUrl,rawUrlKeyExpiredSecond);
        end=System.nanoTime();
        log.info("add raw url to redis, cost {}ns {}ms", end-start,(end-start)/1000000);

        return tinyUrl;
    }


    protected void asyncDbCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        log.info("log delay commit is {}", logDelayCommit);
        if(logDelayCommit){
            delayLogCommit(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
        }else{
            syncLogCommit(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
        }
    }

    private void delayLogCommit(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        log.info("do log delay commit async run begin, id {}", id);
        CompletableFuture<ErrorCode> future = CompletableFuture.supplyAsync(SupplierWrapper.of(new Supplier<ErrorCode>() {
            @Override
            public ErrorCode get() {
                return doDelayLogCommit(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
            }
        }), logDelayProcessor.getExecutor()).whenComplete(new BiConsumer() {
            @Override
            public void accept(Object o, Object o2) {
                log.info("future complete, id {}", id);
            }
        }).exceptionally(new Function() {
            @Override
            public Object apply(Object o) {
                log.error("future tasks run exception, id {}", id,o);
                return ErrorCode.CREATE_TINY_URL_EXCEPTION;
            }
        });

        ErrorCode result = ErrorCode.OK;
        try {
            result = future.get();
            log.info("do log delay commit async run end, id {}",id);
        } catch (Exception ex) {
            log.error("do log delay commit async exception, id {}", id, ex);
            throw new BaseAppException(ErrorCode.FUTURE_GET_FAIL);
        }

        if (!ErrorCode.OK.equals(result)) {
            throw new BaseAppException(result);
        }
    }


}
