package com.vipgp.tinyurl.dubbo.provider.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/5 13:53
 */
@Slf4j
public class MathUtil {

    /**
     * 生成随机数
     * @param min
     * @param max
     * @return
     */
    public static int random(int min, int max){
        int result=(int)(Math.random()*(max-min)+min);

        return result;
    }


    public static void main(String[] args) {
        for(int i=0;i<10000;i++){
            log.info("result="+random(50,100));
        }
    }
}
