package com.vipgp.tinyurl.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/25 11:04
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TinyurlApiApplication.class)
public class AppTest {

    @Test
    public void logTest(){
        log.info("begin");
        log.info("end");
    }
}
