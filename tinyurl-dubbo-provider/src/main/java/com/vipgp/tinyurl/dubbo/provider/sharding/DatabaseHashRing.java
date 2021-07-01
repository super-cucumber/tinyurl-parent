package com.vipgp.tinyurl.dubbo.provider.sharding;

import com.vipgp.tinyurl.dubbo.provider.consistenthash.ConsistentHashProvider;
import com.vipgp.tinyurl.dubbo.provider.consistenthash.DatabaseNode;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
@Component
public class DatabaseHashRing {

    /**
     * default virtual node count
     */
    private final int defaultVirtualNodeNumber=65535;

    //@Resource
    //private ShardingDataSource shardingDataSource;

    @Resource
    private DataSource dataSource;

    /*@Autowired
    public DatabaseHashRing(ShardingDataSource shardingDataSource){
        this.shardingDataSource=shardingDataSource;
    }*/

    /**
     * ring hash init
     */
    private ConsistentHashProvider provider=null;

    @PostConstruct
    public void init(){
       ShardingDataSource shardingDataSource= (ShardingDataSource)dataSource;
        // get all databases
        ShardingRule rule=shardingDataSource.getRuntimeContext().getRule();
        Collection<String> dataSourceNames =rule.getShardingDataSourceNames().getDataSourceNames();
        // init database nodes
        List<DatabaseNode> databaseNodes=new ArrayList<>();
        dataSourceNames.stream().forEach(item-> databaseNodes.add(new DatabaseNode(item)));

        provider=new ConsistentHashProvider(databaseNodes,defaultVirtualNodeNumber);
    }

    public ConsistentHashProvider getProvider() {
        return provider;
    }

}
