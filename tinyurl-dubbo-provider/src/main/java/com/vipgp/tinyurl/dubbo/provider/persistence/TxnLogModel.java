package com.vipgp.tinyurl.dubbo.provider.persistence;

import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import lombok.Data;
import org.springframework.data.annotation.Transient;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/2 12:54
 */
@Data
public class TxnLogModel {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 短地址主域名key
     */
    private String baseUrlKey;

    /**
     * 短地址主域名
     */
    private String baseUrl;

    /**
     * 别名code
     */
    private String aliasCode;

    /**
     * 原地址
     */
    private String rawUrl;

    /**
     * 事务ID
     */
    private volatile long xid;

    /**
     * PREPARE - prepare
     * COMMIT - commit
     */
    private int status;

    @Transient
    private volatile LogCommitEvent logCommitEvent;
    @Transient
    private byte[] data;
    @Transient
    private long crcValue;
    @Transient
    private int size;


    public static TxnLogModel builder(Long id,String baseUrlKey, String baseUrl,String aliasCode, String rawUrl){
        TxnLogModel model=new TxnLogModel();
        model.setId(id);
        model.setBaseUrlKey(baseUrlKey);
        model.setBaseUrl(baseUrl);
        model.setAliasCode(aliasCode);
        model.setRawUrl(rawUrl);

        return model;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append("TxnLogModel {");
        sb.append(" id=").append(id);
        sb.append(",xid=").append(xid);
        sb.append(",baseUrlKey=").append(baseUrlKey);
        sb.append(",baseUrl=").append(baseUrl);
        sb.append(",aliasCode=").append(aliasCode);
        sb.append(",rawUrl=").append(rawUrl);
        sb.append(",status=").append(status);
        sb.append(",offset=").append(logCommitEvent==null?"null":logCommitEvent.toString());
        sb.append(" }");

        return sb.toString();
    }
}
