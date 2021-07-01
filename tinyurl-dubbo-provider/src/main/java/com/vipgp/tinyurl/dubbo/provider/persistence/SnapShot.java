package com.vipgp.tinyurl.dubbo.provider.persistence;

import java.io.File;
import java.io.IOException;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/25 9:37
 */
public interface SnapShot {
    String deserialize();
    void serialize(SnapShotModel model) throws IOException;

    long getLastProcessedXid();

    void archive(File backupDir, String suffix);
}
