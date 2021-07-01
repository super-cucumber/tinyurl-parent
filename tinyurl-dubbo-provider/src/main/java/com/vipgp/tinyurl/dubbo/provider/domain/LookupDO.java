package com.vipgp.tinyurl.dubbo.provider.domain;

import lombok.Data;

import java.util.Date;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/15 9:35
 */
@Data
public class LookupDO {

    private Integer lookupId;

    private String lookupKey;

    private String lookupValue;

    private Date gmtCreate;

    private Date gmtModified;
}
