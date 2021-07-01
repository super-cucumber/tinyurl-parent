package com.vipgp.tinyurl.dubbo.provider.tasks;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/6 9:25
 */
public enum Checkpoint {
    BEFORE_AWAIT, AFTER_AWAIT, BEFORE_PREPARE, AFTER_PREPARE, BEFORE_REDIS, AFTER_REDIS,FAIL,
}
