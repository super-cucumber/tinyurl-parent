package com.vipgp.tinyurl.dubbo.provider.id.generator;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/25 14:19
 */
public class Constant {
    /**
     * property name
     */
    public static final String ID_GENERATOR_PROVIDER_KEY="id.generator.provider";
    public static final String REDIS_SHARD_MODE_KEY="redis.shard.mode";

    public static final String ID_GENERATOR_PROVIDER_LEAF_SEGMENT="leaf-segment";

    public static final String ID_GENERATOR_PROVIDER_LEAF_SNOWFLAKE="leaf-snowflake";

    public static final String ID_GENERATOR_PROVIDER_FLIKER="flicker";

    public static final String ID_GENERATOR_PROVIDER_LEAF_SEGMENT_LOCAL="leaf-segment-local";

    public static final String REDIS_SHARD_MODE_SHARD_JEDIS="shard-jedis";
    public static final String REDIS_SHARD_MODE_REDIS_CLUSTER="redis-cluster";

    /**
     * tiny url biz tag
     */
    public static final String BIZ_TAG_TINYURL="tinyuurl";
}
