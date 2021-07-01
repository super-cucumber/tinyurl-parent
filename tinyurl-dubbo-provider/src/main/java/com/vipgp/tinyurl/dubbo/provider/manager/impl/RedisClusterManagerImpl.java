package com.vipgp.tinyurl.dubbo.provider.manager.impl;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.id.generator.Constant;
import com.vipgp.tinyurl.dubbo.provider.util.Base62Util;
import com.vipgp.tinyurl.dubbo.provider.util.BitMapShardingUtil;
import com.vipgp.tinyurl.dubbo.provider.util.CommonUtil;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/7 0:28
 */
@Slf4j
@Component
@ConditionalOnProperty(value = Constant.REDIS_SHARD_MODE_KEY,havingValue = Constant.REDIS_SHARD_MODE_REDIS_CLUSTER)
public class RedisClusterManagerImpl extends AbstractRedisManager  {

    @Autowired
    private JedisCluster jedis;

    @Resource(name="addtinyurlscript")
    private ResourceScriptSource addTinyUrlScriptSource;

    @Resource(name="rollbacktinyurlscript")
    private ResourceScriptSource rollbackTinyUrlScriptSource;

    @Resource(name="unlockscript")
    private ResourceScriptSource unlockScriptSource;

    /**
     * delay ms insert tinyurl to db, if 0, then insert to db instant
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.delay:1000}",autoRefreshed = true)
    private Integer syncDelayMillis;
    /**
     * delay transaction counts
     */
    @NacosValue(value = "${tiny.url.group.commit.sync.no.delay.count:5000}",autoRefreshed = true)
    private Integer syncDelayCount;

    @NacosValue(value = "${tiny.url.db.commit.sync}",autoRefreshed = true)
    private boolean dbCommitSync;

    /**
     * calc slot base on id, then the slot will be sequential
     */
    private boolean isSequential=true;

    @Override
    public String set(String key, String value) {
        String result = null;

        try {
            result = jedis.set(key,value);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }

        return result;

    }

    @Override
    public String set(String key, String value, long expiredSecond) {
        String result = null;

        try {
            // EX是秒，PX是毫秒
            SetParams params=new SetParams().ex((int)expiredSecond);
            result = jedis.set(key,value,params);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }

        return result;

    }

    @Override
    public boolean setnx(String key, String value, long expiredSecond) {
        try {
            // NX是不存在时才set， XX是存在时才set， EX是秒，PX是毫秒
            SetParams params=new SetParams().ex((int)expiredSecond).nx();
            // success return OK, fail return null
            String  result = jedis.set(key,value,params);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }

        return false;

    }

    @Override
    public boolean setxx(String key, String value, long expiredSecond) {
        try {
            // NX是不存在时才set， XX是存在时才set， EX是秒，PX是毫秒
            SetParams params=new SetParams().ex((int)expiredSecond).xx();
            // success return OK, fail return null
            String  result = jedis.set(key,value,params);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
        }

        return false;

    }


    @Override
    public boolean setbit(String key, long offset, boolean bitValue) {
        boolean result = false;

        try {
            result = jedis.setbit(key,offset,bitValue);
        } catch (Exception e) {
            log.error("setbit key:{} offset:{} error",key,offset,e);
        }

        return result;

    }

    @Override
    public String get(String key) {
        String result = null;

        try {
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("get key:{} error",key,e);
        }

        return result;
    }


    @Override
    public boolean getbit(String key, long offset) {
        boolean result = false;

        try {
            result = jedis.getbit(key,offset);
        } catch (Exception e) {
            log.error("getbit key:{} offset:{} error",key,offset,e);
        }

        return result;
    }

    @Override
    public Object runLua(String script, List<String> keys, List<String> args) {
         return jedis.eval(script, keys, args);
    }

    @Override
    public boolean addTinyUrlToCache(Long id, long xid, String baseUrlKey, String aliasCodeEncode, String rawUrl,
            long newlyTinyUrlKeyExpiredSecond) {
        try {
            // keys
            List<String> keys = getLuaKeys(id, xid, baseUrlKey, aliasCodeEncode, cacheUtil.getWorkerId());

            // values
            long index = BitMapShardingUtil.calcIndex(id);
            // to list
            List<String> args = new ArrayList<>(6);
            args.add(String.valueOf(index));
            args.add(rawUrl);
            args.add(String.valueOf(xid));
            args.add(String.valueOf(newlyTinyUrlKeyExpiredSecond));
            args.add(String.valueOf(newlyTinyUrlKeyExpiredSecond));
            args.add(String.valueOf(System.currentTimeMillis()));

            log.info("run add lua begin");
            // script
            String script = addTinyUrlScriptSource.getScriptAsString();
            runLua(script, keys, args);
            log.info("run add lua end");

            return true;
        } catch (IOException ex) {
            log.error("script load exception", ex);
            throw new BaseAppException(ErrorCode.SCRIPT_LOAD_EXCEPTION);
        }

    }

    @Override
    public boolean rollbackTinyUrlFromCache(Long id, long xid, String baseUrlKey, String aliasCodeEncode, String workerId, long messageCreateTime) {
        try{
            // keys
            List<String> keys = getLuaKeys(id, xid, baseUrlKey, aliasCodeEncode,workerId);

            // values
            long index = BitMapShardingUtil.calcIndex(id);
            // to list
            List<String> args = new ArrayList<>(1);
            args.add(String.valueOf(index));
            args.add(String.valueOf(messageCreateTime));

            log.info("run rollback lua begin");
            // script
            String script = rollbackTinyUrlScriptSource.getScriptAsString();
            runLua(script, keys, args);
            log.info("run rollback lua end");

            // rollback already
            set(CommonUtil.getRollbackKey(workerId,xid,aliasCodeEncode),aliasCodeEncode,rollbackConsumeKeyExpired);

            return true;
        }catch (Exception ex){
            log.error("rollback script run exception", ex);
            return false;
        }
    }

    private List<String> getLuaKeys(Long id, long xid, String baseUrlKey, String aliasCodeEncode, String workerId){
        // should have the same prefix {prefix}. for all the keys to make sure the lua run on one slot in redis cluster env
        String prefix = getKeyPrefix(baseUrlKey, aliasCodeEncode);
        // keys
        String bitKey = BitMapShardingUtil.getBitKey(id, baseUrlKey);
        String tinyUrlKey = CommonUtil.getTinyurlKey(baseUrlKey, aliasCodeEncode);
        String txnLogKey = CommonUtil.getTxnLogKey(baseUrlKey, aliasCodeEncode, workerId, xid);
        String txnLogEndKey = CommonUtil.getTxnLogEndKey(baseUrlKey, aliasCodeEncode, workerId, xid);
        String tinyUrlUpdateTimeKey = CommonUtil.getTinyurlUpdateTimeKey(baseUrlKey, aliasCodeEncode);

        // to list
        List<String> keys = new ArrayList<>(5);
        keys.add(prefix + bitKey);
        keys.add(prefix + tinyUrlKey);
        keys.add(prefix + txnLogKey);
        keys.add(prefix + txnLogEndKey);
        keys.add(prefix + tinyUrlUpdateTimeKey);

        return keys;
    }

    @Override
    public String setRawUrl(String tinyUrlKey, String value, long expiredSecond) {
        if(whetherLuaRun()) {
            // see #addTinyUrlToCache
            // baseUrlKey+"|"+code;
            String[] array=tinyUrlKey.split("|");
            String prefix =getKeyPrefix(array[0],array[1]);
            String fullKey = prefix + tinyUrlKey;
            return set(fullKey, value, expiredSecond);
        }else {
            return set(tinyUrlKey, value, expiredSecond);
        }
    }

    @Override
    public String queryRawUrl(String tinyUrlKey) {
        if(whetherLuaRun()) {
            // see #addTinyUrlToCache
            // baseUrlKey+"|"+code;
            String[] array=tinyUrlKey.split("\\|");
            String prefix =getKeyPrefix(array[0],array[1]);
            String fullKey = prefix + tinyUrlKey;
            return get(fullKey);
        }else {
            return get(tinyUrlKey);
        }
    }


    @Override
    public boolean getbit(String bitKey, long offset, String baseUrlKey, String aliasCode) {
        if(whetherLuaRun()) {
            String prefix = getKeyPrefix(baseUrlKey, aliasCode);
            String fullKey = prefix + bitKey;
            return getbit(fullKey, offset);
        }else {
            return getbit(bitKey,offset);
        }
    }

    @Override
    public String queryXid(String key, String baseUrlKey, String aliasCode) {
        if(whetherLuaRun()) {
            String prefix =getKeyPrefix(baseUrlKey, aliasCode);
            String fullyKey = prefix + key;
            return get(fullyKey);
        }else {
            return get(key);
        }
    }

    private String getKeyPrefix(String baseUrlKey, String aliasCode){
        if(isSequential) {
            long id = Base62Util.decode(aliasCode);
            // then the bitmap will be in the same slot, it can make full use of the memory
            long slot = BitMapShardingUtil.calcSlot(id);

            return "{" + String.valueOf(slot)+ "}.";
        }else {
            return "{" + CommonUtil.getTinyurlKey(baseUrlKey, aliasCode) + "}.";
        }
    }


    @Override
    public boolean setbit(String bitKey, long offset, boolean bitValue, String baseUrlKey, String aliasCode) {
        if(whetherLuaRun()) {
            String prefix = getKeyPrefix(baseUrlKey, aliasCode);
            String fullKey = prefix + bitKey;
            return setbit(fullKey, offset, bitValue);
        }else {
            return setbit(bitKey, offset, bitValue);
        }
    }

    private boolean whetherLuaRun(){
        // if there is group commit in redis cluster env, then it will run lua
        return  syncDelayCount > 0 && syncDelayMillis > 0 && !dbCommitSync;
    }

    @Override
    public boolean unlock(String key, String value) {
        try {
            String script = unlockScriptSource.getScriptAsString();
            String result = (String) runLua(script, Arrays.asList(key), Arrays.asList(value));
            return "OK".equals(result);
        } catch (Exception ex) {
            log.error("unlock exception key {} value {}", key, value, ex);
            return false;
        }
    }
}
