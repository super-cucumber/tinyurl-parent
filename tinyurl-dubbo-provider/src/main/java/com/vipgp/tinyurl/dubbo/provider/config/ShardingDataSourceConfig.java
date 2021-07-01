package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.google.common.collect.Lists;
import com.vipgp.tinyurl.dubbo.provider.sharding.ConsistentShardingAlgorithm;
import com.vipgp.tinyurl.dubbo.provider.util.DataSourceUtil;
import org.apache.shardingsphere.api.config.masterslave.MasterSlaveRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.InlineShardingStrategyConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.ShardingStrategyConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/29 9:51
 */
@Configuration
public class ShardingDataSourceConfig {

    @NacosValue(value = "${spring.shardingsphere.sharding.binding-tables}")
    private String bindingTables;
    @NacosValue(value = "${spring.shardingsphere.sharding.default-database-strategy.standard.sharding-column}")
    private String shardingColumn;
    @NacosValue(value = "${spring.shardingsphere.props.sql.show}")
    private String sqlShow;
    @NacosValue(value = "${spring.shardingsphere.sharding.tables.tiny_raw_url_rel.actual-data-nodes}")
    private String tinyurlActualDataNodes;
    @NacosValue(value = "${spring.shardingsphere.sharding.tables.tiny_raw_url_rel.table-strategy.inline.algorithm-expression}")
    private String inlineAlgorithmExpression;

    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-0.url}")
    private String dsMaster0Url;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-0.username}")
    private String dsMaster0Username;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-0.password}")
    private String dsMaster0Password;

    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-0-slave-0.url}")
    private String dsMaster0Slave0Url;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-0-slave-0.username}")
    private String dsMaster0Slave0Username;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-0-slave-0.password}")
    private String dsMaster0Slave0Password;

    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-1.url}")
    private String dsMaster1Url;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-1.username}")
    private String dsMaster1Username;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-1.password}")
    private String dsMaster1Password;

    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-1-slave-0.url}")
    private String dsMaster1Slave0Url;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-1-slave-0.username}")
    private String dsMaster1Slave0Username;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-1-slave-0.password}")
    private String dsMaster1Slave0Password;

    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-2.url}")
    private String dsMaster2Url;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-2.username}")
    private String dsMaster2Username;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-2.password}")
    private String dsMaster2Password;

    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-2-slave-0.url}")
    private String dsMaster2Slave0Url;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-2-slave-0.username}")
    private String dsMaster2Slave0Username;
    @NacosValue(value = "${spring.shardingsphere.datasource.ds-master-2-slave-0.password}")
    private String dsMaster2Slave0Password;


    @Bean
    public DataSource dataSource() throws SQLException{
        ShardingRuleConfiguration configuration=new ShardingRuleConfiguration();
        configuration.getTableRuleConfigs().add(getTinyUrlTableRuleConfiguration());
        configuration.getBindingTableGroups().add(bindingTables);
        configuration.setDefaultDatabaseShardingStrategyConfig(new StandardShardingStrategyConfiguration(shardingColumn, new ConsistentShardingAlgorithm()));
        configuration.setMasterSlaveRuleConfigs(getMasterSlaveRuleConfiguration());

        return ShardingDataSourceFactory.createDataSource(createDataSourceMap(),configuration,getProperties());
    }

    private Properties getProperties(){
        Properties properties=new Properties();
        properties.setProperty("sql.show",sqlShow);

        return properties;
    }

    /**
     * 库到表分片规则
     * @return
     */
    private TableRuleConfiguration getTinyUrlTableRuleConfiguration(){
        TableRuleConfiguration table=new TableRuleConfiguration(bindingTables,tinyurlActualDataNodes);
        table.setTableShardingStrategyConfig(getTinyurlTableShardingStrategy());

        return table;
    }

    /**
     * 表分片规则
     * @return
     */
    private ShardingStrategyConfiguration getTinyurlTableShardingStrategy(){
        ShardingStrategyConfiguration shardingStrategyConfiguration=new InlineShardingStrategyConfiguration(shardingColumn,inlineAlgorithmExpression);
        return shardingStrategyConfiguration;
    }

    /**
     * master -slave
     * @return
     */
    private List<MasterSlaveRuleConfiguration> getMasterSlaveRuleConfiguration(){
        MasterSlaveRuleConfiguration ds0=new MasterSlaveRuleConfiguration("ds0","ds-master-0", Arrays.asList("ds-master-0-slave-0"));
        MasterSlaveRuleConfiguration ds1=new MasterSlaveRuleConfiguration("ds1","ds-master-1", Arrays.asList("ds-master-1-slave-0"));
        MasterSlaveRuleConfiguration ds2=new MasterSlaveRuleConfiguration("ds2","ds-master-2", Arrays.asList("ds-master-2-slave-0"));

        return Lists.newArrayList(ds0,ds1,ds2);
    }

    /**
     * datasource map
     * @return
     */
    private Map<String,DataSource> createDataSourceMap(){
        Map<String,DataSource> map=new HashMap<>();
        map.put("ds-master-0", DataSourceUtil.createDataSource(dsMaster0Url,dsMaster0Username,dsMaster0Password));
        map.put("ds-master-0-slave-0", DataSourceUtil.createDataSource(dsMaster0Slave0Url,dsMaster0Slave0Username,dsMaster0Slave0Password));
        map.put("ds-master-1", DataSourceUtil.createDataSource(dsMaster1Url,dsMaster1Username,dsMaster1Password));
        map.put("ds-master-1-slave-0", DataSourceUtil.createDataSource(dsMaster1Slave0Url,dsMaster1Slave0Username,dsMaster1Slave0Password));
        map.put("ds-master-2", DataSourceUtil.createDataSource(dsMaster2Url,dsMaster2Username,dsMaster2Password));
        map.put("ds-master-2-slave-0", DataSourceUtil.createDataSource(dsMaster2Slave0Url,dsMaster2Slave0Username,dsMaster2Slave0Password));

        return map;
    }
}
