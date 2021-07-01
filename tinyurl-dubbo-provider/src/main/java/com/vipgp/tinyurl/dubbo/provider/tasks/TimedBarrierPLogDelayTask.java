package com.vipgp.tinyurl.dubbo.provider.tasks;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.persistence.FileTxnSnapLog;
import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/11 20:24
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "txn.prepare.log.delay.mode",havingValue = "timed-barrier")
public class TimedBarrierPLogDelayTask implements PLogDelayProcessor {

    @Autowired
    private FileTxnSnapLog fileTxnSnapLog;

    /**
     * delay ms
     */
    @NacosValue(value = "${txn.prepare.log.timed.barrier.sync.delay:1000}", autoRefreshed = true)
    private Integer syncDelayMillis;
    /**
     * delay counts
     */
    @NacosValue(value = "${txn.prepare.log.timed.barrier.sync.no.delay.count:5000}",autoRefreshed = true)
    private Integer syncDelayCount;

    @NacosValue(value = "${txn.prepare.log.timed.barrier.segments}", autoRefreshed = true)
    private Integer segmentCount;


    @NacosValue(value = "${txn.prepare.log.completable.future.threads}", autoRefreshed = true)
    private Integer completableFutureThreads;
    @NacosValue(value = "${txn.prepare.log.completable.future.core.threads.prestart}", autoRefreshed = true)
    private boolean prestartCoreThreads;

    @NacosValue(value = "${txn.prepare.log.timed.barrier.threads}", autoRefreshed = true)
    private Integer timedBarrierThreads;



    /**
     * computable future executor pool
     */
    private ThreadPoolExecutor futureExecutor = null;

    /**
     * do committer
     */
    private ThreadPoolExecutor commitExecutor = null;


    private ArrayList<Segment> segments;


    /**
     * poll running task
     */
    private ScheduledThreadPoolExecutor scheduleTask=new ScheduledThreadPoolExecutor(1,new NamedThreadFactory("Schedule-Commit-Prepare-Log"));


    @PostConstruct
    private void init(){
        // completable future executor
        if(Integer.valueOf("-1").equals(completableFutureThreads)) {
            futureExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 6000L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), new NamedThreadFactory("Timed-Barrier"));
        }else {
            futureExecutor = new ThreadPoolExecutor(completableFutureThreads, completableFutureThreads, 6000L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new NamedThreadFactory("Timed-Barrier"));
        }
        if(prestartCoreThreads){
            futureExecutor.prestartAllCoreThreads();
        }

        // committer
        commitExecutor = new ThreadPoolExecutor(timedBarrierThreads, Integer.MAX_VALUE, 6000L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("Do-Prepare-Log-Commit"));
        // init segments
        segments = new ArrayList<>(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            segments.add(new Segment(i));
        }
        scheduleTask.scheduleAtFixedRate(new PrepareLogCommitter(), 0, syncDelayMillis, TimeUnit.MILLISECONDS);

    }

    private class PrepareLogCommitter implements Runnable {

        @Override
        public void run() {
            for (Segment segment : segments) {
                long start=System.nanoTime();
                log.info("segment {} start", segment.hashKey);
                long duration = System.currentTimeMillis() - segment.lastFlushTime;
                int queueSize = segment.queue.size();
                if (duration > syncDelayMillis || queueSize >= syncDelayCount) {
                    commitExecutor.execute(new CommitRunnable(segment));
                    segment.lastFlushTime=System.currentTimeMillis();
                }else {
                    log.info("segment {}, duration {}, sync delay millis {}, queue size {}, sync delay count {}, do not need run",
                            segment.hashKey, duration, syncDelayMillis, queueSize, syncDelayCount);
                }

                long end=System.nanoTime();
                log.info("segment {} end, cost {}ns {}ms", segment.hashKey, end-start, (end-start)/1000000);

            }
        }
    }

    private class CommitRunnable implements Runnable {

        private LinkedBlockingQueue<Node> queue;
        private int hashKey;
        private Segment segment;

        public CommitRunnable(Segment segment) {
            this.queue = segment.queue;
            this.hashKey = segment.hashKey;
            this.segment = segment;
        }

        @Override
        public void run() {
            try {
                long from = System.nanoTime();
                if (log.isInfoEnabled()) {
                    //log.info("segment {}, commit prepare log task run begin", hashKey);
                }
                int queueSize = this.queue.size();
                if (queueSize == 0) {
                    //log.info("segment {}, commit prepare log task run end, no logs waiting to commit", hashKey);
                    return;
                }

                log.info("segment {}, commit prepare log task run, queue size {}", hashKey, queueSize);
                long start = System.nanoTime();
                List<TxnLogModel> target = new ArrayList<>();
                List<Node> nodes = new ArrayList<>();
                populateSource(target, nodes, segment, queueSize);
                long end = System.nanoTime();
                log.info("segment {}, node queue polled, list size {}, cost {}ns {}ms",
                        hashKey, target.size(), end - start, (end - start) / 1000000);

                // commit prepare log
                start = System.nanoTime();
                fileTxnSnapLog.prepare(target);
                end = System.nanoTime();
                log.info("segment {}, commit prepare log finish, list size {}, cost {}ns {}ms",
                        hashKey, target.size(), end - start, (end - start) / 1000000);

                start = System.nanoTime();
                // wakeup the waiter
                unpark(nodes);
                end = System.nanoTime();
                log.info("segment {}, unpark threads, node size {}, cost {}ns {}ms",
                        hashKey, nodes.size(), end - start, (end - start) / 1000000);

                long to = System.nanoTime();
                log.info("segment {}, commit prepare log task run end, queue size {}, cost {}ns {}ms",
                        hashKey, queueSize, to - from, (to - from) / 1000000);

            } catch (Exception ex) {
                log.error("segment {}, commit prepare log task run exception", hashKey, ex);
            }
        }
    }

    private void populateSource( List<TxnLogModel> target, List<Node> nodes,Segment segment, int queueSize) {
        segment.locker.lock();
        try{
            int count = 0;
            while (count <= queueSize) {
                Node node = segment.queue.poll();
                if (node != null) {
                    nodes.add(node);
                    target.add(node.model);
                    count = count + 1;
                    log.info("segment {}, id {}", segment.hashKey, node.model.getId());
                } else {
                    break;
                }
            }
        }finally {
            segment.locker.unlock();
        }
    }


    private void unpark(List<Node> nodes){
        for(Node node:nodes){
            long start=System.nanoTime();
            LockSupport.unpark(node.waiter);
            long end=System.nanoTime();
            log.info("thread {} park to unpark, id {}, cost {}ns {}ms", node.waiter.getName(), node.model.getId(), end - start, (end - start)/1000000);
        }
    }


    @Override
    public AwaitStatus await(TxnLogModel model) {
        try {
            long start=System.nanoTime();
            Segment segment = segments.get(hash(model.getId()));
            Node node=new Node(Thread.currentThread(), model);
            segment.queue.put(node);
            log.info("thread {} park begin, id {} xid {}", Thread.currentThread().getName(), model.getId(), model.getXid());
            while (model.getLogCommitEvent()==null) {
                LockSupport.park();
                if (model.getLogCommitEvent() == null) {
                    log.info("thread {} park again, offset is null, id {} xid {}", Thread.currentThread().getName(), model.getId(), model.getXid());
                }
            }
            long end=System.nanoTime();
            log.info("thread {} park end, from park to unpark, id {} xid {}, cost {}ns {}ms",
                    Thread.currentThread().getName(), model.getId(), model.getXid(),
                    end - start, (end - start) / 1000000);
            return AwaitStatus.SUCCESS;
        }catch (Exception ex){
            log.error("park await exception, model {}", model.toString(), ex);
            LockSupport.unpark(Thread.currentThread());
            return AwaitStatus.FAIL;
        }
    }

    private int hash(long id) {
        int hashKey = (int) (id % this.segmentCount);
        log.info("hash key {}", hashKey);

        return hashKey;
    }

    @Override
    public Executor getExecutor() {
        return futureExecutor;
    }

    private class Node{
        private Thread waiter;
        private TxnLogModel model;

        public Node(Thread waiter, TxnLogModel model){
            this.waiter=waiter;
            this.model=model;
        }
    }

    private class Segment {
        private Integer hashKey = null;
        private LinkedBlockingQueue<Node> queue;
        private volatile boolean isActioning=false;
        private ReentrantLock locker=new ReentrantLock();

        /**
         * the time of last flush to db
         */
        private volatile long lastFlushTime;

        public Segment(Integer hashKey) {
            this.queue = new LinkedBlockingQueue();
            this.hashKey = hashKey;
            this.lastFlushTime=System.currentTimeMillis();
        }
    }
}
