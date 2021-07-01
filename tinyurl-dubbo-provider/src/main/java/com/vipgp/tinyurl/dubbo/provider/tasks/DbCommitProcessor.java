package com.vipgp.tinyurl.dubbo.provider.tasks;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/23 0:55
 */
public interface DbCommitProcessor {
    /**
     * push to queue
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     * @param xid
     */
    void offerToQueue(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode, Long xid);
}
