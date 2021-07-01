package com.vipgp.tinyurl.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Shangdu Lin on 2021/1/7 20:56.
 */
@Data
public class ResultVO<T> implements Serializable {

    private static final long serialVersionUID = 4566447196225195446L;

    private Boolean success;

    private String errorCode;

    private String errorMessage;

    private T result;

    public ResultVO(Boolean success, String errorCode, String errorMessage, T result){
        this.success=success;
        this.errorCode=errorCode;
        this.errorMessage=errorMessage;
        this.result=result;
    }

    public static <T> ResultVO<T> success(T result){
        return new ResultVO<T>(true,null,null,result);
    }

    public static <T> ResultVO<T> fail(String errorCode, String errorMessage){
        return new ResultVO<T>(false,errorCode,errorMessage,null);
    }

    @Override
    public String toString() {
        return "ResultVO{" +
                "success=" + success +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", result=" + result +
                '}';
    }
}
