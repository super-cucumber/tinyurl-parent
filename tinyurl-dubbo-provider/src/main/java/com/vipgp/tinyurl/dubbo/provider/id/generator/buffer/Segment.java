package com.vipgp.tinyurl.dubbo.provider.id.generator.buffer;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/21 9:23
 */
@Data
public class Segment {
    private AtomicLong value=new AtomicLong(0);
    private volatile long max;
    private volatile int step;

    public long getIdle(){
        return this.getMax() - getValue().get();
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder("Segment(");
        sb.append("value:");
        sb.append(value);
        sb.append(",max:");
        sb.append(max);
        sb.append(",step:");
        sb.append(step);
        sb.append(")");

        return sb.toString();
    }

}
