package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import com.vipgp.tinyurl.dubbo.provider.persistence.Const;
import com.vipgp.tinyurl.dubbo.provider.persistence.Util;
import com.vipgp.tinyurl.dubbo.provider.system.HealthMonitor;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/8 0:16
 */
@Slf4j
@Component
public class CLogCommitTask implements EventHandler<LogCommitEvent>, CLogCommitProcessor {

    @Autowired
    private RingBuffer<LogCommitEvent> ringBuffer;

    @Autowired
    HealthMonitor healthMonitor;

    @NacosValue(value = "${txn.log.dir}",autoRefreshed = true)
    private String txnDirPath;
    /**
     * delay ms commit log
     */
    @NacosValue(value = "${txn.log.commit.delay:1000}",autoRefreshed = true)
    private Integer commitDelayMillis;
    /**
     * delay transaction counts
     */
    @NacosValue(value = "${txn.log.commit.no.delay.count:5000}",autoRefreshed = true)
    private Integer commitDelayCount;

    private ConcurrentHashMap<String, FileHashEntry> map=new ConcurrentHashMap();

    /**
     * if reach the count, then will remove the map item
     */
    private int emptyCountReachDel=10;

    private ScheduledThreadPoolExecutor logCommitTask=new ScheduledThreadPoolExecutor(1,new NamedThreadFactory("Log-Commit"));

    @PostConstruct
    private void init() {
        if (whetherGroupCommit()) {
            long period = (1000L > commitDelayMillis && commitDelayMillis > 0) ? commitDelayMillis : 1000L;
            logCommitTask.scheduleAtFixedRate(new CommitLog(), 0, period, TimeUnit.MILLISECONDS);
        }
    }

    private boolean whetherGroupCommit() {
        return commitDelayCount > 0 && commitDelayMillis > 0;
    }

    private synchronized Map.Entry<String, FileHashEntry> createIfAbsent(String key){
        FileHashEntry entry = map.get(key);
        if (entry == null) {
            Integer[] offsets = new Integer[commitDelayCount];
            long lastCommitTime = System.currentTimeMillis();
            entry=new FileHashEntry(offsets,lastCommitTime);
            map.put(key,entry);
        }

        Iterator<Map.Entry<String, FileHashEntry>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FileHashEntry> item = iterator.next();
            if(key.equals(item.getKey())){
                return item;
            }
        }

        // should not reach here
        return null;
    }


    private void groupCommit(LogCommitEvent event) throws IOException {
        long start=System.nanoTime();
        Map.Entry<String, FileHashEntry> item =createIfAbsent(event.getFileName());
        // lock, time out or commit delay count reach will trigger group commit
        synchronized (item.getKey()) {
            FileHashEntry entry=item.getValue();
            Integer[] offsets = entry.offsets;

            offsets[entry.size] = event.getOffset();
            entry.size = entry.size + 1;

            // array is full, then commit
            if (entry.size >= commitDelayCount) {
                log.info("the offsets is full, need commit, key {} size {} commitDelayCount {}", item.getKey(), entry.size, commitDelayCount);
                doGroupCommit(event.getFileName());
            }
        }

        long end = System.nanoTime();
        log.info("commit log group commit, cost {}ns {}ms", end - start, (end - start) / 1000000);
    }

    /**
     * need to be locked outside
     * @param fileName
     * @return
     * @throws IOException
     */
    private int doGroupCommit(String fileName) throws IOException {
        try {
            long start = System.nanoTime();
            log.info("do group commit for txn commit log {}", fileName);
            FileHashEntry entry = map.get(fileName);
            if (entry == null) {
                return 0;
            }
            File txnFileWrite = new File(txnDirPath, fileName);
            if (!txnFileWrite.exists()) {
                log.error("file not exist, file name {}", fileName);
                return 0;
            }
            Integer[] buffer = entry.offsets;
            long length = txnFileWrite.length();
            log.info("do group commit for txn commit log {}, length {}", fileName, length);
            // wait to commit
            if (buffer == null || buffer.length == 0) {
                return 0;
            }
            if (buffer[0] == null) {
                return 0;
            }
            if (length <= 0) {
                return 0;
            }

            FileChannel channel = new RandomAccessFile(txnFileWrite, "rw").getChannel();
            MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_WRITE, 0, length);
            int count = 0;
            for (int i = 0; i < entry.size; i++) {
                if (buffer[i] == null) {
                    break;
                }
                count = count + 1;
                int offsetPos = buffer[i];
                int pos = Util.calcStatusPosition(offsetPos);
                if (pos > length) {
                    log.error("exception: pos is out of length bounds, pos {}, length {}, file name {}", pos, length, fileName);
                    healthMonitor.unhealth(Constants.CACHE_KEY_CLOGCOMMIT,
                            "offset " + pos + " is out of file length " + length+ ", file name "+fileName);
                    mbb.clear();
                    channel.close();
                    return 0;
                } else {
                    mbb.put(pos, (byte) Const.STATUS_COMMIT);
                }
            }

            mbb.force();
            channel.force(false);
            channel.close();
            // reset, loop again
            reset(entry);

            long end = System.nanoTime();
            log.info("do group commit for txn commit log {}, {} records commit, cost {}ns {}ms", fileName, count, end - start, (end - start) / 1000000);

            return count;
        } catch (Exception ex) {
            healthMonitor.unhealth(Constants.CACHE_KEY_CLOGCOMMIT, "C log commit exception:" + ex);
            log.error("C log commit exception", ex);
            throw ex;
        }
    }

    private void reset(FileHashEntry entry){
        // reset array, reused memory, do not renew array as it maybe fail if there is insufficient memory
        resetBuffer(entry.offsets);
        // reset to 0
        entry.size=0;
        // update time
        entry.lastCommitTime=System.currentTimeMillis();
    }

    /**
     * reset offsets
     */
    private void resetBuffer(Integer[] buffer){
        for (int i = 0; i < buffer.length; i++) {
            buffer[i]=null;
        }
    }

    @Override
    public void commit(LogCommitEvent event) throws IOException {
        if(whetherGroupCommit()){
            // put to queue
            offerToQueue(event.getFileName(),event.getOffset());
        }else {
            syncCommit(event);
        }
    }



    private void syncCommit(LogCommitEvent event) throws IOException {
        File txnFileWrite = new File(txnDirPath, event.getFileName());
        if(txnFileWrite.exists()) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(txnFileWrite, "rw");
            int pos = Util.calcStatusPosition(event.getOffset());
            randomAccessFile.seek(pos);
            randomAccessFile.writeByte(Const.STATUS_COMMIT);
            randomAccessFile.close();
        }else {
            log.error("file not exist, event {}", event.toString());
        }
    }

    @PreDestroy
    public void preDestroy() {
        try {
            log.info("start txn log preDestroy");
            Thread.sleep(commitDelayMillis * 2);
            log.info("txn log preDestroy finished");
        }catch (InterruptedException ex){
            log.error("txn log thread interrupted",ex);
        }
    }


    private class CommitLog implements Runnable {
        @Override
        public void run() {
            try {
                Iterator<Map.Entry<String, FileHashEntry>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, FileHashEntry> entry = iterator.next();
                    FileHashEntry file = entry.getValue();
                    synchronized (entry.getKey()) {
                        log.info("log commit tasks run begin, key {}", entry.getKey());
                        long lastCommitTime = file.lastCommitTime;
                        long duration = System.currentTimeMillis() - lastCommitTime;
                        if (lastCommitTime != 0 && duration > commitDelayMillis) {
                            if (file.size > 0) {
                                log.info("it is time up, do log commit, key {}", entry.getKey());
                                try {
                                    int count = doGroupCommit(entry.getKey());
                                    log.info("log commit tasks run end, {} records commit, key {}", count, entry.getKey());
                                } catch (Exception ex) {
                                    log.error("log commit tasks exception, duration {} lastCommitTime {} now {}, key {}", duration, lastCommitTime,
                                            System.currentTimeMillis(), entry.getKey(), ex);
                                }
                                // reset empty count
                                file.emptyCount = 0;
                            } else {
                                file.emptyCount = file.emptyCount + 1;
                                if (file.emptyCount > emptyCountReachDel) {
                                    map.remove(entry.getKey());
                                    log.info("remove the item map, key {}", entry.getKey());
                                }
                            }
                        } else {
                            log.info("log commit tasks run end, it is not time yet, key {}", entry.getKey());
                        }
                    }
                }
            }catch (Exception ex){
                log.error("commit log commit task run exception",ex);
            }
        }
    }

    /**
     *  traffic offsets
     * @param offset
     */
    private void offerToQueue(String fileName, Integer offset){
        long sequence = ringBuffer.next();
        try {
            // producer
            LogCommitEvent event = ringBuffer.get(sequence);
            event.setFileName(fileName);
            event.setOffset(offset);
        } finally {
            // activate consumer
            // invoke in the finally block to ensure publish called anyway, if this sequence has not been published, then the queue will be blocked
            ringBuffer.publish(sequence);
        }
    }

    @Override
    public void onEvent(LogCommitEvent event, long sequence, boolean endOfBatch) throws Exception {
        log.info("commit log event trigger, event {}", event.toString());
        groupCommit(event);
    }


    private class FileHashEntry{
        private Integer[] offsets =null;
        private volatile long lastCommitTime;
        private int size;
        private int emptyCount;

        public  FileHashEntry(Integer[] offsets, long lastCommitTime){
            this.offsets = offsets;
            this.lastCommitTime=lastCommitTime;
            this.size=0;
            this.emptyCount=0;
        }
    }
}
