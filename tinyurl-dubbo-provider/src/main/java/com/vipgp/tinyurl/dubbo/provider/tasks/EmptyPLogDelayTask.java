package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.Executor;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/21 10:31
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "txn.prepare.log.delay.mode",havingValue = "empty")
public class EmptyPLogDelayTask implements PLogDelayProcessor {

    @Override
    public AwaitStatus await(TxnLogModel model) {
        throw new NotImplementedException();
    }

    @Override
    public Executor getExecutor() {
        throw new NotImplementedException();
    }
}
