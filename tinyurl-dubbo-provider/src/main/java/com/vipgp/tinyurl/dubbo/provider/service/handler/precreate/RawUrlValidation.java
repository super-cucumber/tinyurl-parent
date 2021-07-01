package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.provider.dto.CreateDTO;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.service.checker.Checker;
import com.vipgp.tinyurl.dubbo.provider.util.TinyurlValidator;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 9:13
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RawUrlValidation extends AbstractCreateValidation implements Checker<CreateDTO> {

    @Autowired
    private TinyurlValidator validator;

    /**
     * process validation
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult doProcess(String baseUrl, String rawUrl, String aliasCode) {
        // raw url
        BaseResult result = validator.validateRawUrl(rawUrl);
        return result;
    }

    @Override
    public void check(CreateDTO createDTO) {
        // raw url
        BaseResult result = validator.validateRawUrl(createDTO.getRawUrl());
        if(result!=null){
            throw new BaseAppException(result.getErrorCode(),result.getErrorMessage());
        }
    }
}
