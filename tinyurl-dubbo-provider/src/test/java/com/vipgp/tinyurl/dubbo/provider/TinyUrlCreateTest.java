package com.vipgp.tinyurl.dubbo.provider;

import com.vipgp.tinyurl.dubbo.service.TinyurlService;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Random;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/12 23:31
 */
@Slf4j
public class TinyUrlCreateTest extends AbstractAppTest {

    @Resource(name = "TinyurlServiceV1")
    TinyurlService tinyurlService;


    /**
     * code不合法
     *
     */
    @Test
    public void invalidCodeTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("t.vipgp88.com","12-34","https://weibo.com/");
        log.info("[TEST]invalid code:"+result.toString());
        Assert.assertEquals("B0011",result.getErrorCode());
    }

    /**
     * base url不存在
     */
    @Test
    public void baseUrlUnexistTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("unexist.vipgp88.com","https://weibo.com/");
        log.info("[TEST]base url unexist:"+result.toString());
        Assert.assertEquals("B0010",result.getErrorCode());
    }

    /**
     * base url为空
     */
    @Test
    public void baseUrlIsEmptyTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("","https://weibo.com/");
        log.info("[TEST]base url is empty:"+result.toString());
        Assert.assertEquals("B0009",result.getErrorCode());
    }

    /**
     * 自定义code为空
     */
    @Test
    public void codeIsEmptyTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("t.vipgp88.com","","https://weibo.com/");
        log.info("[TEST]code is empty:"+result.toString());
        Assert.assertEquals("B0007",result.getErrorCode());
    }

    /**
     * 自定义code长度不合法
     */
    @Test
    public void codeTooLongTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("t.vipgp88.com","123456789","https://weibo.com/");
        log.info("[TEST]code too long test:"+result.toString());
        Assert.assertEquals("B0008",result.getErrorCode());
    }

    /**
     * 自定义code已被使用
     */
    @Test
    public void codeAlreadyUsedTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("t.vipgp88.com","123456","https://weibo.com/");
        log.info("[TEST]code already used test:"+result.toString());
        Assert.assertEquals("B0003",result.getErrorCode());
    }

    /**
     * 自定义code已存在
     */
    @Test
    public void customCreateTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("t.vipgp88.com","oewip","https://weibo.com/");
        log.info("[TEST]custom create result:"+result.toString());
        Assert.assertEquals("B0003",result.getErrorCode());
    }

    /**
     * 自定义code已存在
     */
    @Test
    public void customCreateTestQ(){
        BaseResult<String> result= tinyurlService.createTinyurl("q.vipgp88.com","oeQip","https://Qweibo.com/");
        log.info("[TEST]custom create result:"+result.toString());
        Assert.assertEquals("B0003",result.getErrorCode());
    }

    /**
     * 自定义code
     */
    @Test
    public void customRandomCreateTest() {
        Random random = new Random();
        int code = random.nextInt(10000);
        String rawUrl = "https://weibo.com/" + code;
        BaseResult<String> result = tinyurlService.createTinyurl("t.vipgp88.com", String.valueOf(code), rawUrl);
        log.info("[TEST]0-custom create result:" + result.toString());
        Assert.assertTrue(result.isSuccess());

        BaseResult<String> tem = tinyurlService.getLongUrl("t.vipgp88.com", result.getResult().replace("t.vipgp88.com/", ""));
        log.info("[TEST]1-query result:" + tem.toString());
        Assert.assertEquals( rawUrl,tem.getResult().toString());

    }

    /**
     * 随机生成code
     */
    @Test
    public void randomCreateTest(){
        BaseResult<String> result= tinyurlService.createTinyurl("t.vipgp88.com","https://weibo.com/");
        log.info("[TEST]random create result:"+result.toString());
        Assert.assertTrue(result.isSuccess());
    }

    /**
     * create then query
     */
    @Test
    public void createAndQueryTest() {
        BaseResult<String> result = tinyurlService.createTinyurl("t.vipgp88.com", "https://cqweibo.com/");
        log.info("[TEST]0-create result:" + result.toString());
        Assert.assertTrue(result.isSuccess());

        BaseResult<String> tem = tinyurlService.getLongUrl("t.vipgp88.com", result.getResult().replace("t.vipgp88.com/", ""));
        log.info("[TEST]1-query result:" + tem.toString());
        Assert.assertEquals( "https://cqweibo.com/",tem.getResult().toString());

    }

    /**
     * 循环生成code
     */
    @Test
    public void loopCreateTest(){
        for(int i=0; i<50;i++) {
            BaseResult<String> result = tinyurlService.createTinyurl("t.vipgp88.com", "https://weibo.com/");
            log.info("[TEST]loop {} create result:{}",i, result.toString());
            Assert.assertTrue(result.isSuccess());
        }
    }

    /**
     * 循环生成code
     */
    @Test
    public void threadsCreateTest(){
        for(int i=0; i<200;i++) {
            final int j=i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BaseResult<String> result = tinyurlService.createTinyurl("t.vipgp88.com", "https://weibo.com/");
                        log.info("[TEST]loop {} create result:{}", j, result.toString());
                        Assert.assertTrue(result.isSuccess());
                    }catch (Exception ex){
                        log.error("run exception",ex);
                    }
                }
            }).start();
        }

        try {
            Thread.sleep(1000000);
        }catch (Exception ex){

        }
    }
}
