package com.vipgp.tinyurl.dubbo.provider.sharding;


import com.vipgp.tinyurl.dubbo.provider.consistenthash.Node;
import com.vipgp.tinyurl.dubbo.provider.util.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.util.Collection;

/**
 * author: Sando
 * date: 2021/1/12
 */
@Slf4j
public class ConsistentShardingAlgorithm implements PreciseShardingAlgorithm<Long>, RangeShardingAlgorithm<Long> {

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Long> preciseShardingValue) {
        log.info("collection="+collection+",preciseShardingValue="+preciseShardingValue);
        // ConsistentShardingAlgorithm由ShardingAlgorithmFactory反射创建, 非IOC创建，不能依赖注入
        DatabaseHashRing databaseHashRing= SpringContextUtils.getBean(DatabaseHashRing.class);
        Node node = databaseHashRing.getProvider().routeNode(preciseShardingValue.getValue().toString());
        return node.getKey();
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return null;
    }
}
