package com.vipgp.tinyurl.dubbo.provider.persistence;

import lombok.Data;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/2 12:54
 */
@Data
public class SnapShotModel {
    /**
     * 最大的xid
     */
    private Long lastXid;
}
