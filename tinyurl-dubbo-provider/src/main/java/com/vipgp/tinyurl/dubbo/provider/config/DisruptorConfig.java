package com.vipgp.tinyurl.dubbo.provider.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.vipgp.tinyurl.dubbo.provider.mq.message.CreationEvent;
import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import com.vipgp.tinyurl.dubbo.provider.tasks.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/23 1:05
 */
@Slf4j
@Configuration
public class DisruptorConfig {

    @Bean
    public RingBuffer<CreationEvent> dbInsertRecordRingBuffer(EventHandler<CreationEvent> eventHandler){
        // 指定事件工厂
        TinyUrlCreationEventFactory factory=new TinyUrlCreationEventFactory();

        //指定ringbuffer字节大小，必须为2的N次方（能将求模运算转为位运算提高效率），否则将影响效率
        // 1024KB
        int bufferSize = 1024 * 1024;

        // 创建disruptor, 采用单生成者模式
        Disruptor<CreationEvent> disruptor=new Disruptor<CreationEvent>(factory,bufferSize, new NamedThreadFactory("Db-Insert-Disruptor-Consumer"),
                ProducerType.MULTI, new BlockingWaitStrategy());

        // 设置事件业务消费者处理器
        disruptor.handleEventsWith(eventHandler);

        // 启动disruptor线程
        disruptor.start();
        // 获取ringbuffer环，用于接取生产者生产的事件
        RingBuffer<CreationEvent> ringBuffer=disruptor.getRingBuffer();

        log.info("db insert records ring buffer inited");

        return ringBuffer;
    }

    /**
     * RingBuffer生产工厂,初始化RingBuffer的时候使用
     */
    private class TinyUrlCreationEventFactory implements EventFactory<CreationEvent>{
        @Override
        public CreationEvent newInstance() {
            return new CreationEvent();
        }
    }


    @Bean
    public RingBuffer<LogCommitEvent> logCommitEntryRingBuffer(EventHandler<LogCommitEvent> eventHandler){
        // 指定事件工厂
        LogCommitEventFactory factory=new LogCommitEventFactory();

        //指定ringbuffer字节大小，必须为2的N次方（能将求模运算转为位运算提高效率），否则将影响效率
        // 256KB
        int bufferSize = 1024 * 256;

        // 创建disruptor, 采用单生成者模式
        Disruptor<LogCommitEvent> disruptor=new Disruptor<LogCommitEvent>(factory,bufferSize, new NamedThreadFactory("Log-Commit-Disruptor-Consumer"),
                ProducerType.MULTI, new BlockingWaitStrategy());

        // 设置事件业务消费者处理器
        disruptor.handleEventsWith(eventHandler);

        // 启动disruptor线程
        disruptor.start();
        // 获取ringbuffer环，用于接取生产者生产的事件
        RingBuffer<LogCommitEvent> ringBuffer=disruptor.getRingBuffer();

        log.info("log commit entry ring buffer inited");

        return ringBuffer;
    }

    /**
     * RingBuffer生产工厂,初始化RingBuffer的时候使用
     */
    private class LogCommitEventFactory implements EventFactory<LogCommitEvent>{
        @Override
        public LogCommitEvent newInstance() {
            return new LogCommitEvent();
        }
    }
}
