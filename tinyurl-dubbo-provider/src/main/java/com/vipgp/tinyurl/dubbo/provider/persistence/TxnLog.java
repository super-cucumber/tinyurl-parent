package com.vipgp.tinyurl.dubbo.provider.persistence;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/25 9:37
 */
public interface TxnLog {

    /**
     * append a request to the transaction log
     * @return write pos
     */
    void prepare(List<TxnLogModel> models, long wholeSize) throws IOException;

    long getLastLoggedXid() throws IOException;

    List<TxnLogModel> extractTxnLogsAfter(long lastSnapShotXid);

    void archive(File backupDir, String suffix);

    void setBaseXid(long maxLoggedXid);

    long getNextXid();

    long populateModels(List<TxnLogModel> models);

    List<String> getTxnsToFlush();

    void removeTxnFromFlushList(int index);

    List<TxnLogModel> extractTxnLogsAfter(String fileName, long lastSnapShotXid);
}
