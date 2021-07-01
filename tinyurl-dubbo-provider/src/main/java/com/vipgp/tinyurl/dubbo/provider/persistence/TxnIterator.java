package com.vipgp.tinyurl.dubbo.provider.persistence;

import java.io.IOException;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/27 1:20
 */
public interface TxnIterator {

    boolean next() throws IOException;

    void close() throws IOException;

    TxnLogModel getTxn();
}
