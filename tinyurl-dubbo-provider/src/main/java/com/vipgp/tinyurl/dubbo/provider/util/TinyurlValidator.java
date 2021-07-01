package com.vipgp.tinyurl.dubbo.provider.util;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/15 11:06
 */
@Component
public class TinyurlValidator {

    @Autowired
    CacheUtil cacheUtil;
    @Autowired
    LookupManager lookupManager;

    /**
     * 62位进制字符长度,将可以产生62^7个短链接
     */
    @NacosValue(value = "${tiny.url.length}",autoRefreshed = true)
    int codeLength;

    /**
     * 验证raw url输入是否合法
     * @param rawUrl
     * @return
     */
    public BaseResult validateRawUrl(String rawUrl){
        if(StringUtils.isEmpty(rawUrl)){
            return BaseResult.fail(ErrorCode.URL_IS_EMPTY.getCode(), ErrorCode.URL_IS_EMPTY.getInfo());
        }

        if(rawUrl.length()>2048){
            return BaseResult.fail(ErrorCode.URL_IS_TOO_LONG.getCode(), ErrorCode.URL_IS_TOO_LONG.getInfo());
        }

        String[] schemes={"http","https","ftp"};
        UrlValidator urlValidator=new UrlValidator(schemes,UrlValidator.ALLOW_ALL_SCHEMES);
        boolean isValid= urlValidator.isValid(rawUrl);
        if(!isValid){
            return BaseResult.fail(ErrorCode.URL_IS_INVALID.getCode(), ErrorCode.URL_IS_INVALID.getInfo());
        }

        return null;
    }

    /**
     * 验证code输入是否合法
     * @param base62Code
     * @return
     */
    public BaseResult validateCode(String base62Code){
        if(StringUtils.isEmpty(base62Code)){
            return BaseResult.fail(ErrorCode.CODE_IS_EMPTY.getCode(), ErrorCode.CODE_IS_EMPTY.getInfo());
        }

        if(base62Code.length()>codeLength){
            return BaseResult.fail(ErrorCode.CODE_IS_TOO_LONG.getCode(), ErrorCode.CODE_IS_TOO_LONG.getInfo());
        }

        if(!Base62Util.validateCode(base62Code)){
            return BaseResult.fail(ErrorCode.CODE_INVALID.getCode(), ErrorCode.CODE_INVALID.getInfo());
        }

        return null;
    }

    /**
     * 验证baseUrl输入是否合法
     * @param baseUrl
     * @return
     */
    public BaseResult validateBaseUrl(String baseUrl){
        if(StringUtils.isEmpty(baseUrl)){
            return BaseResult.fail(ErrorCode.BASEURL_IS_EMPTY.getCode(), ErrorCode.BASEURL_IS_EMPTY.getInfo());
        }

        String key=lookupManager.getValue(baseUrl);
        if(StringUtils.isEmpty(key)) {
            return BaseResult.fail(ErrorCode.BASEURL_UNEXIST.getCode(), ErrorCode.BASEURL_UNEXIST.getInfo());
        }

        return null;
    }
}
