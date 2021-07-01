# [Tiny Url - Distributed Tiny Url Generate System](http://tinyurl.vipgp88.com/)

**Official website: http://tinyurl.vipgp88.com/**

**Skywalking website: http://skywalking.vipgp88.com/**

**Kibana website: http://kibana.vipgp88.com/**

**Sentinel website: http://sentinel.vipgp88.com/**

**Dubbo Admin website: http://dubbo.admin.vipgp88.com/**

**RocketMQ website: http://rocketmq.vipgp88.com/**


## Document

[![EN doc](https://img.shields.io/badge/document-English-blue.svg)]()
[![CN doc](https://img.shields.io/badge/文档-中文版-blue.svg)]()

## Overview
### 业务场景: 微博短地址

### 新浪微博短url业务分析
##### 需求分析
1. 输入长地址，系统生成短url
2. 输入长地址，用户自定义短url
3. 访问短url, 跳转长地址
4. 用户自定义短url使用前面5位，系统生成的短url使用剩下的位数，因此用户自定义的与系统生成的不会产生竞争
5. 短地址生成后不会更新和删除

##### 并发分析 - 写QPS 2.5万、读QPS 25万
1. 活跃用户5.5亿, 每天发表2贴，1/10有链接，那么每天有1.1亿个url
2. 写并发量为：110,000,000 / 24 / 60/ 60 = 1273
3. 考虑写巅峰20倍，写qps为25460，较高
4. 读写比例10：1，读qps为254600，较高

##### 短url key位数分析
1. 7位字符
2. Base62 
3. 总量62^7=3 521 614 606 208, 约有35216亿, 每天消耗1.1亿，可以使用87年

##### 容量分析
1. 短url 64字节，长url 256字节
2. 一天的消耗：(((64+256)B)x1.1x10,000,000)/(1024x1024x1024) = 32GB
3. 一年的容量：(((64+256)B)x1.1x10,000,000x365)/(1024x1024x1024x1024) = 11TB
4. 一天产生1.1亿条记录，1年401.5亿条记录，5年2007.5亿条记录


##### 位图容量
1. 位图每一个位是1bit
2. 1天消耗：1.1x100000000/8/1024/1024=13m
3. 5年消耗：(1.1x100000000/8/1024/1024/1024)x5x365=23G
4. 7位短地址key总共需消耗：62^7bit=410G

##### 网络分析
1.  一台服务器带宽20Mbps, 一个请求字节数1KB, 则极限QPS=20Mbps/8/1KB=2560

### 系统设计
##### 流量分发
1. DNS负载 -> LVS四层负载 -> Nginx七层负载
##### 数据库高性能高可用可扩展
1. mysql分库分表，主从主备，读写分离
2. 一致性hash
3. innodb默认页大小16KB(show variables like 'innodb_page_size')
4. 一个索引节点大小：8(id, bigint)+辅助数据9=17B，每页节点数：16KB/17B=960, 索引为960叉树
5. 树高如果是4的话，960的3次方=884736000，单表记录能到8亿，查找一个值最多需要3次磁盘IO。树高如果是3的话，960的2次方=921600，单表记录92万，2次磁盘IO
6. 单表1000万条记录，基本上磁盘IO在2-3次左右
7. 5年容量规划，5个节点，每个节点400亿条记录，单表1000万条记录，每个节点4000张表

##### 缓存高性能高可用可扩展
1.  redis分片，主从主备，读写分离
2.  一致性hash
3.  本地caffine缓存
4.  redis支持ShardedJedis和JedisCluster, 但ShardedJedis不支持Lua

##### 读优化 - 减少请求数，缩短请求路径
1. 多级缓存
2. 加锁读库
3. 过半随机失效策略
4. 防止缓存击穿、缓存穿透、缓存雪崩

##### 写优化 - 组提交
1. ID生成器本地分发
2. 数据库写支持2种模式：同步提交和异步组提交。同步提交的瓶颈在数据库性能，组提交的难点在数据一致性
3. 数据库组提交，解决数据库写瓶颈
4. 数据一致性保证：WAL，2阶段提交，graceful shutdown, crash safe(txn+snapshot), 引入分布式消息队列RocketMQ做rollback
5. IO优化：padding，顺序写，零拷贝，同步延迟组提交，异步组提交


### 技术架构
##### 分层
1. Web层 -> 接入层 -> 服务层 -> 存储层

##### Project
1. tinyurl-ui, web层入口，React项目
2. tinyurl-api, 接入api层，api级别本地缓存，SpringBoot Maven项目
3. tinyurl-dubbo-provider, dubbo服务层，实现所有的业务逻辑，SpringBoot Maven项目
4. doraemon-parent, ID生成器dubbo服务，SpringBoot Maven项目

##### ID生成器
1. 基于数据库号段预读
2. 双buffer

##### 测试环境搭建与部署 - 9台ecs 1台rds
1. 3台1c2g，2Mbps(峰值)，原生部署3个节点的zookeeper集群(注册中心)和3个节点的nacos集群(配置中心)
2. 1台2c8g，1Mbps(峰值), docker部署ELK、skywalking、rocketmq(2nameserver2master2slave)
3. 1台4c8g，100Mbps(峰值), docker部署3主3从redis cluster集群，原生部署nginx
4. 1台2c16g，2Mbps(峰值)，原生部署DubboAdmin、Sentinel Website、doraemon-server dubbo服务
5. 3台4c8g，100Mbps(峰值), 原生部署3个节点的tinyurl-api、3节点的tinyurl-dubbo-provider
6. 1台4c8g RDS实例, 250g SSD, 新建3个主库，从库没有部署，读写分离时直接连接3个主库当从库


##### 运维监控
1. 健康检测：云服务商提供
2. 日志: ELK
3. 调用链监控：Skywalking
4. 指标监控：Prometheus
5. 流量监控：Sentinel

##### 上线部署
1. git+jenkins

##### 压测与调优
1. jmeter
2. top(1/H), jstat, jstack, jps,

##### Sugar
1. bin: startup.sh, shutdown.sh, stats.sh


##### 待续
1. 热点探测
2. 安全防刷
3. 数据统计





 












