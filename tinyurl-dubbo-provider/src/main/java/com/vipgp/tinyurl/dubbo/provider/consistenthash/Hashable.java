package com.vipgp.tinyurl.dubbo.provider.consistenthash;

/**
 * author: Sando
 * date: 2021/1/13
 */
public interface Hashable {
    long hash(String key);
}
