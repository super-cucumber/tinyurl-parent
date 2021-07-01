# [Tiny Url - Distributed Tiny Url Generate System](http://tinyurl.vipgp88.com/)

**Official website: http://t.vipgp88.com/tyurl -> http://tinyurl.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/a10f6c43d2939f8bda1153f2e7b5dd5b/xmlnote/CAE8D2FED0A84A8AB19F6D7E70459D27/38352)

**Skywalking website: http://t.vipgp88.com/skywk -> http://skywalking.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/03eca037c30a03584c78da6091705773/xmlnote/1CE717CFBE27416ABD5E1E68DF03CA9D/38340)

**Kibana website: http://t.vipgp88.com/kibna -> http://kibana.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/f4b8161b64508802e5d4dbbbf633fc04/xmlnote/0DCF096CDB604E929B7873BE77D7D16C/38343)

**Sentinel website: http://t.vipgp88.com/sntel -> http://sentinel.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/c18001853a2f04637fdc3499033332a3/xmlnote/9569FA6E0B0D49889906A54222D70E65/38326)

**Dubbo Admin website: http://t.vipgp88.com/dubbo ->  http://dubbo.admin.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/e62b1ddd14e9ca4a5eed48f29991b117/xmlnote/CF7EFCA415D14C2A97FF3A5658ACDFF0/38372)

**RocketMQ website: http://t.vipgp88.com/rktmq -> http://rocketmq.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/38738ed4bb8be67ce72d9ab7d0b79d2e/xmlnote/71285707CF7A45A3A36010C532581CC8/38409)

**Nacos website: http://t.vipgp88.com/nacos -> http://nacos.vipgp88.com/**

![image](https://note.youdao.com/yws/public/resource/3ab76a99041fa951b429a6df0c6faee9/xmlnote/FDA7E36B45614014BCECE44277C64E65/38393)


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
6. 支持一段时间内相同的长地址生成相同的短url

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

##### ID生成器 - 修改美团Leaf使支持本地分发
1. 基于数据库号段预读
2. 双buffer

##### 测试环境搭建与部署 - 9台ecs 1台rds
1. 3台1c2g，2Mbps(峰值)，原生部署3个节点的zookeeper集群(注册中心)和3个节点的nacos集群(配置中心)
2. 1台2c8g，1Mbps(峰值), docker部署ELK、skywalking、rocketmq(2nameserver2master2slave)
3. 1台4c8g，100Mbps(峰值), docker部署3主3从redis cluster集群，原生部署nginx
4. 1台2c16g，2Mbps(峰值)，原生部署DubboAdmin、Sentinel Website、doraemon-server dubbo服务
5. 3台4c8g，100Mbps(峰值), 原生部署3个节点的tinyurl-api、3节点的tinyurl-dubbo-provider
6. 1台1c2g RDS实例, 250g SSD, 新建3个主库，从库没有部署，读写分离时直接连接3个主库当从库


##### 运维监控
1. 健康检测：云服务商提供
2. 日志: ELK
3. 调用链监控：Skywalking
4. 指标监控：Prometheus
5. 流量监控：Sentinel

##### 上线部署
1. git+jenkins

##### 压测与调优
1. ab, jmeter
2. arthas, top(1/H), jstat, jstack, jps

##### Sugar
1. sh in bin: startup.sh, shutdown.sh, stats.sh

##### 待续
1. 热点探测
2. 安全防刷
3. 数据统计

##### 架构图

![image](https://note.youdao.com/yws/public/resource/b52eae515516650abe77eaa394259bbd/xmlnote/57F0B0C3973A4598A98B8A77A3705370/38140)

##### 写流程图

![image](https://note.youdao.com/yws/public/resource/2b1e33f2ca714491e03fcbba0069bd0b/xmlnote/40B1791F6029456BAAA920B4EC15663B/38160)

##### 查询流程图

![image](https://note.youdao.com/yws/public/resource/35fef2123865110b01f91353e2a92073/xmlnote/80A0719666D745038D4BE33F4B8868BE/38231)

##### 2PC - Delay Log 

![image](https://note.youdao.com/yws/public/resource/5a9029b2bd794a92c6668dd7d96b6604/xmlnote/A53E222AF53F4E8E9CA6E73C53937BD4/38183)

##### Key Step Detail - 2PC - Delay Log - Alias Code Dispatched By System

![image](https://note.youdao.com/yws/public/resource/5a9029b2bd794a92c6668dd7d96b6604/xmlnote/C56F052EC3244F35A529435B9F6E6EEA/38208)

##### Key Step Detail - 2PC - Delay Log - Alias Code Inputted By User

![image](https://note.youdao.com/yws/public/resource/5a9029b2bd794a92c6668dd7d96b6604/xmlnote/5015B2DC29664F47AF106E7FBA03A5EF/38213)

##### Flush Logs To DB - Task

![image](https://note.youdao.com/yws/public/resource/574ce7f561e813ce045f1cf0b2708144/xmlnote/AD2B8E4DF3F54424B72890E1925569C1/38249)

##### Crash Safe - Recover

![image](https://note.youdao.com/yws/public/resource/4d995251fe6351b18814b203a450ccd1/xmlnote/784053281377487A8C3CA60B4907FD49/38259)


##### C Log Commit - Task

![image](https://note.youdao.com/yws/public/resource/a6e38a45fc7ac3c833a46f5ab8fee823/xmlnote/92A2CA285A4346A4A9A6FBDC194D4C0F/38273)

##### Leaf - ID Generate

![image](https://note.youdao.com/yws/public/resource/8e169b67af5c5dfce862957ff25ccf94/xmlnote/88BFE97E8283454FA04B238063EAEC7E/38378)

![image](https://note.youdao.com/yws/public/resource/8e169b67af5c5dfce862957ff25ccf94/xmlnote/245A862CA2DA435EAEB1C39A543CB883/38380)




### 读压测结果

1.  ECS - network: 流入57Mbps，流出53Mbps

![image](https://note.youdao.com/yws/public/resource/f863281e916bcafaf3527b0f94cde121/xmlnote/282DADB9B5A54F5AA38352E21BA3B98A/38044)

2.  ab: qps 15891

![image](https://note.youdao.com/yws/public/resource/f863281e916bcafaf3527b0f94cde121/xmlnote/B9B52533760041079AA79777A926923A/38027)


### 写压测结果-单机
启动参数：关闭偏向锁

```
nohup java -jar -Xmx2048m -Xms2048m -Xmn1024m -XX:SurvivorRatio=3 -XX:MetaspaceSize=256m -XX:-UseBiasedLocking -XX:+UnlockDiagnosticVMOptions  -XX:+LogVMOutput -XX:LogFile=/opt/tinyurl/logs/vm.log  -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution  -XX:+PrintGCDetails -XX:+PrintGCDateStamps  -XX:+PrintGCApplicationStoppedTime  -Xloggc:/opt/tinyurl/logs/tinyurl-provider-gc.log  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/tinyurl/logs/tinyurl-provider-dump.log  -javaagent:/opt/skywalking/provideragent/skywalking-agent.jar -Djava.net.preferIPv4Stack=true -Dcsp.sentinel.api.port=8085 -Dproject.name=tinyurl-dubbo-provider  -Dcsp.sentinel.dashboard.server=127.0.0.1:4899  tinyurl-dubbo-provider.jar --spring.profiles.active=prod   > /opt/tinyurl/logs/tinyurl-provider.log 2>&1 &

echo "start successfully!"

```

##### 方案1： sync db - 数据库同步提交 - 压测记录 - QPS 1476
1. 应用参数：设置数据库同步提交

```
# 开启数据库同步提交
tiny.url.db.commit.sync=true
# ID生成器预取号段
leaf_alloc.step=15000

```

2. dubbo service provider - ECS - CPU: 40%使用率

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/CA7EBE2830AE43DFAD70E71813A9787A/37706)
![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/2B07168D57D94B3085F9ED0ECC39590D/37710)

3. 数据库：cpu 56.22%， IOPS 1382.25次/秒

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/73C092FCF14D44AFB4DA88F4247433C8/37745)
![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/DAF1CEAF851B4633A0EB22E86232AC49/37749)

4. ECS - network: 流入9Mbps，流出12Mbps

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/89123BBA8B5F48208B3356C3E9550F0C/37753)

**5. ab: qps 1476**

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/C79968D236FF4F4FA7121985DF8B91CE/37719)

6. stats: nginx->api 耗时0.8ms，api->dubbo provider 耗时1.2ms

nginx-stats:

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/9562A0E6EF0846B396B89EF881E9611F/37732)

api-stats:

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/F7488901321B4DB892E52060E00E76B1/37736)

**dubbo provider stats: 接口耗时65.43ms，其中数据库事务插入耗时65ms**

![image](https://note.youdao.com/yws/public/resource/0e51c57faa631a4eafe5cb0a83a5c876/xmlnote/EF5A0165F70149A29A4F16DFB6E5BB00/37723)



##### 方案2： async db - sync log - 数据库异步提交2阶段P日志同步提交 - 压测记录 - QPS 432
1. 应用参数：设置数据库异步提交2阶段P日志同步提交


```
# ID生成器每次预取15000号段
leaf_alloc.step=15000
# 开启数据库异步提交
tiny.url.db.commit.sync=false
# 开启2PhaseCommit-P日志同步提交
txn.prepare.log.delay.commit=false
# 数据库组提交时间，过半随机策略，10s为基数
tiny.url.group.commit.sync.delay=10000
# 2PhaseCommit-C日志每隔5秒异步提交一次
txn.log.commit.delay=5000
# 2PhaseCommit-C日志累计超过10万次记录异步提交一次
txn.log.commit.no.delay.count=100000
```


2. dubbo service provider - ECS - CPU: 40%使用率

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/56715F7366F545D8BEA916F9191FA292/37649)

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/B28116FC6BBC43ED8009767A43911F88/37652)

3. 磁盘：写IOPS 1342个，写吞吐量14MBps

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/505096FCCC6F47C5B44CF6B425BB75C3/37662)


4. 数据库：cpu 1%， IOPS 17.5次/秒

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/4F3A48258A784C4AB65742E554965301/37664)

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/97446721950A42E592D9EAB77E35DD36/37667)

5. ECS - network: 流入6Mbps，流出4Mbps

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/8C46276D37D64A68B8531A44BC9C0E8C/37675)

**6. ab: qps 432**

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/2EB4E2093C004223B3CC6D03C50DEB65/37680)

7. stats: nginx -> api 耗时0.5ms，api->dubbo provider耗时0.5ms

nginx-stats:

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/F00F00674D2B4DF3BF53FDA0F4060629/37686)

api-stats:

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/F9B274991B1B4C13AAF6672DA8F74DC0/37688)

**dubbo provider stats: 接口耗时229.8ms，其中写磁盘耗时229.03ms，包括 sync disk 耗时2.27ms和锁竞争耗时226.76ms**

![image](https://note.youdao.com/yws/public/resource/8d28ea939437723e7fc0b23c1a844a52/xmlnote/6EC61B07112944328A5D398F8CB6458F/37692)


##### 方案3(QPS最高)： async db - delay log - 数据库异步提交2阶段P日志延迟提交 - 压测记录 - QPS 2872
1. 应用参数：设置数据库异步提交2阶段P日志延迟提交


```
# ID生成器每次预取15000号段
leaf_alloc.step=15000
# 开启数据库异步提交
tiny.url.db.commit.sync=false
# 开启日志延迟提交
txn.prepare.log.delay.commit=true
# 延迟等待算法用timed-barrier
txn.prepare.log.delay.mode=timed-barrier
# 数据库组提交时间，过半随机策略，10s为基数
tiny.url.group.commit.sync.delay=10000
# 禁用重复ID检查
tiny.url.id.duplicate.validate.enable=false
# 延迟7毫秒提交，本机环境最佳值
txn.prepare.log.timed.barrier.sync.delay=7
# 设置1个segment，本机环境最佳值
txn.prepare.log.timed.barrier.segments=1
# 设置日志文件大小，64M	`
pre.alloc.size=64
# 2PhaseCommit-C日志每隔5秒异步提交一次
txn.log.commit.delay=5000
# 2PhaseCommit-C日志累计超过10万次记录异步提交一次
txn.log.commit.no.delay.count=100000
```



2. dubbo service provider - ECS - CPU: 80%使用率

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/7EF2A7F7C54C4E4793F662F5F7EF958E/37579)

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/6D81A09D1BC44A5A99396D10520886EB/37582)

3. 磁盘：写IOPS 223个，写吞吐量 32MBps

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/D7ECC126D74B447CADC878AF52C98C01/37588)

4. 数据库：cpu 4%， IOPS  21次/秒

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/AB60C0A7941B4A72A63CB6B20D10B474/37601)

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/F29FA6F49EA44618BE74C58495BD1EEE/37603)

5. ECS - network: 流入32Mbps  流出22Mbps

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/8685E1C8A5B545018EC70206C608EECC/37608)

**6. ab: qps 2872**

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/7141D811E8864C2DA3CA466850156C94/37614)

7.  stats: nginx -> api 耗时2ms，api->dubbo provider耗时3ms

nginx stats: 

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/C57745A1D6ED4FA99AA99C54C2401A4B/37620)

api stats:

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/280D6262069040C6B78DEACCE1677F52/37623)

**dubbo-provider stats: 接口耗时28.1ms，其中写磁盘耗时15ms，包括 sync disk 耗时8.8ms和锁竞争耗时6.2ms**

![image](https://note.youdao.com/yws/public/resource/dcfeb7c26beb2bb5feb3981f5ceb63a0/xmlnote/1C50993FBA3A43A09C69E1364EF0F72C/37628)
















 












