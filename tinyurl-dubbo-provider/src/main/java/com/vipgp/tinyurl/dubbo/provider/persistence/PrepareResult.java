package com.vipgp.tinyurl.dubbo.provider.persistence;

import lombok.Data;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/12 14:45
 */
@Data
public class PrepareResult {

    private long xid;
    private int currentWritePos;
}
