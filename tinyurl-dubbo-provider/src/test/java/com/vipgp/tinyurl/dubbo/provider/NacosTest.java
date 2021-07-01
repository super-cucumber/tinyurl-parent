package com.vipgp.tinyurl.dubbo.provider;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/9 14:34
 */
@Slf4j
public class NacosTest extends AbstractAppTest{

    @NacosValue(value = "${tiny.url.length}",autoRefreshed = true)
    int codeLength;

    @Test
    public void testAutoRefresh(){
        for(int i=0;i<100;i++){
            log.info("legth={}", codeLength);
            try{
                Thread.sleep(3000);
            }catch (Exception ex){

            }
        }

        try{
            Thread.sleep(300000);
        }catch (Exception ex){

        }

    }
}
