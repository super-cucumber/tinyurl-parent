package com.vipgp.tinyurl.dubbo.provider.consistenthash;

/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
public class DatabaseNode implements Node {

    private final String dbName;

    public DatabaseNode(String dbName){
        this.dbName=dbName;
    }

    /**
     * 获取key
     *
     * @return
     */
    @Override
    public String getKey() {
        return this.dbName;
    }
}
