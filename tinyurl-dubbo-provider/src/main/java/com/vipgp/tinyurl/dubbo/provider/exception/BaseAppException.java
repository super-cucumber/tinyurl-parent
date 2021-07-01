package com.vipgp.tinyurl.dubbo.provider.exception;

import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import lombok.Data;
import lombok.ToString;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/18 17:03
 */
@Data
@ToString(callSuper = true)
public class BaseAppException extends RuntimeException {


    private static final long serialVersionUID = 2510253740027393016L;

    private String code;
    private String desc;

    public BaseAppException(){super();}

    public BaseAppException(ErrorCode code){
        super(code.getInfo());
        this.code=code.getCode();
        this.desc=code.getInfo();
    }

    public BaseAppException(String code){
        this.code=code;
    }

    public BaseAppException(String code, String desc){
        super(desc);
        this.code=code;
        this.desc=desc;
    }

    @Override
    public String toString(){
       StringBuilder sb=new StringBuilder("BaseAppException-");
       sb.append("code=").append(this.code);
       sb.append(",desc=").append(this.desc);
       return sb.toString();
    }
}
