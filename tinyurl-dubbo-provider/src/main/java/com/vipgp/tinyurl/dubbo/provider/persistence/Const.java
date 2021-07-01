package com.vipgp.tinyurl.dubbo.provider.persistence;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/12 14:09
 */
public class Const {
    /**
     * char - B
     */
    public static final int EOR =66;
    /**
     * char - C
     */
    public static final int STATUS_COMMIT =67;
    /**
     * char - P
     */
    public static final int STATUS_PREPARE =80;

    public static final String TAG_DATA_LENGTH ="dataLength";
    public static final String TAG_TXN_STATUS ="txnStatus";
    public static final String TAG_CRC_VALUE ="crcValue";
    public static final String TAG_TXN_XID ="txnXid";
    public static final String TAG_TXN_ENTRY ="txnEntry";
    public static final String TAG_END_OF_REEL ="EOR";


    public static final String LOG_FILE_PREFIX="log";
    public static final String SNAP_FILE_PREFIX="snapshot";
}
