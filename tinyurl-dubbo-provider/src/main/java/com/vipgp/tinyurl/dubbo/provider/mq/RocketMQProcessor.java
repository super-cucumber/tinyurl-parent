package com.vipgp.tinyurl.dubbo.provider.mq;

import com.vipgp.tinyurl.dubbo.provider.mq.message.CacheEvent;
import com.vipgp.tinyurl.dubbo.provider.persistence.ProtostuffUtils;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/29 16:24
 */
@Slf4j
@Component
public class RocketMQProcessor {

    @Autowired
    DefaultMQProducer producer;

    public boolean isNeedWaitMessageConsumed() {
        try {
            Date now = new Date();
            long begin = DateUtils.addMinutes(now, -3).getTime();
            long end = now.getTime();
            QueryResult result = producer.queryMessage(Constants.TOPIC_ROLLBACK, Constants.TAGS_TINY_URL, 50, begin, end);
            List<MessageExt> source = result.getMessageList();
            if (CollectionUtils.isEmpty(source)) {
                return false;
            }

            for (MessageExt item : source) {
                long duration = end - item.getStoreTimestamp();
                // 3 seconds
                if (duration < 3000) {
                    return true;
                }
            }

            return false;
        } catch (Exception ex) {
            return true;
        }
    }



    public boolean rollback(CacheEvent cacheEvent) {
        try {
            Message message = new Message(Constants.TOPIC_ROLLBACK, Constants.TAGS_TINY_URL, ProtostuffUtils.serialize(cacheEvent));
            message.setKeys(Arrays.asList(Constants.TAGS_TINY_URL));
            SendResult sendResult = producer.send(message);
            if(SendStatus.SEND_OK.equals(sendResult.getSendStatus())){
                log.error("rollback message send success, message body {} send result {}",cacheEvent.toString(),sendResult.toString());
                return true;
            }else {
                log.error("rollback message send failed, message body {} send result {}",cacheEvent.toString(),sendResult.toString());
                return false;
            }
        } catch (Exception ex) {
            log.error("message body {}", cacheEvent.toString(), ex);
            return false;
        }
    }

}
