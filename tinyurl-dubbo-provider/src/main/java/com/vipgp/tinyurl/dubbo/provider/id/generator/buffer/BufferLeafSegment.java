package com.vipgp.tinyurl.dubbo.provider.id.generator.buffer;

import com.vipgp.doraemon.service.dubbo.IDGenerateService;
import com.vipgp.doraemon.service.dubbo.LeafSegment;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/21 10:27
 */
@Slf4j
@Component
public class BufferLeafSegment  {

    @Autowired
    private LookupManager lookupManager;

    @Reference(version = "1.0.0")
    IDGenerateService idGenerateService;

    /**
     * 最大步长1500,0000
     * e.g. TPS 2.5万,10分钟消耗完
     */
    private static final int MAX_STEP=15000000;

    /**
     * 一个segment维持时间为10分钟
     */
    private static final long SEGMENT_DURATION=10*60*1000L;

    private volatile boolean initOK=false;
    private Map<String,SegmentBuffer> cache=new ConcurrentHashMap<>();

    /**
     * 从二方服务获取号段更新到本地buffer的task
     */
    private ExecutorService fillBufferTask= new ThreadPoolExecutor(1,1,0L,
            TimeUnit.SECONDS,new SynchronousQueue<Runnable>(), new UpdateThreadFactory("Segment-Update"));

    /**
     * 使用自定义工厂类，用于指定有意义的线程名称，方便出错时回溯
     */
    public static class UpdateThreadFactory implements ThreadFactory{
        private final String namePrefix;
        private final AtomicInteger nextId = new AtomicInteger(1);

        public UpdateThreadFactory(String namePrefix){
            this.namePrefix="Thread-"+namePrefix+"-";
        }

        @Override
        public Thread newThread(Runnable r) {
            String name=namePrefix+nextId.getAndIncrement();
            Thread thread=new Thread(r,name);
            log.info(thread.getName());
            return thread;
        }
    }

    /**
     * 获取ID
     * @param key
     * @return
     */
    public Long getId(final String key){
        if(!initOK){
            throw new BaseAppException(ErrorCode.ID_CACHE_INIT_FALSE.getCode(),ErrorCode.ID_CACHE_INIT_FALSE.getInfo());
        }
        if(cache.containsKey(key)){
            SegmentBuffer buffer=cache.get(key);
            if(!buffer.isInitOk()){
                synchronized (buffer){
                    if(!buffer.isInitOk()){
                        try{
                            fillSegment(key, buffer, buffer.getCurrent());
                            log.info("buffer init - fill leafkey {} {}",key,buffer.getCurrent());
                            buffer.setInitOk(true);
                        }catch (Exception ex){
                            log.warn("init buffer {} exception", buffer.getCurrent(),ex);
                        }
                    }
                }
            }

            return getIdFromSegmentBuffer(buffer);
        }else {
            throw new BaseAppException(ErrorCode.ID_KEY_NOT_EXISTS.getCode(),ErrorCode.ID_KEY_NOT_EXISTS.getInfo());
        }
    }

    /**
     * 初始化key
     * @return
     */
    @PostConstruct
    public void initDefaultCache(){
        log.info("init default cache keys");
        StopWatch sw = new Slf4JStopWatch();
        List<String> keys= lookupManager.getValues();
        if(!CollectionUtils.isEmpty(keys)) {
            for (String key : keys) {
                SegmentBuffer buffer = new SegmentBuffer();
                buffer.setKey(key);
                Segment segment = buffer.getCurrent();
                segment.setValue(new AtomicLong(0));
                segment.setMax(0);
                segment.setStep(0);
                cache.put(key, buffer);
                log.info("add key {} to cache, segmentbuffer {}", key, buffer);
            }
            log.info("init default cache keys success, key count {}",keys.size());
            initOK= true;
        }else {
            log.info("init default cache keys failed");
            initOK= false;
        }

        sw.stop("initDefaultCache");
    }

    /**
     * 填充buffer
     * @param key
     * @param buffer
     */
    public void fillSegment(String key, SegmentBuffer buffer, Segment needFillSegment){
        StopWatch sw = new Slf4JStopWatch();
        LeafSegment leafSegment=null;
        if(!buffer.isInitOk()){
            // 双buffer都没初始化
            leafSegment=idGenerateService.getSegment(key);
            buffer.setStep(leafSegment.getStep());
            buffer.setMinStep(leafSegment.getStep());
        }else if(buffer.getUpdateTimestamp()==0L){
            // 第一个buffer已初始化，但第二个buffer还未初始化
            leafSegment=idGenerateService.getSegment(key);
            buffer.setStep(leafSegment.getStep());
            buffer.setMinStep(leafSegment.getStep());
            buffer.setUpdateTimestamp(System.currentTimeMillis());
        }else {
            // 双buffer都已初始化过
            long duration = System.currentTimeMillis() - buffer.getUpdateTimestamp();
            int nextStep = buffer.getStep();
            // 根据消耗时间来决定步长大小
            if (duration < SEGMENT_DURATION) {
                // T < 15min，nextStep = step * 2
                if (nextStep * 2 > MAX_STEP) {
                    // 不能超过最大步长
                } else {
                    nextStep = nextStep * 2;
                }
            } else if (duration < SEGMENT_DURATION * 2) {
                // 15min < T < 30min，nextStep = step
                // 继续使用上一次的步长
            } else {
                // T > 30min，nextStep = step / 2
                nextStep = nextStep / 2 >= buffer.getMinStep() ? nextStep / 2 : nextStep;
            }
            log.info("leafKey[{}], step[{}], duration[{}mins], nextStep[{}]", key, buffer.getStep(),
                    String.format("%.2f",((double)duration / (1000 * 60))), nextStep);
            LeafSegment params=new LeafSegment();
            params.setKey(key);
            params.setStep(nextStep);
            leafSegment=idGenerateService.getSegment(params);
            buffer.setStep(nextStep);
            buffer.setMinStep(leafSegment.getStep());
            buffer.setUpdateTimestamp(System.currentTimeMillis());
        }

        // [n,m)
        long value=leafSegment.getMaxId() - buffer.getStep();
        needFillSegment.getValue().set(value);
        needFillSegment.setMax(leafSegment.getMaxId());
        needFillSegment.setStep(buffer.getStep());
        sw.stop("fillSegment", key + " " + needFillSegment);
    }

    /**
     * 从buffer中获取ID
     * @param buffer
     * @return
     */
    private Long getIdFromSegmentBuffer(final SegmentBuffer buffer) {
        while (true) {
            // 添加读锁
            long start=System.nanoTime();
            buffer.rLock().lock();
            try {
                final Segment segment = buffer.getCurrent();
                // 如果当前号段已消耗10%，则预读入下一段buffer
                if (!buffer.isNextReady() && (segment.getIdle() < 0.9 * segment.getStep())
                        && buffer.getThreadRunning().compareAndSet(false, true)) {
                    // 用另外的线程去异步加载下一个号段
                    fillBufferTask.execute(() -> {
                        Segment next = buffer.getSegments()[buffer.nextPos()];
                        boolean updateOK = false;
                        try {
                            fillSegment(buffer.getKey(), buffer, next);
                            updateOK = true;
                            log.info("finish fill next segment {}", next);
                        } catch (Exception ex) {
                            log.warn(buffer.getKey() + "fail fill next buffer exception", ex);
                        } finally {
                            if (updateOK) {
                                buffer.wLock().lock();
                                buffer.setNextReady(true);
                                buffer.getThreadRunning().set(false);
                                buffer.wLock().unlock();
                            } else {
                                buffer.getThreadRunning().set(false);
                            }
                        }
                    });
                }

                // 从当前号段获取ID
                long value = segment.getValue().getAndIncrement();
                // [n,m)
                if (value < segment.getMax()) {
                    // stats
                    long end=System.nanoTime();
                    log.info("get id {} from leaf segment, cost {}ns {}ms, rlock", value, end-start,(end-start)/1000000);

                    return value;
                }
            } finally {
                buffer.rLock().unlock();
            }


            // 如果执行到这里还未取到ID，则说明当前buffer已用完，需要等待下一个buffer准备好
            waitAndSleep(buffer);
            // 切换到下一个buffer，添加写锁
            buffer.wLock().lock();
            try {
                // 获得锁之后再次尝试去当前buffer读取，因为可能已被切换
                final Segment segment = buffer.getCurrent();
                long value = segment.getValue().getAndIncrement();
                // [n,m)
                if (value < segment.getMax()) {
                    // stats
                    long end=System.nanoTime();
                    log.info("get id {} from leaf segment, cost {}ns {}ms, wlock", value, end-start,(end-start)/1000000);

                    return value;
                }
                // 如果在当前号段还是获取不到，则切换到下一个号段, 再次执行循环获取
                if (buffer.isNextReady()) {
                    buffer.switchPos();
                    buffer.setNextReady(false);
                } else {
                    log.error("two segment in {} are not ready", buffer);
                    throw new BaseAppException(ErrorCode.TWO_SEGMENTS_ARE_NULL.getCode(), ErrorCode.TWO_SEGMENTS_ARE_NULL.getInfo());
                }
            } finally {
                buffer.wLock().unlock();
            }
        }
    }

    /**
     * 自旋等待
     * @param buffer
     */
    private void waitAndSleep(SegmentBuffer buffer){
        int spin=0;
        while (buffer.getThreadRunning().get()){
            // 自旋等待
            spin+=1;
            if(spin>10000){
                try{
                    TimeUnit.MILLISECONDS.sleep(10);
                    break;
                }catch (InterruptedException ex){
                    log.warn("Thread {} Interrupted",Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
}
