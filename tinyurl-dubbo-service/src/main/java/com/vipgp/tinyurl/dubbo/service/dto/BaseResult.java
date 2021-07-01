package com.vipgp.tinyurl.dubbo.service.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/8 13:13
 */
@Data
public class BaseResult<T> implements Serializable {

    private static final long serialVersionUID = -8119616235050936350L;

    private boolean success;

    private String errorCode;

    private String errorMessage;

    private T result;

    public BaseResult(){}

    public BaseResult(Boolean success, String errorCode, String errorMessage, T result){
        this.success=success;
        this.errorCode=errorCode;
        this.errorMessage=errorMessage;
        this.result=result;
    }

    public static <T> BaseResult<T> success(T result){
        return new BaseResult<T>(true,null,null,result);
    }

    public static <T> BaseResult<T> fail(String errorCode, String errorMessage){
        return new BaseResult<T>(false,errorCode,errorMessage,null);
    }

    @Override
    public String toString() {
        return "BaseResult{" +
                "success=" + success +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", result=" + result +
                '}';
    }
}
