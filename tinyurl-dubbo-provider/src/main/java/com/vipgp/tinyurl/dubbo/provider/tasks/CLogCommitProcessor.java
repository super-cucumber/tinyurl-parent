package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;

import java.io.IOException;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/8 0:33
 */
public interface CLogCommitProcessor {

    void commit(LogCommitEvent event) throws IOException;
}
