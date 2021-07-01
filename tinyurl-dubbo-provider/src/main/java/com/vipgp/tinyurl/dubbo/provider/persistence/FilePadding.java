package com.vipgp.tinyurl.dubbo.provider.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/28 1:07
 */
@Slf4j
@Component
public class FilePadding {

    /**
     * 67108864 b= 64*1024*1024 b=64MB
     */
    @Value(value = "${pre.alloc.size:64}")
    private long preAllocSize;
    private static final ByteBuffer fill=ByteBuffer.allocateDirect(1);

    private long currentSize=0;

    public long padFile(FileChannel fileChannel) throws IOException {
        if (currentSize < preAllocSize) {
            fileChannel.write((ByteBuffer) fill.position(0), getPreAllocSize() - fill.remaining());
        }

        return preAllocSize;
    }

    public long getPreAllocSize(){
        return preAllocSize*1024*1024;
    }
}
