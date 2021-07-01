package com.vipgp.tinyurl.dubbo.provider.manager;

import java.io.IOException;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/21 16:10
 */
public interface RecoverManager {

    /**
     * recover base on txn log and snapshot
     * @throws IOException
     */
    int recover() throws IOException;
}
