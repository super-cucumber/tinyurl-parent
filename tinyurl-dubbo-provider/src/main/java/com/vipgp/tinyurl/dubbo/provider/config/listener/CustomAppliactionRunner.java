package com.vipgp.tinyurl.dubbo.provider.config.listener;

import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RecoverManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/7 8:33
 */
@Component
@Slf4j
public class CustomAppliactionRunner implements ApplicationRunner {

    @Autowired
    LookupManager lookupManager;

    @Autowired
    RecoverManager recoverManager;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        /**
         * Step 1: lookup cache init
         */
        log.info("run after app bootstarp, argus:{}", applicationArguments.getSourceArgs());
        try {
            lookupManager.refreshToCache();
            log.info("lookup cache init success");
        }catch (Exception ex){
            log.error("lookup cache init failed",ex);
            throw ex;
        }

        /**
         * Step 2: crash safe base on txn and snapshot
         */
        log.info("-------------- begin recover -----------------");
        try {
            int recordsCount = recoverManager.recover();
            log.info("-------------- end recover, {} records recovered --------------------", recordsCount);
        }catch (Exception ex){
            log.error("recover failed", ex);
            throw ex;
        }

    }
}
