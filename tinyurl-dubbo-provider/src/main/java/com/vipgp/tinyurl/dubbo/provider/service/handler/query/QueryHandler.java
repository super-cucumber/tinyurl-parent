package com.vipgp.tinyurl.dubbo.provider.service.handler.query;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 14:14
 */
public interface QueryHandler {

    /**
     * query from cache, otherwise query from db
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    String doQuery(String baseUrl, String aliasCode);
}
