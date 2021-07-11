package com.vipgp.tinyurl.dubbo.provider.mq.consumer;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.mq.message.CacheEvent;
import com.vipgp.tinyurl.dubbo.provider.persistence.ProtostuffUtils;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/29 1:09
 */
@Slf4j
@Component
public class RollbackConsumer implements MessageListenerConcurrently {

    @Autowired
    RedisManager redisManager;

    @NacosValue(value = "${redis.tiny.url.lock.expired:3}", autoRefreshed = true)
    long tinyUrlLockExpired;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        log.info("consume message {}", msgs.toString());
        for (MessageExt item :
                msgs) {
            CacheEvent event = ProtostuffUtils.deserialize(item.getBody(), CacheEvent.class);
            String rollbackKey = CommonUtil.getRollbackKey(event.getWorkerId(), event.getXid(), event.getAliasCode());
            String randomValue = String.valueOf(CommonUtil.random6());
            try {
                // check rollback already, avoid duplicate consume
                if (redisManager.checkRollbackAlready(event.getXid(), event.getWorkerId(), event.getAliasCode(), event.getBaseUrlKey())) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }

                boolean lockSuccess = redisManager.lock(rollbackKey, randomValue, tinyUrlLockExpired);
                if (lockSuccess) {
                    // recheck rollback already, avoid duplicate consume
                    if (redisManager.checkRollbackAlready(event.getXid(), event.getWorkerId(), event.getAliasCode(), event.getBaseUrlKey())) {
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }

                    // do rollback
                    boolean isSuccess = redisManager.rollbackTinyUrlFromCache(event.getId(), event.getXid(),
                            event.getBaseUrlKey(), event.getAliasCode(), event.getRawUrl(), event.getLockValue());
                    if (isSuccess) {
                        // unlock, it is locked in creation process and has been extended when creation failed
                        if (!StringUtils.isEmpty(event.getLockValue())) {
                            String lockKey = CommonUtil.getLockKey(event.getBaseUrlKey(), event.getAliasCode());
                            redisManager.unlock(lockKey, event.getLockValue());
                            log.info("release lock, lockKey {} lockValue {}", lockKey, event.getLockValue());
                        } else {
                            log.info("no lock used as it is a creation with no alias code");
                        }
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } else {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                } else {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            } finally {
                redisManager.unlock(rollbackKey, randomValue);
            }
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

    }
}

