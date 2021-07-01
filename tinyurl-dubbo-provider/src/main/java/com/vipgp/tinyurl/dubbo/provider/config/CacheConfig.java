package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import com.vipgp.tinyurl.dubbo.provider.util.MathUtil;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/5 13:20
 */
@Configuration
public class CacheConfig {

    @NacosValue(value = "${caffeine.max.expired}",autoRefreshed = true)
    int maxExpiredSeconds;

    @NacosValue(value = "${caffeine.min.expired}",autoRefreshed = true)
    int minExpiredSeconds;

    @NacosValue(value = "${caffeine.maximum.size}",autoRefreshed = true)
    long maximumSize;

    @NacosValue(value = "${caffeine.checkpoint.expired}",autoRefreshed = true)
    int checkPointExpiredSeconds;

    @NacosValue(value = "${caffeine.checkpoint.maximum.size}",autoRefreshed = true)
    long checkPointMaximumSize;

    @Bean
    public CacheManager caffeineCacheManager(){
        SimpleCacheManager cacheManager=new SimpleCacheManager();
        ArrayList<CaffeineCache> caches= Lists.newArrayList();
        // 不同服务器缓存失效时间是随机值，可以防止缓存击穿和缓存雪崩
        int seconds= MathUtil.random(minExpiredSeconds,maxExpiredSeconds);
        Duration duration=Duration.ofSeconds(seconds);
        CaffeineCache tinyUrlCache=new CaffeineCache(Constants.CACHE_NAME_TINYURL,
                Caffeine.newBuilder().recordStats().expireAfterWrite(duration).maximumSize(maximumSize).build());
        CaffeineCache lookupCache=new CaffeineCache(Constants.CACHE_NAME_LOOKUP,
                Caffeine.newBuilder().recordStats().expireAfterWrite(Duration.ofSeconds(Integer.MAX_VALUE)).maximumSize(1000).build());
        // no expire
        CaffeineCache commonCache=new CaffeineCache(Constants.CACHE_NAME_COMMON,
                Caffeine.newBuilder().recordStats().expireAfterWrite(Duration.ofSeconds(Integer.MAX_VALUE)).maximumSize(1000).build());
        // no expire
        CaffeineCache healthCache=new CaffeineCache(Constants.CACHE_NAME_HEALTH,
                Caffeine.newBuilder().recordStats().expireAfterWrite(Duration.ofSeconds(Integer.MAX_VALUE)).maximumSize(1000).build());
        // for db flush which base on txn
        CaffeineCache dbFlushCache=new CaffeineCache(Constants.CACHE_NAME_DB_FLUSH,
                Caffeine.newBuilder().recordStats().expireAfterWrite(Duration.ofSeconds(checkPointExpiredSeconds)).maximumSize(checkPointMaximumSize).build());
        caches.add(tinyUrlCache);
        caches.add(lookupCache);
        caches.add(commonCache);
        caches.add(dbFlushCache);
        caches.add(healthCache);
        cacheManager.setCaches(caches);

        return cacheManager;
    }
}
