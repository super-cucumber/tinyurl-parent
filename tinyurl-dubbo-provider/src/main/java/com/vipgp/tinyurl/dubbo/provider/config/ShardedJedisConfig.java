package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.id.generator.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.util.Hashing;
import redis.clients.jedis.util.Sharded;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/6 18:40
 */
@Configuration
@ConditionalOnProperty(value = Constant.REDIS_SHARD_MODE_KEY,havingValue = Constant.REDIS_SHARD_MODE_SHARD_JEDIS)
public class ShardedJedisConfig {

    @Autowired
    RedisProperties prop;

    @Bean
    public JedisPoolConfig jedisPool() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(prop.getMaxIdle());
        jedisPoolConfig.setMaxWaitMillis(prop.getMaxWait());
        jedisPoolConfig.setMaxTotal(prop.getMaxTotal());
        return jedisPoolConfig;
    }

    @Bean
    public List<JedisShardInfo> shards(){
        if(StringUtils.isEmpty(prop.getNodes())){
            return null;
        }

        List<JedisShardInfo> list=new ArrayList<>();
        String[] array= prop.getNodes().split(",");
        for(int i=0;i<array.length;i++){
            String node=array[i];
            String[] ip=node.split(":");
            JedisShardInfo shardInfo=new JedisShardInfo(ip[0],ip[1]);
            shardInfo.setPassword(prop.getPassword());
            list.add(shardInfo);
        }

        return list;
    }

    @Bean
    public ShardedJedisPool shardedJedisPool(GenericObjectPoolConfig poolConfig,List<JedisShardInfo> shards) {
        return new ShardedJedisPool(poolConfig, shards, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);
    }
}
