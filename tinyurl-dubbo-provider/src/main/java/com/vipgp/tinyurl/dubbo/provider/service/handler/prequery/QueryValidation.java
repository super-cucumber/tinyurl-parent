package com.vipgp.tinyurl.dubbo.provider.service.handler.prequery;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/22 15:44
 */
public interface QueryValidation {

    /**
     * process the handler
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    BaseResult process(String baseUrl,String aliasCode);

    /**
     * set next to chain
     * @param handler
     */
    void setNext(QueryValidation handler);

    /**
     * process the next in the chain
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    BaseResult processNext(String baseUrl,String aliasCode);
}
