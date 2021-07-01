package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/22 15:40
 */
public interface CreateValidation {

    /**
     * process the handler
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    BaseResult process(String baseUrl,String rawUrl, String aliasCode);

    /**
     * set next to chain
     * @param handler
     */
    void setNext(CreateValidation handler);

    /**
     *  process the next in the chain
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    BaseResult processNext(String baseUrl, String rawUrl, String aliasCode);


}
