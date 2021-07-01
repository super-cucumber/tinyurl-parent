package com.vipgp.tinyurl.dubbo.provider.service.handler.query;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 14:34
 */
@Slf4j
@Component
public class QueryProcessor implements QueryHandler {

    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private TinyManager tinyManager;

    @Autowired
    private LookupManager lookupManager;

    @NacosValue(value = "${redis.tiny.url.expired}",autoRefreshed = true)
    private long tinyUrlExpiredSecond;

    private Map<String,ReentrantLock> locks=new HashMap<>();

    @PostConstruct
    public void init(){
        List<String> keys=lookupManager.getKeys();
        if(!CollectionUtils.isEmpty(keys)) {
            keys.stream().forEach(item->locks.put(item,new ReentrantLock()));
        }
    }

    /**
     * from cache
     * @param tinyUrlKey
     * @return
     */
    private String queryFromCache(String tinyUrlKey) {
        /**
         * Step 0: get from jvm local cache - caffeine at first
         */
        String rawUrl = cacheUtil.get(Constants.CACHE_NAME_TINYURL,tinyUrlKey);
        if (!StringUtils.isEmpty(rawUrl)) {
            log.info("localCache touched");
            return rawUrl;
        }

        /**
         * Step 1: get from redis
         */
        rawUrl = redisManager.queryRawUrl(tinyUrlKey);
        if (!StringUtils.isEmpty(rawUrl)) {
            log.info("redis touched");
            // it is possible the local cache is missed, e.g. memory insufficient, machine restart, so reset again
            cacheUtil.put(Constants.CACHE_NAME_TINYURL,tinyUrlKey, rawUrl);
            log.info("redis touched then put local cache");
            return rawUrl;
        }

        return null;
    }


    /**
     * from db
     * @param baseUrlKey
     * @param aliasCode
     * @param tinyUrlKey
     * @return
     */
    private String queryFromDb(String baseUrlKey, String aliasCode,String tinyUrlKey) {
        /**
         * read from cache at the first
         */
        String rawUrl =null;
        ReentrantLock lock=locks.get(baseUrlKey);
        if(lock==null){
            throw new BaseAppException(ErrorCode.BASEURL_UNEXIST.getCode(),ErrorCode.BASEURL_UNEXIST.getInfo());
        }

        lock.lock();
        try{
            /**
             * Step 1: try again after lock gotten
             */
            rawUrl = redisManager.queryRawUrl(tinyUrlKey);
            if (!StringUtils.isEmpty(rawUrl)) {
                log.info("lock got then redis touched");
                // cache can be expired, so reset again
                cacheUtil.put(Constants.CACHE_NAME_TINYURL,tinyUrlKey, rawUrl);
                log.info("lock got redis touched then put local cache");
                return rawUrl;
            }

            /**
             * Step 2: get from db at the end
             */
            rawUrl = tinyManager.getLongUrl(aliasCode,baseUrlKey);

        }finally {
            lock.unlock();
        }

        if(StringUtils.isEmpty(rawUrl)){
            throw new BaseAppException(ErrorCode.URL_UNEXIST_IN_DB.getCode(),ErrorCode.URL_UNEXIST_IN_DB.getInfo());
        }

        log.info("db touched");

        /**
         * Step 3: cache refresh
         */
        refreshToCache(tinyUrlKey,rawUrl);

        return rawUrl;
    }

    /**
     * cache refresh
     * @param tinyUrlKey
     * @param rawUrlFromDb
     */
    private void refreshToCache(String tinyUrlKey, String rawUrlFromDb) {
        // it is possible the cache has been updated by other servers, so read first, in order to reduce write qps
        String rawUrlFromRedis=redisManager.queryRawUrl(tinyUrlKey);
        if(StringUtils.isEmpty(rawUrlFromRedis)){
            redisManager.setRawUrl(tinyUrlKey,rawUrlFromDb, tinyUrlExpiredSecond);
        }
        // set local cache
        cacheUtil.put(Constants.CACHE_NAME_TINYURL,tinyUrlKey,rawUrlFromDb);
    }

    /**
     * query from cache, otherwise query from db
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public String doQuery(String baseUrl, String aliasCode){
        /**
         * Step 0: prepare
         */
        // baseurl key, e.g. t.vipgp88.com -> t
        String baseUrlKey=lookupManager.getValue(baseUrl);
        String tinyUrlKey= CommonUtil.getTinyurlKey(baseUrlKey, aliasCode);

        /**
         * Step 1: get from cache
         */
        String rawUrl=queryFromCache(tinyUrlKey);
        if(!StringUtils.isEmpty(rawUrl)){
            return rawUrl;
        }

        /**
         * Step 2: get from db
         */
        rawUrl= queryFromDb(baseUrlKey,aliasCode,tinyUrlKey);

        return rawUrl;
    }
}
