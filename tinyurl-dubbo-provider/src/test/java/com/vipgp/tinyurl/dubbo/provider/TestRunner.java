package com.vipgp.tinyurl.dubbo.provider;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/12 23:14
 */
@Slf4j
public class TestRunner {

    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(TinyUrlCreateTest.class,TinyUrlQueryTest.class);
        for (Failure failure : result.getFailures()) {
            log.info("[TEST]failure="+failure.toString());
        }
        log.info("----------------------[TEST]success="+result.wasSuccessful()+"----------------------------");
    }
}
