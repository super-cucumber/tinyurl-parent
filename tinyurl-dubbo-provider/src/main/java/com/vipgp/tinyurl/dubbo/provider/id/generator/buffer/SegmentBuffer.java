package com.vipgp.tinyurl.dubbo.provider.id.generator.buffer;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/21 9:46
 */
@Data
public class SegmentBuffer {
    private String key;
    @Setter(AccessLevel.NONE)
    private Segment[] segments;
    @Setter(AccessLevel.NONE)
    private volatile int currentPos;
    private volatile boolean nextReady;
    private volatile boolean initOk;
    @Setter(AccessLevel.NONE)
    private final AtomicBoolean threadRunning;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private final ReadWriteLock lock;

    private volatile int step;
    private volatile int minStep;
    private volatile long updateTimestamp;

    public SegmentBuffer(){
        segments=new Segment[]{new Segment(),new Segment()};
        currentPos=0;
        nextReady=false;
        initOk=false;
        threadRunning=new AtomicBoolean(false);
        lock=new ReentrantReadWriteLock();
    }


    public Segment getCurrent(){
        return segments[currentPos];
    }

    public int nextPos() {
        return (currentPos + 1) & 1; // % 2
    }

    public void switchPos(){
        currentPos=nextPos();
    }

    public Lock rLock() {
        return lock.readLock();
    }

    public Lock wLock() {
        return lock.writeLock();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SegmentBuffer{");
        sb.append("key='").append(key).append('\'');
        sb.append(", segments=").append(Arrays.toString(segments));
        sb.append(", currentPos=").append(currentPos);
        sb.append(", nextReady=").append(nextReady);
        sb.append(", initOk=").append(initOk);
        sb.append(", threadRunning=").append(threadRunning);
        sb.append(", step=").append(step);
        sb.append(", minStep=").append(minStep);
        sb.append(", updateTimestamp=").append(updateTimestamp);
        sb.append('}');
        return sb.toString();
    }
}
