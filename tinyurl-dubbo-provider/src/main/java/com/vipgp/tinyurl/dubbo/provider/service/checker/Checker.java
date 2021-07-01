package com.vipgp.tinyurl.dubbo.provider.service.checker;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/18 16:12
 */
public interface Checker<T> {
    void check(T t);
}
