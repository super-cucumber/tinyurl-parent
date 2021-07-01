package com.vipgp.tinyurl.dubbo.provider.manager;

import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;

import java.util.List;

/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
public interface TinyManager {

    void createTinyUrl(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode);

    String getLongUrl(String code, String baseUrlKey);

    void batchInsert(List<TinyRawUrlRelDO> list, Long lastXid);

    /**
     * insert to db and update the bitmap in db transaction
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    void addAndRefreshCacheInTransaction(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode);
}
