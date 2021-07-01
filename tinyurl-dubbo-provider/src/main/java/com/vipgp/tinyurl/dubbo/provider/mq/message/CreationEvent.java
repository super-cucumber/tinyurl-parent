package com.vipgp.tinyurl.dubbo.provider.mq.message;

import lombok.Data;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/23 10:06
 */
@Data
public class CreationEvent {
    private Long xid;
    private Long id;
    private String rawUrl;
    private String baseUrlKey;
    private String baseUrl;
    private String aliasCode;
}
