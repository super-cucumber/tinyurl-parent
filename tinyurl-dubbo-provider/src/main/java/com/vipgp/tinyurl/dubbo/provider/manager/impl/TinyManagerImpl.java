package com.vipgp.tinyurl.dubbo.provider.manager.impl;

import com.vipgp.tinyurl.dubbo.provider.dao.TinyRawUrlRelDao;
import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.persistence.FileTxnSnapLog;
import com.vipgp.tinyurl.dubbo.provider.util.Base62Util;
import com.vipgp.tinyurl.dubbo.provider.util.BitMapShardingUtil;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/13
 */
@Slf4j
@Component
public class TinyManagerImpl implements TinyManager {

    @Resource
    private TinyRawUrlRelDao tinyRawUrlRelDao;
    @Autowired
    private RedisManager redisManager;

    @Autowired
    FileTxnSnapLog txnSnapLog;

    @Override
    public void createTinyUrl(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        TinyRawUrlRelDO entity = builder(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
        tinyRawUrlRelDao.add(entity);
    }

    private TinyRawUrlRelDO builder(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode){
        TinyRawUrlRelDO entity = new TinyRawUrlRelDO();
        entity.setId(id);
        entity.setBaseUrl(baseUrlKey);
        entity.setTinyUrl(aliasCode);
        entity.setRawUrl(rawUrl);

        return entity;
    }


    /**
     * sync insert and set bitmap in db transaction
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addAndRefreshCacheInTransaction(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode) {
        long start=System.nanoTime();
        /**
         * Step 1: insert to db
         */
        TinyRawUrlRelDO entity =builder(id, rawUrl, baseUrlKey, baseUrl, aliasCode);
        tinyRawUrlRelDao.add(entity);
        long end=System.nanoTime();
        log.info("dao add, cost {}ns {}ms", end-start, (end-start)/1000000);

        start=System.nanoTime();
        /**
         * Step 2: set bitmap
         */
        String bitKey= BitMapShardingUtil.getBitKey(id,baseUrlKey);
        long index=BitMapShardingUtil.calcIndex(id);
        redisManager.setbit(bitKey, index, true);
        end=System.nanoTime();
        log.info("bitkey {}, index {}, set bit, cost {}ns {}ms", bitKey,index, end-start, (end-start)/1000000);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchInsert(List<TinyRawUrlRelDO> list, Long lastXid){
        tinyRawUrlRelDao.batchInsert(list);
        txnSnapLog.takeSnapShot(lastXid);
    }

    @Override
    public String getLongUrl(String code, String baseUrlKey) {

        long id=Base62Util.decode(code);
        TinyRawUrlRelDO query=new TinyRawUrlRelDO();
        query.setId(id);
        query.setBaseUrl(baseUrlKey);
        TinyRawUrlRelDO entity= tinyRawUrlRelDao.get(query);
        if(entity==null){
           throw new BaseAppException(ErrorCode.URL_UNEXIST_IN_DB);
        }else {
            log.info("long url is:" + entity.getRawUrl());
            return entity.getRawUrl();
        }
    }

}
