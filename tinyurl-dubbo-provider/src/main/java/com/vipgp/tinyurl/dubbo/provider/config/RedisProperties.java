package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/14 14:50
 */
@Component
@Data
public class RedisProperties {

    @NacosValue("${spring.redis.cluster.nodes}")
    private String nodes;
    @NacosValue("${spring.redis.password}")
    private String password;
    @NacosValue("${spring.redis.database:0}")
    private Integer database;

    @NacosValue("${spring.redis.maxTotal:8}")
    private Integer maxTotal;
    @NacosValue("${spring.redis.maxIdle:8}")
    private Integer maxIdle;
    @NacosValue("${spring.redis.maxWaitMillis:-1}")
    private Long maxWait;
}
