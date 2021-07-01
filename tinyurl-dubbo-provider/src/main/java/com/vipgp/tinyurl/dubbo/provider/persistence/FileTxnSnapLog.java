package com.vipgp.tinyurl.dubbo.provider.persistence;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import com.vipgp.tinyurl.dubbo.provider.system.HealthMonitor;
import com.vipgp.tinyurl.dubbo.provider.tasks.CLogCommitProcessor;
import com.vipgp.tinyurl.dubbo.provider.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/27 13:51
 */
@Slf4j
@Component
public class FileTxnSnapLog {

    @NacosValue(value = "${archive.log.dir}",autoRefreshed = true)
    private String archiveDirPath;


    @Autowired
    TxnLog txnLog;
    @Autowired
    SnapShot snapShot;

    @Autowired
    CLogCommitProcessor logCommitProcessor;
    @Autowired
    HealthMonitor healthMonitor;


    public void prepare(List<TxnLogModel> models) {

        if (CollectionUtils.isEmpty(models)) {
            return;
        }

        long start=System.nanoTime();
        // serialize
        long wholeSize= txnLog.populateModels(models);
        long end=System.nanoTime();
        log.info("populate {} models, cost {}ns {}ms", models.size(), end-start, (end-start)/1000000);

        try {
            start=System.nanoTime();
            txnLog.prepare(models, wholeSize);
            end=System.nanoTime();
            log.info("flush {} models to log file, cost {}ns {}ms", models.size(), end-start, (end-start)/1000000);
        } catch (Exception ex) {
            log.error("txn prepare log exception",ex);
            // log prepare fail
            for (TxnLogModel model : models) {
                model.setXid(-1L);
            }
        }
    }


    /**
     * should be success
     * @param event
     * @return
     */
    public boolean commit(LogCommitEvent event) {
        int errorCount = 0;
        while (true) {
            try {
                logCommitProcessor.commit(event);
                if (errorCount > 0) {
                    healthMonitor.getwell(Constants.CACHE_KEY_DISRUPTOR);
                }
                break;
            } catch (Exception ex) {
                log.error("commit exception, log commit event {}", event, ex);
                healthMonitor.unhealth(Constants.CACHE_KEY_DISRUPTOR, "C log push to disruptor exception " + ex);
                errorCount++;
                try {
                    Thread.sleep(1000);
                } catch (Exception inner) {
                    log.error("sleep exception", inner);
                }
            }
        }

        return true;
    }


    public void takeSnapShot(Long lastXid){
        try{
            SnapShotModel model=new SnapShotModel();
            model.setLastXid(lastXid);
            snapShot.serialize(model);
        }catch (Exception ex){
            log.error("take snap shot exception",ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    public long getLastXidInSnapShot(){
        return snapShot.getLastProcessedXid();
    }

    public long getLastXidInTxn() throws IOException{
        return txnLog.getLastLoggedXid();
    }

    public List<TxnLogModel> extractTxnLogsAfter(long lastSnapShotXid){
        return txnLog.extractTxnLogsAfter(lastSnapShotXid);
    }

    public boolean archive() {
        log.info("archive begin");
        try {
            /**
             * Step 0: 新建文件夹
             */
            File dir = new File(archiveDirPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            /**
             * Step 1: 备份时间，也是备份的文件夹名称
             */
            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String time = formatter.format(localDateTime);

            /**
             * Step 1: 备份
             */
            txnLog.archive(dir, time);
            snapShot.archive(dir, time);
        } catch (Exception ex) {
            log.error("archive exception archiveDirPath {}", archiveDirPath, ex);
            return false;
        }

        log.info("archive end");
        return true;
    }

    public void setBaseXid(long maxLoggedXid){
        txnLog.setBaseXid(maxLoggedXid);
    }

    public List<String> getTxnsToFlush(){
        return txnLog.getTxnsToFlush();
    }

    public void removeTxnFromFlushList(int index){
        txnLog.removeTxnFromFlushList(index);
    }

    public List<TxnLogModel> extractTxnLogsAfter(String fileName, long lastSnapShotXid){
        return txnLog.extractTxnLogsAfter(fileName, lastSnapShotXid);
    }
}
