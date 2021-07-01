package com.vipgp.tinyurl.dubbo.provider;

import com.vipgp.tinyurl.dubbo.service.TinyurlService;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Assert;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/12 9:23
 */
@Slf4j
public class TinyUrlQueryTest extends AbstractAppTest {

    @Autowired
    TinyurlService tinyurlService;

    /**
     * 测试不存在的地址
     */
    @Test
    public void unexistCodeTest(){
        BaseResult<String> tem = tinyurlService.getLongUrl("t.vipgp88.com", "unexist");
        log.info("[TEST]t unexist test:"+tem.toString());
        Assert.assertEquals("B0004",tem.getErrorCode());

        tem = tinyurlService.getLongUrl("q.vipgp88.com", "unexist");
        log.info("[TEST]q unexist test:"+tem.toString());
        Assert.assertEquals("B0004",tem.getErrorCode());
    }

    /**
     * 测试自定义的地址
     */
    @Test
    public void existCodeTest(){
        BaseResult<String> tem = tinyurlService.getLongUrl("t.vipgp88.com", "ewipo");
        log.info("[TEST]t exist test:"+tem.toString());
        Assert.assertEquals("https://weibo.com/",tem.getResult().toString());

        tem = tinyurlService.getLongUrl("q.vipgp88.com", "eQipo");
        log.info("[TEST]q exist test:"+tem.toString());
        Assert.assertEquals("https://Qweibo.com/",tem.getResult().toString());
    }

    /**
     * 测试随机生成的地址
     */
    @Test
    public void randonCodeTest(){
        BaseResult<String> tem = tinyurlService.getLongUrl("t.vipgp88.com", "123456");
        log.info("[TEST]t randon test:"+tem.toString());
        Assert.assertEquals("https://weibo.com/",tem.getResult().toString());
    }
}
