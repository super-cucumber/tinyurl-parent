package com.vipgp.tinyurl.dubbo.provider.mq.message;

import lombok.Data;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/28 21:39
 *
 */
@Data
public class CacheEvent {
    private Long id;
    private Long xid;
    private String baseUrlKey;
    private String aliasCode;
    private String rawUrl;
    private Long newlyTinyUrlKeyExpiredSecond;
    private boolean lockRelease;
    private String lockValue;
    private String workerId;

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder("RollbackMessage{");
        sb.append(" id=").append(id);
        sb.append(", xid=").append(xid);
        sb.append(", baseUrlKey=").append(baseUrlKey);
        sb.append(", aliasCode=").append(aliasCode);
        sb.append(", rawUrl=").append(rawUrl);
        sb.append(", newlyTinyUrlKeyExpiredSecond=").append(newlyTinyUrlKeyExpiredSecond);
        sb.append(", lockRelease=").append(lockRelease);
        sb.append(", lockValue=").append(lockValue);
        sb.append(", workerId=").append(workerId);
        sb.append(" }");

        return sb.toString();
    }
}
