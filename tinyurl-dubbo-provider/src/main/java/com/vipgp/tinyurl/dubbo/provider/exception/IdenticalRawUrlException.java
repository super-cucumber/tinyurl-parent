package com.vipgp.tinyurl.dubbo.provider.exception;

import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/19 13:41
 */
public class IdenticalRawUrlException extends BaseAppException {

    public IdenticalRawUrlException(String desc){
        super(ErrorCode.IDENTICAL_RAW_URL.getCode(),desc);
    }
}
