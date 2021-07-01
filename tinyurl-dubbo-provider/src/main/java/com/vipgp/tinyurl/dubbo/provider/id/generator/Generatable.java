package com.vipgp.tinyurl.dubbo.provider.id.generator;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/14
 */
public interface Generatable {
    /**
     * generate sequence id
     * @return
     */
    Long generate(String tag);
}
