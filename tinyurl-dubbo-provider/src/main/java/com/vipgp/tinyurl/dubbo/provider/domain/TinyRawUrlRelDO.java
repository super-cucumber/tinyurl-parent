package com.vipgp.tinyurl.dubbo.provider.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
@Data
public class TinyRawUrlRelDO implements Serializable {

    private Long id;

    private String baseUrl;

    private String tinyUrl;

    private String rawUrl;

    private Date gmtCreate;
}
