package com.vipgp.tinyurl.dubbo.provider.config;

import com.vipgp.tinyurl.dubbo.provider.id.generator.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/14 14:01
 */
@Configuration
@ConditionalOnProperty(value = Constant.REDIS_SHARD_MODE_KEY,havingValue = Constant.REDIS_SHARD_MODE_REDIS_CLUSTER)
public class JedisClusterConfig {

    @Autowired
    RedisProperties prop;

    private Set<HostAndPort> getShards(){
        if(StringUtils.isEmpty(prop.getNodes())){
            return null;
        }

        Set<HostAndPort> nodes=new HashSet<>();
        String[] array= prop.getNodes().split(",");
        for(int i=0;i<array.length;i++){
            String node=array[i];
            String[] ip=node.split(":");
            HostAndPort shard=new HostAndPort(ip[0], Integer.valueOf(ip[1]));
            nodes.add(shard);
        }

        return nodes;

    }

    @Bean
    public JedisCluster jedisCluster(){
        Set<HostAndPort> shards=getShards();

        if(StringUtils.isEmpty(prop.getPassword())){
            return new JedisCluster(shards,prop.getMaxWait().intValue(),1000,1,new GenericObjectPoolConfig());
        }else {
            return new JedisCluster(shards,prop.getMaxWait().intValue(),1000,1, prop.getPassword(), new GenericObjectPoolConfig());
        }
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(prop.getMaxIdle());
        jedisPoolConfig.setMaxWaitMillis(prop.getMaxWait());
        jedisPoolConfig.setMaxTotal(prop.getMaxTotal());

        JedisClientConfiguration clientConfig=JedisClientConfiguration.builder().usePooling().poolConfig(jedisPoolConfig).build();

        RedisClusterConfiguration clusterConfig=new RedisClusterConfiguration();
        clusterConfig.setPassword(prop.getPassword());
        clusterConfig.setMaxRedirects(5);
        for(HostAndPort item : getShards()){
            clusterConfig.clusterNode(item.getHost(),item.getPort());
        }

        return new JedisConnectionFactory(clusterConfig,clientConfig);
    }
}
