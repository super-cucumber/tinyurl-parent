package com.vipgp.tinyurl.dubbo.provider.system;

import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/29 11:47
 */
@Component
public class HealthMonitor {

    @Autowired
    CacheUtil cacheUtil;

    public void unhealth(String key, String error){
        cacheUtil.put(Constants.CACHE_NAME_HEALTH, key, error);
    }

    public void getwell(String key){
        cacheUtil.put(Constants.CACHE_NAME_HEALTH, key, "");
    }

    public String checkHealth() {
        String error = "";
        String rocketmq = cacheUtil.get(Constants.CACHE_NAME_HEALTH, Constants.CACHE_KEY_ROCKETMQ);
        if (!StringUtils.isEmpty(rocketmq)) {
            error = error + rocketmq + ";";
        }
        String clogcommit = cacheUtil.get(Constants.CACHE_NAME_HEALTH, Constants.CACHE_KEY_CLOGCOMMIT);
        if (!StringUtils.isEmpty(clogcommit)) {
            error = error + clogcommit + ";";
        }

        String disruptor = cacheUtil.get(Constants.CACHE_NAME_HEALTH, Constants.CACHE_KEY_DISRUPTOR);
        if (!StringUtils.isEmpty(disruptor)) {
            error = error + disruptor+ ";";
        }

        String recover = cacheUtil.get(Constants.CACHE_NAME_HEALTH, Constants.CACHE_KEY_RECOVER);
        if (!StringUtils.isEmpty(recover)) {
            error = error + recover+ ";";
        }

        return error;
    }

    public boolean isRecoveredAlready(){
        String recover = cacheUtil.get(Constants.CACHE_NAME_HEALTH, Constants.CACHE_KEY_RECOVER);
        if (StringUtils.isEmpty(recover)) {
            return true;
        }

        return false;
    }
}
