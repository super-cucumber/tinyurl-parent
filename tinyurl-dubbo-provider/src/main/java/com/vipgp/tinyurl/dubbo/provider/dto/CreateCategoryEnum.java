package com.vipgp.tinyurl.dubbo.provider.dto;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/18 16:31
 */
public enum CreateCategoryEnum {
    NO_ALIAS("noalias","noalias"),
    WITH_ALIAS("withalias","withalias");

    private String key;
    private String value;
    private CreateCategoryEnum(String key,String value){
        this.key=key;
        this.value=value;
    }
}
