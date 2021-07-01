package com.vipgp.tinyurl.dubbo.provider.id.generator;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/25 14:21
 */
public enum ProviderEnum {
    LEAF_SEGMENT("leaf-segment","meituan leaf segment id"),
    LEAF_SNOWFLAKE("leaf-snowflake","meituan leaf snowflake id"),
    FLIKER("flicker","flicker id");

    private String key;
    private String desc;

    private ProviderEnum(String key, String desc){
        this.key=key;
        this.desc=desc;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }
}
