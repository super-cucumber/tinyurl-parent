package com.vipgp.tinyurl.dubbo.provider.util;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/8 13:41
 */
public enum ErrorCode {

    OK("00000","ok"),
    UNHEALTH("A0001","unhealth"),
    URL_CREATE_EXCEPTION("B0001","create tiny url encounter exception"),
    URL_GET_EXCEPTION("B0002","get raw url encounter exception"),
    URL_ALREADY_USED("B0003","tiny url already used"),
    URL_UNEXIST("B0004","tiny url unexist"),
    URL_IS_EMPTY("B0005","raw url is empty"),
    URL_IS_TOO_LONG("B0006","raw url is too long"),
    CODE_IS_EMPTY("B0007","code is empty"),
    CODE_IS_TOO_LONG("B0008","code is too long"),
    BASEURL_IS_EMPTY("B0009","base url is empty"),
    BASEURL_UNEXIST("B0010","base url unexist"),
    CODE_INVALID("B0011","code is invalid"),
    IDENTICAL_RAW_URL("B0012","identical raw url"),
    ID_CACHE_INIT_FALSE("B0013","id cache is not already init"),
    ID_KEY_NOT_EXISTS("B0014","id cache key is not exists"),
    TWO_SEGMENTS_ARE_NULL("B0015","two segments are null"),
    WRITE_AHEAD_PREPARE_LOG_EXCEPTION("B0016","write ahead prepare log exception"),
    WRITE_AHEAD_COMMIT_LOG_EXCEPTION("B0017","write ahead commit log exception"),
    EOF("B0018","EOF"),
    RECOVER_EXCEPTION("B0019","recover exception"),
    SCRIPT_LOAD_EXCEPTION("B0020","lua script load exception"),
    LOCK_IS_BUSY("B0021","system is busy"),
    URL_IS_INVALID("B0022","url is invalid"),
    URL_UNEXIST_IN_DB("B0023","tiny url unexist in db"),
    FUTURE_GET_FAIL("B0024","future get fail"),
    CYCLIC_BARRIER_AWAIT_EXCEPTION("B0025","cyclic barrier await exception"),
    CREATE_TINY_URL_EXCEPTION("B0026","create tiny url exception"),;

    private String code;
    private String info;

    private ErrorCode(String code,String info){
        this.code=code;
        this.info=info;
    }

    public String getCode(){
        return this.code;
    }

    public String getInfo(){
        return this.info;
    }

    public static ErrorCode getErrorCode(String errorCode){
        for(ErrorCode item : ErrorCode.values()){
            if(item.getCode().equals(errorCode)){
                return item;
            }
        }

        return null;
    }
}
