package com.vipgp.tinyurl.dubbo.provider.util;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.tasks.Checkpoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/15 14:41
 */
@Slf4j
@Component
public class CacheUtil {

    @Autowired
    CacheManager cacheManager;

    @NacosValue(value = "${dubbo.protocol.port}")
    int port;

    int capacity=2048;

    final ArrayList<String> locks=new ArrayList(capacity);

    @PostConstruct
    public void init(){
        // init locks
        for(int i=0;i<capacity;i++){
            locks.add(String.valueOf(i));
        }
    }

    private int hash(long xid){
        return (int)(xid % locks.size());
    }


    /**
     * get local cache
     * @param cacheName
     * @param key
     * @return
     */
    public String get(String cacheName, String key){
        Cache cache= cacheManager.getCache(cacheName);
        if(cache==null){
            log.error("caffeine item has not been init, cacheName="+cacheName);
            return null;
        }else {
            return  cache.get(key,String.class);
        }

    }

    /**
     * put local cache
     * @param cacheName
     * @param key
     * @param value
     */
    public void put(String cacheName, String key, String value){
        Cache cache=cacheManager.getCache(cacheName);
        if(cache==null){
            log.error("caffeine item has not been init, cacheName="+cacheName);
            return;
        }else {
            cache.put(key, value);
        }
    }

    /**
     * each server have worker id
     * work id comprised by ip+port
     * @return
     */
    public String getWorkerId(){
        String workerId= get(Constants.CACHE_NAME_COMMON, Constants.CACHE_KEY_WORKERID);
        if(StringUtils.isEmpty(workerId)){
            String ip= CommonUtil.getIp();
            workerId=ip+"|"+port;
            put(Constants.CACHE_NAME_COMMON, Constants.CACHE_KEY_WORKERID,workerId);
        }

        return workerId;
    }

    public void putCheckpoint(long id, Checkpoint checkpoint, boolean noException){
        if(noException){
            try{
                putCheckpoint(id,checkpoint);
            }catch (Exception ex){
                log.error("put check point exception", ex);
            }
        }else {
            putCheckpoint(id, checkpoint);
        }
    }

    public void putCheckpoint(long id, Checkpoint checkpoint){
        long from=System.nanoTime();
        int index= hash(id);
        long start=System.nanoTime();
        synchronized (locks.get(index)){
            long end=System.nanoTime();
            log.info("id {}, index {}, put checkpoint {} lock cost {}ns {}ms", id, index, checkpoint.ordinal(), end-start, (end-start)/1000000);
            put(Constants.CACHE_NAME_DB_FLUSH, String.valueOf(id), String.valueOf(checkpoint.ordinal()));
        }
        long to=System.nanoTime();
        log.info("id {},  index {}, put checkpoint {}, cost {}ns {}ms", id, index, checkpoint.ordinal(), to-from, (to-from)/1000000);

    }

    public String getCheckpoint(long id){
        int index= hash(id);
        synchronized (locks.get(index)){
            return get(Constants.CACHE_NAME_DB_FLUSH, String.valueOf(id));
        }
    }
}
