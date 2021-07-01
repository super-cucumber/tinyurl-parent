package com.vipgp.tinyurl.dubbo.provider.manager;

import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/15 10:07
 */
public interface LookupManager {
    /**
     * 根据key获取value
     * @param key
     * @return
     */
    String getValue(String key);

    /**
     * lookup refresh to cache
     */
    void refreshToCache();

    /**
     * 获取所有keys
     * @return
     */
    List<String> getKeys();

    /**
     * 获取所有的values
     * @return
     */
    List<String> getValues();
}
