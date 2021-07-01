package com.vipgp.tinyurl.dubbo.provider.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/10 0:16
 */
@Slf4j
public class BitMapShardingUtil {

    /**
     * 获取分片的bit key
     * @param id
     * @return
     */
    public static String getBitKey(long id, String baseUrlKey){
        return baseUrlKey+"_"+calcSlot(id);
    }

    /**
     * 按2^32次方分块
     * @param id
     * @return
     */
    public static long calcSlot(long id){
       long slot=id>>32;
       return slot;
    }

    /**
     * 按2^32次方取余
     * @param id
     * @return
     */
    public static long calcIndex(long id){
        long index=id % (1L<<32);
        return index;
    }

    public static void main(String[] args) {
        long slot=calcSlot(55);
        log.info("slot="+slot);

        // 62^7=3 521 614 606 208
        slot=calcSlot(3521614606208L);
        log.info("slot="+slot);

        long index=calcIndex(55);
        log.info("index="+index);

        // 62^7=3 521 614 606 208
        index=calcIndex(3521614606208L);
        log.info("index="+index);
    }
}
