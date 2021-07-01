package com.vipgp.tinyurl.dubbo.provider.config.listener;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/7 8:30
 */
@Component
@Slf4j
public class CustomApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private AbstractApplicationContext ctx;

    @NacosValue(value = "${logging.file.path}",autoRefreshed = true)
    private String loggingFilePath;
    @NacosValue(value = "${txn.log.dir}",autoRefreshed = true)
    private String txnLogDir;
    @NacosValue(value = "${snapshot.log.dir}",autoRefreshed = true)
    private String snapshotLogDir;
    @NacosValue(value = "${pre.alloc.size}",autoRefreshed = true)
    private String preallocSize;
    @NacosValue(value = "${txn.log.commit.delay}",autoRefreshed = true)
    private String txnLogCommitDelay;
    @NacosValue(value = "${txn.log.commit.no.delay.count}",autoRefreshed = true)
    private String txnLogCommitNoDelayCount;
    @NacosValue(value = "${tiny.url.group.commit.sync.no.delay.count}",autoRefreshed = true)
    private String tinyUrlGroupCommitSyncNoDelayCount;
    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.parties}",autoRefreshed = true)
    private Integer parties;
    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.await.time}",autoRefreshed = true)
    private Integer awaitTime;
    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.segments}",autoRefreshed = true)
    private Integer segmentCount;
    @NacosValue(value = "${txn.prepare.log.cyclic.barrier.sleep.time}",autoRefreshed = true)
    private Integer sleepTime;
    @NacosValue(value = "${txn.prepare.log.completable.future.threads}", autoRefreshed = true)
    private Integer completableFutureThreads;
    @NacosValue(value = "${txn.prepare.log.timed.barrier.threads}", autoRefreshed = true)
    private Integer timedBarrierThreads;
    @NacosValue(value = "${tiny.url.db.commit.sync}",autoRefreshed = true)
    private boolean dbCommitSync;
    @NacosValue(value = "${txn.prepare.log.delay.commit}",autoRefreshed = true)
    private boolean logDelayCommit;
    @NacosValue(value = "${tiny.url.group.commit.sync.delay}",autoRefreshed = true)
    private String tinyUrlGroupCommitSyncDelay;


    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {

        log.info("=======================================");
        log.info("         loggingFilePath - {}                           ", loggingFilePath);
        log.info("         txnLogDir - {}                                 ", txnLogDir);
        log.info("         snapshotLogDir - {}                            ", snapshotLogDir);
        log.info("         preallocSize - {}                              ", preallocSize);
        log.info("         txnLogCommitDelay - {}                         ", txnLogCommitDelay);
        log.info("         txnLogCommitNoDelayCount - {}                  ", txnLogCommitNoDelayCount);
        log.info("         tinyUrlGroupCommitSyncNoDelayCount - {}        ", tinyUrlGroupCommitSyncNoDelayCount);
        log.info("         cyclic barrier parties - {}                    ", parties);
        log.info("         cyclic barrier awaitTime - {}                  ", awaitTime);
        log.info("         cyclic barrier segmentCount - {}               ", segmentCount);
        log.info("         cyclic barrier sleepTime - {}                  ", sleepTime);
        log.info("         completable future threads - {}                ", completableFutureThreads);
        log.info("         timed barrier threads - {}                     ", timedBarrierThreads);
        log.info("         db commit sync - {}                            ", dbCommitSync);
        log.info("         log delay commit - {}                          ", logDelayCommit);
        log.info("         db group commit sync delay - {}                ", tinyUrlGroupCommitSyncDelay);
        log.info("=======================================");
        log.info("=                                     =");
        log.info("=         Application Ready           =");
        log.info("=======================================");

        log.info("bean definition count = {}", ctx.getBeanDefinitionCount());

        log.info("Power by super-cucumber\n");
    }
}
