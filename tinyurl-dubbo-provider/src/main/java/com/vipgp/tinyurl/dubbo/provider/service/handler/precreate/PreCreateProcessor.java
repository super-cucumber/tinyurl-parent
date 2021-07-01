package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 15:46
 */
public interface PreCreateProcessor {

    /**
     * pre create process
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    BaseResult process(String baseUrl, String rawUrl,String aliasCode);
}
