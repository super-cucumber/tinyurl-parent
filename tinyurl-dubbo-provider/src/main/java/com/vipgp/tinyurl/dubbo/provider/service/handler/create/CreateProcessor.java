package com.vipgp.tinyurl.dubbo.provider.service.handler.create;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 15:50
 */
public interface CreateProcessor {

    /**
     * do create
     * @param rawUrl
     * @param baseUrl
     * @param aliasCodeInput
     * @return
     */
    String doCreate(String rawUrl, String baseUrl, String aliasCodeInput);
}
