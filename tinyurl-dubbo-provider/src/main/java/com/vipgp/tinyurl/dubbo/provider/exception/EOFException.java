package com.vipgp.tinyurl.dubbo.provider.exception;

import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/8 13:45
 */
public class EOFException extends BaseAppException {
    public EOFException(String desc){
        super(ErrorCode.IDENTICAL_RAW_URL.getCode(),desc);
    }
}
