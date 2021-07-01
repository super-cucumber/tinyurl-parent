package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.provider.dto.CreateDTO;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.service.checker.Checker;
import com.vipgp.tinyurl.dubbo.provider.system.HealthMonitor;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/29 13:33
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AppHealthValidation extends AbstractCreateValidation implements Checker<CreateDTO> {

    @Autowired
    HealthMonitor healthMonitor;

    @Override
    public void check(CreateDTO createDTO) {
        String unhealth=healthMonitor.checkHealth();
        if(!StringUtils.isEmpty(unhealth)){
            throw new BaseAppException(ErrorCode.UNHEALTH.getCode(), unhealth);
        }
    }

    /**
     * do process in each validation
     *
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult doProcess(String baseUrl, String rawUrl, String aliasCode) {
        String unhealth=healthMonitor.checkHealth();
        if(!StringUtils.isEmpty(unhealth)){
            return BaseResult.fail(ErrorCode.UNHEALTH.getCode(), unhealth);
        }
        return null;
    }
}
