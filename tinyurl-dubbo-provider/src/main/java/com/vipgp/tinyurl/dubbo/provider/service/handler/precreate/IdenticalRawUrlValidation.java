package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.dto.CreateDTO;
import com.vipgp.tinyurl.dubbo.provider.exception.IdenticalRawUrlException;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.service.checker.Checker;
import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 9:13
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdenticalRawUrlValidation extends AbstractCreateValidation implements Checker<CreateDTO> {

    @Autowired
    CacheUtil cacheUtil;

    @Autowired
    RedisManager redisManager;
    @Autowired
    LookupManager lookupManager;

    @NacosValue(value = "${raw.url.identical.allow}", autoRefreshed = true)
    boolean allowIdentical;

    /**
     * process validation
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult doProcess(String baseUrl, String rawUrl, String aliasCode) {
        if(allowIdentical){
            log.info("allow identical is true");
            return null;
        }
        // baseurl对应的key, e.g. t.vipgp88.com -> t
        String baseUrlKey=lookupManager.getValue(baseUrl);

        /**
         * Step 1: 判断是否是同个rawUrl
         */
        String rawUrlKey=CommonUtil.getRawurlKey(baseUrlKey, rawUrl);
        String tinyUrl = redisManager.get(rawUrlKey);
        if (!StringUtils.isEmpty(tinyUrl)) {
            log.info("重复rawurl,返回tinyUrl=" + tinyUrl);
            return BaseResult.success(tinyUrl);
        }

        return null;
    }

    @Override
    public void check(CreateDTO createDTO) {
        // baseurl对应的key, e.g. t.vipgp88.com -> t
        String baseUrlKey=lookupManager.getValue(createDTO.getBaseUrl());

        /**
         * Step 1: 判断是否是同个rawUrl
         */
        String rawUrlKey=CommonUtil.getRawurlKey(baseUrlKey, createDTO.getRawUrl());
        String tinyUrl = redisManager.get(rawUrlKey);
        if (!StringUtils.isEmpty(tinyUrl)) {
            log.info("重复rawurl,返回tinyUrl=" + tinyUrl);
            throw new IdenticalRawUrlException(tinyUrl);
        }

    }
}
