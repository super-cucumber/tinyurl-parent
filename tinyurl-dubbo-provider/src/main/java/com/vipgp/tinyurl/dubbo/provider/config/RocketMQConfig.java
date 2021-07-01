package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.mq.consumer.RollbackConsumer;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/28 20:19
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @NacosValue(value = "${rocketmq.name.server}",autoRefreshed = true)
    String nameServerAddress;

    @Autowired
    RollbackConsumer rollbackConsumer;

    @Bean
    public DefaultMQProducer defaultMQProducer() throws MQClientException{
        DefaultMQProducer producer=new DefaultMQProducer(Constants.PRODUCER_GROUP_TINY_URL);
        producer.setNamesrvAddr(nameServerAddress);
        producer.start();
        log.info("rocketmq producer start");
        return producer;
    }

    @Bean
    public DefaultMQPushConsumer defaultMQPushConsumer() throws MQClientException{
        DefaultMQPushConsumer consumer=new DefaultMQPushConsumer(Constants.CONSUMER_GROUP_TINY_URL);
        consumer.setNamesrvAddr(nameServerAddress);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe(Constants.TOPIC_ROLLBACK,"*");
        consumer.registerMessageListener(rollbackConsumer);
        consumer.start();
        log.info("rocketmq rollback consumer start");

        return consumer;
    }
}
