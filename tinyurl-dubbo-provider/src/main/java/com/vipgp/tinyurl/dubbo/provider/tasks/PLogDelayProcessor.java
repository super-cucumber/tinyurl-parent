package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;

import java.util.concurrent.Executor;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/6 9:24
 */
public interface PLogDelayProcessor {
    AwaitStatus await(TxnLogModel model);
    Executor getExecutor();
}
