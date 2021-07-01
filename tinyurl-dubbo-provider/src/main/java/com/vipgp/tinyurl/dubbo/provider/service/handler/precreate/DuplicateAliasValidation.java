package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.provider.dto.CreateDTO;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.service.checker.Checker;
import com.vipgp.tinyurl.dubbo.provider.util.*;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
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
public class DuplicateAliasValidation extends AbstractCreateValidation implements Checker<CreateDTO> {

    @Autowired
    CacheUtil cacheUtil;

    @Autowired
    RedisManager redisManager;

    @Autowired
    LookupManager lookupManager;

    /**
     * process validation
     *
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult doProcess(String baseUrl, String rawUrl, String aliasCode) {
        // baseurl对应的key, e.g. t.vipgp88.com -> t
        String baseUrlKey = lookupManager.getValue(baseUrl);

        /**
         * Step 1: 查看code是否已被使用
         */
        long id = Base62Util.decode(aliasCode);
        String bitKey = BitMapShardingUtil.getBitKey(id, baseUrlKey);
        long index = BitMapShardingUtil.calcIndex(id);
        // 使用bitmaps
        boolean isTouched = redisManager.getbit(bitKey,index,baseUrlKey,aliasCode);
        if (isTouched) {
            log.info("code already used, please change it,bitKey=" + bitKey + ",index=" + index);
            return BaseResult.fail(ErrorCode.URL_ALREADY_USED.getCode(), ErrorCode.URL_ALREADY_USED.getInfo());
        }

        return null;
    }

    @Override
    public void check(CreateDTO createDTO) {
        // baseurl对应的key, e.g. t.vipgp88.com -> t
        String baseUrlKey = lookupManager.getValue(createDTO.getBaseUrl());

        /**
         * Step 1: 查看code是否已被使用
         */
        long id = Base62Util.decode(createDTO.getAliasCode());
        String bitKey = BitMapShardingUtil.getBitKey(id, baseUrlKey);
        long index = BitMapShardingUtil.calcIndex(id);
        // 使用bitmaps
        boolean isTouched = redisManager.getbit(bitKey,index,baseUrlKey,createDTO.getAliasCode());
        if (isTouched) {
            log.info("code already used, please change it,bitKey=" + bitKey + ",index=" + index);
            throw new BaseAppException(ErrorCode.URL_ALREADY_USED.getCode(), ErrorCode.URL_ALREADY_USED.getInfo());
        }
    }
}
