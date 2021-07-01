package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 9:54
 */
@Component("preCreateProcessorNoAlias")
public class PreCreateProcessorNoAlias extends AbstractPreCreateProcessor {
    @Autowired
    private BaseUrlValidationForPreCreate baseUrlValidation;
    @Autowired
    private RawUrlValidation rawUrlValidation;
    @Autowired
    private IdenticalRawUrlValidation identicalRawUrlValidation;
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
        rawUrlValidation.setNext(baseUrlValidation);
        baseUrlValidation.setNext(identicalRawUrlValidation);
        identicalRawUrlValidation.setNext(healthValidation);
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
