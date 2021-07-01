package com.vipgp.tinyurl.dubbo.provider.service.handler.prequery;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 13:54
 */
@Component
public class PreQueryProcessor {
    @Autowired
    private AliasCodeValidationForPreQuery aliasCodeValidation;
    @Autowired
    private BaseUrlValidationForPreQuery baseUrlValidation;
    @Autowired
    private NotExistValidation notExistValidation;


    /**
     * the header of the chain
     */
    private QueryValidation header;

    /**
     * init the chain
     */
    @PostConstruct
    private void init(){
        this.header=baseUrlValidation;
        baseUrlValidation.setNext(aliasCodeValidation);
        aliasCodeValidation.setNext(notExistValidation);
    }

    /**
     * process pre query
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    public BaseResult process(String baseUrl, String aliasCode) {
        return this.header.process(baseUrl, aliasCode);
    }
}
