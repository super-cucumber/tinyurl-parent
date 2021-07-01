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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/6 10:02
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "txn.prepare.log.delay.mode",havingValue = "cyclic-barrier")
public class CyclicBarrierPLogDelayTask implements PLogDelayProcessor {

    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.parties}", autoRefreshed = true)
    private Integer parties;

    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.await.time}", autoRefreshed = true)
    private Integer awaitTime;

    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.segments}", autoRefreshed = true)
    private Integer segmentCount;

    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.sleep.time}", autoRefreshed = true)
    private Integer sleepTime;

    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.hash.algorithm}", autoRefreshed = true)
    private String algorithm;

    @NacosValue(value = "${txn.prepare.log.completable.future.threads}", autoRefreshed = true)
    private Integer completableFutureThreads;
    @NacosValue(value = "${txn.prepare.log.completable.future.core.threads.prestart}", autoRefreshed = true)
    private boolean prestartCoreThreads;

    private Hashable hashable;
    private String MOD="mod";
    private String ROUND_ROBIN="roundrobin";

    @Autowired
    private FileTxnSnapLog fileTxnSnapLog;

    private ArrayList<Segment> segments;

    private ThreadPoolExecutor executor;

    @PostConstruct
    public void init() {
        segments = new ArrayList<>(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            segments.add(new Segment(parties, i));
        }

        if(MOD.equals(algorithm)){
            hashable=new ModHash(segmentCount);
        }
        if(ROUND_ROBIN.equals(algorithm)){
            hashable=new RoundRobin(segmentCount);
        }

        // completable future executor
        if(Integer.valueOf("-1").equals(completableFutureThreads)) {
            executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 6000L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), new NamedThreadFactory("Cyclic-Barrier"));
        }else {
            executor = new ThreadPoolExecutor(completableFutureThreads, completableFutureThreads, 6000L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new NamedThreadFactory("Cyclic-Barrier"));
        }
        if(prestartCoreThreads){
            executor.prestartAllCoreThreads();
        }
    }

    @Override
    public AwaitStatus await(TxnLogModel model) {
        Segment segment = segments.get(hashable.hash(model.getId()));
        return doAwait(segment, model);
    }

    private AwaitStatus doAwait(Segment segment, TxnLogModel model) {
        log.info("await, model {}", model.toString());
        // populate from segment
        LinkedBlockingQueue<TxnLogModel> queue = segment.queue;
        CyclicBarrier cyclicBarrier = segment.cyclicBarrier;
        // put to queue
        long start = System.nanoTime();
        queue.add(model);
        int numberWaiting=cyclicBarrier.getNumberWaiting();
        long end = System.nanoTime();
        log.info("queue add, parties {} waitings {} segment id {}, cost {}ns {}ms",
                cyclicBarrier.getParties(), numberWaiting, segment.hashKey, end - start, (end - start) / 1000000);

        try {
            log.info("await begin, segment id {}", segment.hashKey);
            start = System.nanoTime();
            int arriveIndex= cyclicBarrier.await(awaitTime, TimeUnit.MILLISECONDS);
            end = System.nanoTime();
            log.info("await finish, arrive index {}, segment id {}, cost {}ns {}ms", arriveIndex, segment.hashKey, end - start, (end - start) / 1000000);
        } catch (TimeoutException | InterruptedException | BrokenBarrierException timeout) {
            log.error("await time out, exception {}", timeout.toString());
            if (queue.contains(model)) {
                queue.remove(model);
                return AwaitStatus.TIME_OUT;
            } else {
                return AwaitStatus.SUCCESS;
            }
        } catch (Exception ex) {
            log.error("await exception", ex);
            queue.remove(model);
            return AwaitStatus.FAIL;
        }

        return AwaitStatus.SUCCESS;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    private class BarrierAction implements Runnable {

        private Segment segment;

        public BarrierAction(Segment segment) {
            this.segment = segment;
        }

        @Override
        public void run() {
            log.info("segment {} barrier action run", this.segment.hashKey);
            long start = System.nanoTime();
            // ensure all items in the queue in this time will be flushed to txn
            // it is very important, it is a trick for bug free
            int queueSize = this.segment.queue.size();
            long end = System.nanoTime();
            log.info("segment {} get queue size {}, cost {}ns {}ms",
                    this.segment.hashKey, queueSize, end - start, (end - start) / 1000000);

            start = System.nanoTime();
            List<TxnLogModel> target = new ArrayList<>();
            int count = 0;
            while (count <= queueSize) {
                TxnLogModel item = this.segment.queue.poll();
                if (item != null) {
                    target.add(item);
                    count = count + 1;
                } else {
                    break;
                }
            }

            end = System.nanoTime();
            log.info("segment {} queue polled, list size {}, cost {}ns {}ms",
                    this.segment.hashKey, target.size(), end - start, (end - start) / 1000000);

            // commit prepare log
            start = System.nanoTime();
            fileTxnSnapLog.prepare(target);
            end = System.nanoTime();
            log.info("segment {} barrier action end, committed prepare log, list size {}, cost {}ns {}ms",
                    this.segment.hashKey, target.size(), end - start, (end - start) / 1000000);

        }
    }

    private class Segment {
        private Integer hashKey = null;
        private CyclicBarrier cyclicBarrier;
        private LinkedBlockingQueue<TxnLogModel> queue;

        public Segment(int parties, Integer hashKey) {
            this.queue = new LinkedBlockingQueue();
            this.hashKey = hashKey;
            this.cyclicBarrier = new CyclicBarrier(parties, new BarrierAction(this));
        }
    }


    private interface Hashable {
        int hash(long id);
    }

    private class ModHash implements Hashable {

        private int mod;

        public ModHash(int segmentCount) {
            this.mod = segmentCount;
        }

        @Override
        public int hash(long id) {
            if (this.mod <= 1) {
                return 0;
            }
            int hashKey = (int) (id % this.mod);
            log.info("hash key {}", hashKey);

            return hashKey;
        }
    }

    private class RoundRobin implements Hashable {

        private AtomicInteger adder = new AtomicInteger();

        private int boundary;

        public RoundRobin(int segmentCount) {
            this.boundary = segmentCount;
        }

        @Override
        public synchronized int hash(long id) {
            if (boundary <= 1) {
                return 0;
            }
            int index = adder.intValue();
            if (index >= boundary) {
                index = 0;
                adder.set(0);
            } else {
                adder.incrementAndGet();
            }

            log.info("hash key {}", index);
            return index;
        }
    }

}
