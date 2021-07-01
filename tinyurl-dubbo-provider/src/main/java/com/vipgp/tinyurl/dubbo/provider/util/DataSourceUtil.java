package com.vipgp.tinyurl.dubbo.provider.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.mysql.jdbc.Driver;

import javax.sql.DataSource;


/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/29 18:13
 */
public class DataSourceUtil {

    /**
     *  druid datasource
     * @param url
     * @param username
     * @param password
     * @return
     */
    public static DataSource createDataSource(final String url,String username, String password){
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(Driver.class.getName());
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);

        return druidDataSource;
    }
}
