package com.vipgp.tinyurl.dubbo.provider.service.handler.prequery;

import com.vipgp.tinyurl.dubbo.provider.util.TinyurlValidator;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 9:13
 */
@Component
public class BaseUrlValidationForPreQuery extends AbstractQueryValidation {

    @Autowired
    private TinyurlValidator validator;


    /**
     * process the handler
     *
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult process(String baseUrl, String aliasCode) {
        // base url
        BaseResult result=validator.validateBaseUrl(baseUrl);
        if(result!=null){
            return result;
        }

        return processNext(baseUrl, aliasCode);
    }
}
