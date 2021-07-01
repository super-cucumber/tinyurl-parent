package com.vipgp.tinyurl.dubbo.provider.manager;

import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/7 0:27
 */
public interface RedisManager {

    String set(String key, String value);

    String set(String key, String value, long expiredSecond);

    boolean setbit(String key, long offset, boolean bitValue);

    String get(String key);

    boolean getbit(String key, long offset);

    Object runLua(String script, List<String> keys, List<String> args);

    boolean addTinyUrlToCache(Long id, long xid, String baseUrlKey, String aliasCodeEncode, String rawUrl, long newlyTinyUrlKeyExpiredSecond);

    boolean rollbackTinyUrlFromCache(Long id, long xid, String baseUrlKey, String aliasCodeEncode, String workerId, long messageCreateTime);

    void refreshRawUrlToCache(String rawUrl, String baseUrlKey, String tinyUrl, long rawUrlKeyExpiredSecond);

    String setRawUrl(String key, String value, long expiredSecond);

    String queryRawUrl(String tinyUrlKey);

    String queryXid(String key, String baseUrlKey, String aliasCode);

    boolean getbit(String bitKey, long offset, String baseUrlKey, String aliasCode);

    boolean setbit(String key, long offset, boolean bitValue, String baseUrlKey, String aliasCode);

    boolean setnx(String key, String value, long expiredSecond);

    boolean setxx(String key, String value, long expiredSecond);

    boolean lock(String key, String value, long expiredSecond);

    boolean unlock(String key, String value);

    boolean extendLock(String key, String value, long expiredSecond);

    boolean checkRollbackAlready(long xid, String workerId, String aliasCode);
}
