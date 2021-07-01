package com.vipgp.tinyurl.dubbo.service;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * author: admin
 * date: 2021/1/9
 */
public interface TinyurlService {

    /**
     * 根据长链接生成短链接
     * @param baseUrl
     * @param rawUrl
     * @return
     */
    BaseResult<String> createTinyurl(String baseUrl, String rawUrl);

    /**
     * 根据短连接code查找长地址
     * @param base62Code
     * @return
     */
    BaseResult<String> getLongUrl(String baseUrl,String base62Code);

    /**
     * 个性化code
     * @param baseUrl
     * @param base62Code
     * @param rawUrl
     * @return
     */
    BaseResult<String> createTinyurl(String baseUrl,String base62Code, String rawUrl);
}
