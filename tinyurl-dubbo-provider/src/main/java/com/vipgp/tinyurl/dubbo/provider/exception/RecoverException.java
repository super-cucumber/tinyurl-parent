package com.vipgp.tinyurl.dubbo.provider.exception;

import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/8 13:45
 */
public class RecoverException extends BaseAppException {
    public RecoverException(String desc){
        super(ErrorCode.RECOVER_EXCEPTION.getCode(),desc);
    }
}
