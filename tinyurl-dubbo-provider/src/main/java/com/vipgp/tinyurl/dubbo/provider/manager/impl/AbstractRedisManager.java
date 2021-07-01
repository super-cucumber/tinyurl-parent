package com.vipgp.tinyurl.dubbo.provider.manager.impl;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.util.BitMapShardingUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/18 14:16
 */
@Slf4j
@Component
public abstract class AbstractRedisManager implements RedisManager {

    @Autowired
    CacheUtil cacheUtil;

    @NacosValue(value = "${redis.rollback.consume.key.expired:360}",autoRefreshed = true)
    protected long rollbackConsumeKeyExpired;

    @Override
    public void refreshRawUrlToCache(String rawUrl, String baseUrlKey, String tinyUrl, long rawUrlKeyExpiredSecond) {
        try {
            // raw url放入redis中, 使一段时间内重复raw url的返回同个tiny url
            String rawUrlKey = CommonUtil.getRawurlKey(baseUrlKey, rawUrl);
            set(rawUrlKey, tinyUrl, rawUrlKeyExpiredSecond);

            log.info("tiny url={},rawUrl={}", tinyUrl, rawUrl);
        }catch (Exception ex){
            log.error("rawurl={},baseUrlKey={},tinyUrl={}",rawUrl,baseUrlKey,tinyUrl,ex);
        }
    }

    /**
     * refresh the tiny url -> raw url to cache
     * 异步提交的时候调用，因为异步按组提交，就会出现生成短链后数据库还未落库，这个时候去查询会查询不到，所以用redis缓存来先保存对应关系
     * @param baseUrlKey
     * @param aliasCodeEncode
     * @param rawUrl
     */
    protected void refreshTinyUrlToCache(String baseUrlKey, String aliasCodeEncode, String rawUrl, long newlyTinyUrlKeyExpiredSecond) {
        String tinyUrlKey = CommonUtil.getTinyurlKey(baseUrlKey, aliasCodeEncode);
        set(tinyUrlKey, rawUrl, newlyTinyUrlKeyExpiredSecond);
        log.info("baseUrlKey={}, code={}, rawUrl={}", baseUrlKey, aliasCodeEncode, rawUrl);
    }

    protected void refreshTinyUrlUpdateTimeToCache(String baseUrlKey, String aliasCodeEncode, long updateTime, long newlyTinyUrlKeyExpiredSecond) {
        String tinyurlUpdateTimeKey = CommonUtil.getTinyurlUpdateTimeKey(baseUrlKey, aliasCodeEncode);
        set(tinyurlUpdateTimeKey, String.valueOf(updateTime), newlyTinyUrlKeyExpiredSecond);
        log.info("baseUrlKey={}, code={}, updateTime={}", baseUrlKey, aliasCodeEncode, updateTime);
    }

    /**
     * 更新位图
     * @param id
     * @param baseUrlKey
     */
    protected void refreshIdToBitmap(Long id, String baseUrlKey, boolean bitValue){
        // 使用bitmaps
        // id放入redis位图，用于判重以及验证是否存在，可以防止缓存穿透
        String bitKey= BitMapShardingUtil.getBitKey(id,baseUrlKey);
        long index=BitMapShardingUtil.calcIndex(id);
        setbit(bitKey, index, true);

        log.info("bitmap update:bitKey={},index={}", bitKey,index);
    }

    protected void refreshTxnXidToCache(String baseUrlKey, String aliasCodeEncode, long xid, long newlyTinyUrlKeyExpiredSecond){
        String key= CommonUtil.getTxnLogKey(baseUrlKey, aliasCodeEncode, cacheUtil.getWorkerId(),xid);
        set(key, String.valueOf(xid),newlyTinyUrlKeyExpiredSecond);
    }

    protected void refreshTxnXidEndToCache(String baseUrlKey, String aliasCodeEncode, long xid, long newlyTinyUrlKeyExpiredSecond){
        String key= CommonUtil.getTxnLogEndKey(baseUrlKey, aliasCodeEncode, cacheUtil.getWorkerId(),xid);
        set(key, String.valueOf(xid),newlyTinyUrlKeyExpiredSecond);
    }

    /**
     * The issue can happen that more than one clients get the same lock, if one of the below conditions meet:
     * 1. the lock expired before business run out, so the lock can be gotten by other clients
     * 2. the master-slave switch, but the lock has not been sync to slave
     *
     * alternative solution:
     * 1. redis redisson client - renew + redlock
     * 2. zookeeper lock - sequential ephemeral node + watch
     * 3. etcd lock - lease + watch
     * 4. mongodb - findAndModify
     *
     * @param key
     * @param value
     * @param expiredSecond
     * @return
     */
    @Override
    public boolean lock(String key, String value, long expiredSecond) {
        return setnx(key, value, expiredSecond);
    }

    @Override
    public boolean extendLock(String key, String value, long expiredSecond) {
        return setxx(key, value, expiredSecond);
    }

    @Override
    public boolean checkRollbackAlready(long xid, String workerId, String aliasCode) {
        String value = get(CommonUtil.getRollbackKey(workerId, xid, aliasCode));
        if (StringUtils.isEmpty(value)) {
            return false;
        }

        return true;
    }
}
