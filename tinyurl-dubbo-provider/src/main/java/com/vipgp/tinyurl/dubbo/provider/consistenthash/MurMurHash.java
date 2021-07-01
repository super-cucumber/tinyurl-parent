package com.vipgp.tinyurl.dubbo.provider.consistenthash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
public class MurMurHash implements Hashable {

    @Override
    public long hash(String key) {
        long rv = 0;
        ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
        int seed = 0x1234ABCD;

        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        rv = seed ^ (buf.remaining() * m);

        long ky;
        while (buf.remaining() >= 8) {
            ky = buf.getLong();

            ky *= m;
            ky ^= ky >>> r;
            ky *= m;

            rv ^= ky;
            rv *= m;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(
                    ByteOrder.LITTLE_ENDIAN);
            // for big-endian version, do this first:
            // finish.position(8-buf.remaining());
            finish.put(buf).rewind();
            rv ^= finish.getLong();
            rv *= m;
        }

        rv ^= rv >>> r;
        rv *= m;
        rv ^= rv >>> r;
        buf.order(byteOrder);

        return rv & 0xffffffffL; /* Truncate to 32-bits */
    }
}
