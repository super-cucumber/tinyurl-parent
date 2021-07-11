package com.vipgp.tinyurl.dubbo.provider.manager.impl;

import com.vipgp.tinyurl.dubbo.provider.id.generator.Constant;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.params.SetParams;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/7 0:28
 */
@Slf4j
@Component
@ConditionalOnProperty(value = Constant.REDIS_SHARD_MODE_KEY,havingValue = Constant.REDIS_SHARD_MODE_SHARD_JEDIS)
public class ShardJedisManagerImpl extends AbstractRedisManager  {

    @Autowired
    ShardedJedisPool shardedJedisPool;

    @Override
    public String set(String key, String value) {
        ShardedJedis jedis = null;
        String result = null;

        try {
            jedis = shardedJedisPool.getResource();
            result = jedis.set(key,value);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return result;

    }

    @Override
    public String set(String key, String value, long expiredSecond) {
        ShardedJedis jedis = null;
        String result = null;

        try {
            jedis = shardedJedisPool.getResource();
            //EX是秒，PX是毫秒
            SetParams params=new SetParams().ex((int)expiredSecond);
            result = jedis.set(key,value,params);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return result;

    }


    @Override
    public boolean setnx(String key, String value, long expiredSecond) {
        ShardedJedis jedis = null;

        try {
            jedis = shardedJedisPool.getResource();
            // NX是不存在时才set， XX是存在时才set， EX是秒，PX是毫秒
            SetParams params=new SetParams().ex((int)expiredSecond).nx();
            // success return OK, fail return null
            String result = jedis.set(key,value,params);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return false;

    }

    @Override
    public boolean setxx(String key, String value, long expiredSecond) {
        ShardedJedis jedis = null;

        try {
            jedis = shardedJedisPool.getResource();
            // NX是不存在时才set， XX是存在时才set， EX是秒，PX是毫秒
            SetParams params=new SetParams().ex((int)expiredSecond).xx();
            // success return OK, fail return null
            String result = jedis.set(key,value,params);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return false;

    }


    @Override
    public boolean setbit(String key, long offset, boolean bitValue) {
        ShardedJedis jedis = null;
        boolean result = false;

        try {
            jedis = shardedJedisPool.getResource();
            result = jedis.setbit(key,offset,bitValue);
        } catch (Exception e) {
            log.error("setbit key:{} offset:{} error",key,offset,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return result;

    }

    @Override
    public String get(String key) {
        ShardedJedis jedis = null;
        String result = null;

        try {
            jedis = shardedJedisPool.getResource();
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("get key:{} error",key,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return result;
    }


    @Override
    public boolean getbit(String key, long offset) {
        ShardedJedis jedis = null;
        boolean result = false;

        try {
            jedis = shardedJedisPool.getResource();
            result = jedis.getbit(key,offset);
        } catch (Exception e) {
            log.error("getbit key:{} offset:{} error",key,offset,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return result;
    }

    @Override
    public Object runLua(String script, List<String> keys, List<String> args) {
        // shard jedis does not support lua
        throw new NotImplementedException();
    }


    @Override
    public boolean addTinyUrlToCache(Long id,long xid,String baseUrlKey, String aliasCodeEncode, String rawUrl, long newlyTinyUrlKeyExpiredSecond,
            String randomValue) {
        refreshTxnXidToCache(baseUrlKey,aliasCodeEncode,xid,newlyTinyUrlKeyExpiredSecond);
        refreshIdToBitmap(id,baseUrlKey,true);
        refreshTinyUrlToCache(baseUrlKey, aliasCodeEncode, rawUrl, newlyTinyUrlKeyExpiredSecond);
        refreshTinyUrlRandomValueToCache(baseUrlKey,aliasCodeEncode, randomValue, newlyTinyUrlKeyExpiredSecond);
        refreshTxnXidEndToCache(baseUrlKey,aliasCodeEncode,xid,newlyTinyUrlKeyExpiredSecond);

        return true;
    }

    @Override
    public boolean rollbackTinyUrlFromCache(Long id, long xid, String baseUrlKey, String aliasCodeEncode, String workerId, String randomValue) {
        try {

            String tinyUrlRandomValueKey = CommonUtil.getTinyurlRandomValueKey(baseUrlKey, aliasCodeEncode);
            String valueInRedis = get(tinyUrlRandomValueKey);
            if (!StringUtils.isEmpty(valueInRedis) && !valueInRedis.equals(randomValue)) {
                // alias code has been taken by other users, do not need rollback
                return true;
            } else {
                // del tiny url key
                String tinyUrlKey = CommonUtil.getTinyurlKey(baseUrlKey, aliasCodeEncode);
                del(tinyUrlKey);

                // reset bitmap
                refreshIdToBitmap(id, baseUrlKey, false);

                // del txn xid key
                String txnLogKey = CommonUtil.getTxnLogKey(baseUrlKey, aliasCodeEncode, workerId, xid);
                del(txnLogKey);

                // del tiny url update time key
                del(tinyUrlRandomValueKey);

                // del txn xid end key
                String txnLogEndKey = CommonUtil.getTxnLogEndKey(baseUrlKey, aliasCodeEncode, workerId, xid);
                del(txnLogEndKey);

                // rollback already
                set(CommonUtil.getRollbackKey(workerId, xid, aliasCodeEncode), aliasCodeEncode, rollbackConsumeKeyExpired);

                return true;
            }
        } catch (Exception ex) {
            log.error("rollback exception", ex);
            return false;
        }
    }

    @Override
    public String queryRawUrl(String key) {
        return get(key);
    }

    @Override
    public String setRawUrl(String key, String value, long expiredSecond) {
        return set(key, value, expiredSecond);
    }

    @Override
    public boolean getbit(String bitKey, long offset, String baseUrlKey, String aliasCode) {
        return getbit(bitKey, offset);
    }

    @Override
    public String queryXid(String key, String baseUrlKey, String aliasCode) {
        return get(key);
    }



    @Override
    public boolean setbit(String key, long offset, boolean bitValue, String baseUrlKey, String aliasCode) {
        return setbit(key, offset, bitValue);
    }

    @Override
    public long del(String key){
        ShardedJedis jedis = null;
        long result = 0;

        try {
            jedis = shardedJedisPool.getResource();
            result = jedis.del(key);
        } catch (Exception e) {
            log.error("del key:{} error",key,e);
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

        return result;
    }


    @Override
    public boolean checkRollbackAlready(long xid, String workerId, String aliasCode, String baseUrlKey) {
        String value = get(CommonUtil.getRollbackKey(workerId, xid, aliasCode));
        if (StringUtils.isEmpty(value)) {
            return false;
        }

        return true;
    }

    /**
     * this is not safe delete, should be in atomic
     * @param key
     * @param expectedValue
     * @return
     */
    @Override
    public boolean unlock(String key, String expectedValue) {
        String actualValue=get(key);
        if(expectedValue.equals(actualValue)){
            del(key);
            return true;
        }
        return false;
    }
}
