package com.vipgp.tinyurl.dubbo.provider.util;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/5 14:17
 */
public class Constants {

    /**
     * tinyurl
     */
    public static final String CACHE_NAME_TINYURL ="tinyurl";

    /**
     * lookup
     */
    public static final String CACHE_NAME_LOOKUP ="lookup";

    /**
     * common
     */
    public static final String CACHE_NAME_COMMON ="common";

    /**
     * health
     */
    public static final String CACHE_NAME_HEALTH ="health";

    /**
     * keep the create url process step, e.g. before redis, after redis,
     */
    public static final String CACHE_NAME_DB_FLUSH="dbflush";

    /**
     * worker id key
     */
    public static final String CACHE_KEY_WORKERID ="workerid";

    /**
     * rocketmq
     */
    public static final String CACHE_KEY_ROCKETMQ ="rocketmq";

    /**
     * clogcommit
     */
    public static final String CACHE_KEY_CLOGCOMMIT ="clogcommit";

    /**
     * disruptor
     */
    public static final String CACHE_KEY_DISRUPTOR ="disruptor";

    /**
     * recover
     */
    public static final String CACHE_KEY_RECOVER ="recover";

    /**
     * rocketmq topic - rollback
     */
    public static final String TOPIC_ROLLBACK ="rollback";

    /**
     * rocketmq message tags
     */
    public static final String TAGS_TINY_URL ="tinyurl";

    public static final String CHECKPOINT_BEFORE_REDIS ="before_redis";
    public static final String CHECKPOINT_AFTER_REDIS ="after_redis";

    public static final String PRODUCER_GROUP_TINY_URL ="producer-tinyurl";
    public static final String CONSUMER_GROUP_TINY_URL ="consumer-tinyurl";

}
