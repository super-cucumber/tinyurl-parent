package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.mq.message.CreationEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.extension.SpringExtensionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/23 0:57
 */
@Slf4j
@Component
public class DisruptorDbCommitTask implements EventHandler<CreationEvent>,DbCommitProcessor {

    @Autowired
    private RingBuffer<CreationEvent> ringBuffer;
    @Autowired
    private TinyManager tinyManager;

    /**
     * delay ms insert tinyurl to db, if 0, then insert to db instant
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.delay:1000}", autoRefreshed = true)
    private Integer syncDelayMillis;
    /**
     * delay transaction counts
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.no.delay.count:5000}",autoRefreshed = true)
    private Integer syncDelayCount;

    @NacosValue(value = "${tiny.url.db.commit.sync}",autoRefreshed = true)
    private boolean dbCommitSync;

    /**
     * half random strategy
     */
    private Integer actualSyncDelayMillis;
    /**
     * half random strategy
     */
    private Integer actualSyncDelayCount;

    /**
     * buffer array
     * 1. crash safe
     */
    private CreationEvent[] buffer=null;
    /**
     * array size
     */
    private volatile int size =0;

    /**
     * the time of last flush to db
     */
    private volatile long lastFlushTime;

    /**
     * last xid in the snapshot
     */
    private long lastXid;

    /**
     * delay ms and delay count lock
     */
    private Object lock=new Object();


    /**
     * flush db
     */
    private ScheduledThreadPoolExecutor flushToDbTask=null;

    @PostConstruct
    private void init() {
        if (syncDelayCount > 0 && syncDelayMillis > 0 && !dbCommitSync) {
            actualSyncDelayCount = syncDelayCount / 2 + new Random().nextInt(syncDelayCount / 2);
            actualSyncDelayMillis = syncDelayMillis / 2 + new Random().nextInt(syncDelayMillis / 2);

            long period = (1000L > actualSyncDelayMillis && actualSyncDelayMillis > 0) ? actualSyncDelayMillis : 1000L;
            flushToDbTask=new ScheduledThreadPoolExecutor(1,new NamedThreadFactory("Disruptor-Flush-To-Db"));
            flushToDbTask.scheduleAtFixedRate(new DisruptorFlushToDb(), 10000, period, TimeUnit.MILLISECONDS);
        }
    }

    private class DisruptorFlushToDb implements Runnable{
        @Override
        public void run() {
            log.info("flush to db tasks run begin");
            long duration=System.currentTimeMillis() - lastFlushTime;
            if(lastFlushTime!=0 && duration> actualSyncDelayMillis){
                synchronized (lock){
                    /**
                     * clear dirty data
                     */
                    clearDirtyBuffer();
                    // check again, it is possible it has been flushed in last lock clock
                    duration=System.currentTimeMillis() - lastFlushTime;
                    if(duration > actualSyncDelayMillis) {
                        List<TinyRawUrlRelDO> target = copy();
                        // batch insert, it costs times when sync insert to db for each txn. it is group commit.
                        if (!CollectionUtils.isEmpty(target)) {
                            tinyManager.batchInsert(target,lastXid);
                            // reset to loop again
                            reset();
                            log.info("flush to db tasks run end, wait actualSyncDelayMillis {}, " +
                                    "batch insert length {}", actualSyncDelayMillis, target.size());
                        }
                    }
                }

            }else {
                log.info("flush to db tasks run end, it is not time yet");
            }
        }
    }

    private void reset(){
        // reset array, reused memory, do not renew array as it maybe fail if there is insufficient memory
        resetBuffer();
        // reset to 0
        size = 0;
        // update time
        lastFlushTime=System.currentTimeMillis();
    }

    /**
     * reset buffer
     */
    private void resetBuffer(){
        for (int i = 0; i < buffer.length; i++) {
            buffer[i]=null;
        }
    }

    /**
     * copy and reset buffer
     */
    private List<TinyRawUrlRelDO> copy() {
        // no items
        if (size == 0) {
            return null;
        }
        // sequential write
        List<TinyRawUrlRelDO> target = new ArrayList<>(size);
        try {
            for (int i = 0; i < size; i++) {
                CreationEvent item = buffer[i];
                if (item == null) {
                    break;
                }
                TinyRawUrlRelDO entity = new TinyRawUrlRelDO();
                entity.setId(item.getId());
                entity.setBaseUrl(item.getBaseUrlKey());
                entity.setTinyUrl(item.getAliasCode());
                entity.setRawUrl(item.getRawUrl());
                target.add(entity);
            }
            // max xid
            lastXid = buffer[size - 1] == null ? 0L : buffer[size - 1].getXid();
        } catch (Exception ex) {
            log.error("copy exception size {}, buffer length {}", size, buffer == null ? 0 : buffer.length, ex);
        }

        return target;
    }

    /**
     * push to queue, the disruptor queue can be considered as buffer
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     * @param xid
     */
    @Override
    public void offerToQueue(Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode, Long xid) {
        long sequence = ringBuffer.next();
        try {
            // producer
            CreationEvent event = ringBuffer.get(sequence);
            builder(event, id, rawUrl, baseUrlKey, baseUrl, aliasCode, xid);
        } finally {
            // activate consumer
            // invoke in the finally block to ensure publish called anyway, if this sequence has not been published, then the queue will be blocked
            ringBuffer.publish(sequence);
        }
    }

    /**
     * builder event
     * @param event
     * @param id
     * @param rawUrl
     * @param baseUrlKey
     * @param baseUrl
     * @param aliasCode
     */
    private void builder(CreationEvent event,Long id, String rawUrl, String baseUrlKey, String baseUrl, String aliasCode, Long xid){
        event.setAliasCode(aliasCode);
        event.setBaseUrl(baseUrl);
        event.setRawUrl(rawUrl);
        event.setBaseUrlKey(baseUrlKey);
        event.setId(id);
        event.setXid(xid);
    }

    @Override
    public void onEvent(final CreationEvent event, long sequence, boolean endOfBatch) throws Exception {
        synchronized (lock) {
            if (buffer == null) {
                buffer = new CreationEvent[actualSyncDelayCount];
                lastFlushTime = System.currentTimeMillis();
                size=0;
            }

            /**
             * clear dirty caused by reset method exception
             */
            clearDirtyBuffer();

            // put self record
            buffer[size] = event;
            size = size + 1;

            //the array is full
            if (size >= actualSyncDelayCount) {
                List<TinyRawUrlRelDO> target = copy();
                // batch insert
                if (!CollectionUtils.isEmpty(target)) {
                    // take snapshot and batch insert should be in the transaction, the data consistence can be ensured by db transaction
                    // the snapshot will be used in crash safe
                    tinyManager.batchInsert(target,lastXid);
                    // if reset fail, clearDirtyBuffer method erase the dirty data
                    reset();
                    log.info("actualSyncDelayCount {} batch insert length {}", actualSyncDelayCount, target.size());
                }
            }

        }

    }

    /**
     * reset again
     */
    private void clearDirtyBuffer(){
        if(buffer[0]!=null) {
            long smallestXid = buffer[0].getXid();
            // current smallest xid should be greater than xid in snapshot which has been flush to db
            if (smallestXid <= lastXid) {
                reset();
            }
        }
    }

    /**
     * graceful shutdown
     * dubbo add listener to ContextClosedEvent event, it will destroy register and protocol to refuse new request,
     * also it will wait until ongoing request processed
     * so there only need wait 2 actualSyncDelayMillis, make sure buffer has been flushed to DB
     * @see SpringExtensionFactory.ShutdownHookListener
     */
    @PreDestroy
    public void preDestroy() {
        try {
            log.info("start delay tiny manager preDestroy");
            if(actualSyncDelayMillis!=null) {
                Thread.sleep(actualSyncDelayMillis * 2);
            }
            log.info("delay tiny manager preDestroy finished, actualSyncDelayMillis {}", actualSyncDelayMillis);
        }catch (InterruptedException ex){
            log.error("delay tiny manager thread interrupted",ex);
        }
    }

}
