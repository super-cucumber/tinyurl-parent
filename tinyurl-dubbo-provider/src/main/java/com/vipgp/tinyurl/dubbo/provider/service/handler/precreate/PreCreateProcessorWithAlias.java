package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 9:54
 */
@Component("preCreateProcessorWithAlias")
public class PreCreateProcessorWithAlias extends AbstractPreCreateProcessor{
    @Autowired
    private AliasCodeValidationForPreCreate aliasCodeValidation;
    @Autowired
    private BaseUrlValidationForPreCreate baseUrlValidation;
    @Autowired
    private RawUrlValidation rawUrlValidation;
    @Autowired
    private DuplicateAliasValidation duplicateAliasValidation;
    @Autowired
    private AppHealthValidation healthValidation;

    private CreateValidation header;

    /**
     * init the chain
     */
    @PostConstruct
    @Override
    protected void initChain(){
        this.header=rawUrlValidation;
        rawUrlValidation.setNext(aliasCodeValidation);
        aliasCodeValidation.setNext(baseUrlValidation);
        baseUrlValidation.setNext(duplicateAliasValidation);
        duplicateAliasValidation.setNext(healthValidation);
    }

    /**
     * the header of the chain
     *
     * @return
     */
    @Override
    protected CreateValidation getHeader() {
        return this.header;
    }
}
