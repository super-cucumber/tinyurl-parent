package com.vipgp.tinyurl.dubbo.provider.manager.impl;

import com.vipgp.tinyurl.dubbo.provider.dao.LookupDao;
import com.vipgp.tinyurl.dubbo.provider.domain.LookupDO;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/15 10:13
 */
@Slf4j
@Component
public class LookupManagerImpl implements LookupManager{

    @Autowired
    RedisManager redisManager;
    @Autowired
    LookupDao lookupDao;
    @Autowired
    private CacheUtil cacheUtil;

    @Override
    public List<String> getKeys(){
        List<LookupDO> list = lookupDao.queryAll();
        List<String> result=new ArrayList<>();
        for (LookupDO item :
                list) {
            result.add(item.getLookupKey());
        }

        return result;
    }

    /**
     * 获取所有的values
     *
     * @return
     */
    @Override
    public List<String> getValues() {
        List<LookupDO> list = lookupDao.queryAll();
        List<String> result=new ArrayList<>();
        for (LookupDO item :
                list) {
            result.add(item.getLookupValue());
        }

        return result;
    }

    /**
     * 根据key获取value
     *
     * @param key
     * @return
     */
    @Override
    public String getValue(String key) {
        // 优先从本地缓存中获取
        String value= cacheUtil.get(Constants.CACHE_NAME_LOOKUP,key);
        if(StringUtils.isEmpty(value)) {
            // 从redis中获取
            value = redisManager.get(key);
            if (StringUtils.isEmpty(value)) {
                loadLookupToRedis(key);
                value = redisManager.get(key);
            }
            // 放入local cache中
            cacheUtil.put(Constants.CACHE_NAME_LOOKUP,key, value);
            log.info("lookup local cache miss, key {} value {}",key,value);
            return value;
        }else {
            log.info("lookup local cache hit, key {} value {}",key,value);
            return value;
        }
    }

    /**
     * lookup refresh to cache
     */
    @Override
    public void refreshToCache() {
        List<LookupDO> list = lookupDao.queryAll();
        for (LookupDO item :
                list) {
            redisManager.set(item.getLookupKey(),item.getLookupValue());
            // 放入local cache中
            cacheUtil.put(Constants.CACHE_NAME_LOOKUP,item.getLookupKey(), item.getLookupValue());
            redisManager.set(item.getLookupValue(),item.getLookupKey());
            // 放入local cache中
            cacheUtil.put(Constants.CACHE_NAME_LOOKUP,item.getLookupValue(), item.getLookupKey());
        }
    }

    /**
     * 初始化字典表缓存
     * @param key
     */
    private void loadLookupToRedis(String key){
        synchronized(this) {
            // 获取锁之后访问数据库之前再次判断缓存是否已存在
            if(!StringUtils.isEmpty(redisManager.get(key))){
                return;
            }
            List<LookupDO> list = lookupDao.queryAll();
            for (LookupDO item :
                    list) {
                redisManager.set(item.getLookupKey(),item.getLookupValue());
                redisManager.set(item.getLookupValue(),item.getLookupKey());
            }
        }
    }
}
