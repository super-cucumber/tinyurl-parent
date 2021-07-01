package com.vipgp.tinyurl.dubbo.provider.service.handler.prequery;

import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.util.*;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 14:01
 */
@Slf4j
@Component
public class NotExistValidation extends AbstractQueryValidation {

    @Autowired
    CacheUtil cacheUtil;

    @Autowired
    RedisManager redisManager;
    @Autowired
    LookupManager lookupManager;

    /**
     * process the handler
     *
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult process(String baseUrl, String aliasCode) {
        // baseurl->key, e.g. t.vipgp88.com -> t
        String baseUrlKey=lookupManager.getValue(baseUrl);

        /**
         * filter unexist code, keep off cache penetration
         */
        long id = Base62Util.decode(aliasCode);
        String bitKey = BitMapShardingUtil.getBitKey(id,baseUrlKey);
        long index = BitMapShardingUtil.calcIndex(id);
        // bitmap check exist
        boolean isTouched = redisManager.getbit(bitKey,index,baseUrlKey,aliasCode);
        if (!isTouched) {
            log.info("id untouched, aliasCode {}, id {}, bitKey {}, index {}", aliasCode,id,bitKey,index);
            return BaseResult.fail(ErrorCode.URL_UNEXIST.getCode(), ErrorCode.URL_UNEXIST.getInfo());
        }

        return null;
    }
}
