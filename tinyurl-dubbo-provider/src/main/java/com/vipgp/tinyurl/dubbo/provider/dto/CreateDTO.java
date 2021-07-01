package com.vipgp.tinyurl.dubbo.provider.dto;

import com.vipgp.tinyurl.dubbo.provider.service.checker.Valid;
import com.vipgp.tinyurl.dubbo.provider.service.handler.precreate.AliasCodeValidationForPreCreate;
import com.vipgp.tinyurl.dubbo.provider.service.handler.precreate.BaseUrlValidationForPreCreate;
import com.vipgp.tinyurl.dubbo.provider.service.handler.precreate.IdenticalRawUrlValidation;
import com.vipgp.tinyurl.dubbo.provider.service.handler.precreate.RawUrlValidation;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/18 16:23
 */
@Data
public class CreateDTO {

    /**
     * 短地址主域名
     */
    @Valid(BaseUrlValidationForPreCreate.class)
    private String baseUrl;

    /**
     * 别名code
     */
    @Valid(AliasCodeValidationForPreCreate.class)
    private String aliasCode;

    /**
     * 原地址
     */
    @Valid({IdenticalRawUrlValidation.class, RawUrlValidation.class})
    private String rawUrl;

    /**
     * 创建类别 -
     */
    @NotNull
    private CreateCategoryEnum category;

}
