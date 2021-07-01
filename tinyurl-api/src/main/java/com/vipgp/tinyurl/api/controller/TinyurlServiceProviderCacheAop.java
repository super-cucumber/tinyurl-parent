package com.vipgp.tinyurl.api.controller;

import com.vipgp.tinyurl.dubbo.service.TinyurlService;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/15 1:56
 */
@Component
public class TinyurlServiceProviderCacheAop {

    @Reference(version = "1.0.0")
    TinyurlService tinyurlService;

    /**
     * put to cache
     * @param baseUrl
     * @param code
     * @return
     */
    @Cacheable(value = "query",key = "#code.concat('-').concat(#baseUrl)")
    public BaseResult<String> getLongUrl(String baseUrl,String code){
        return tinyurlService.getLongUrl(baseUrl,code);
    }
}
