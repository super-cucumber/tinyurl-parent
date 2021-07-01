package com.vipgp.tinyurl.dubbo.provider.persistence;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.mq.message.LogCommitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Checksum;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/27 1:25
 */
@Slf4j
@Component
public class FileTxnLog implements  TxnLog, Closeable {

    @NacosValue(value = "${txn.log.dir}",autoRefreshed = true)
    private String txnDirPath;

    @Autowired
    private FilePadding filePadding;

    private AtomicLong xidSource = new AtomicLong(0);

    private File txnDir;
    private volatile File txnFileWrite=null;
    private volatile BufferedOutputStream bos=null;
    private volatile FileOutputStream fos=null;
    private DataOutputStream dos=null;

    private volatile AtomicLong adder=new AtomicLong(0);
    /**
     * wait to flush to db
     */
    private List<String> txnsToFlush= Collections.synchronizedList(new ArrayList<>());

    final ReentrantLock lock = new ReentrantLock();

    @PostConstruct
    private void init(){
        this.txnDir=new File(txnDirPath);
    }

    @Override
    public void prepare(List<TxnLogModel> models, long wholeSize) throws IOException {
        lock.lock();
        try {
            log.info("batch flush prepare log start");
            // file roll
            onlyRollLog(wholeSize);
            long xid = getCurrentXid() <= 0 ? getNextXid() : getCurrentXid();
            // new log file
            if (txnFileWrite == null) {
                createNewFile(xid);
            }

            long start = System.nanoTime();
            // append file
            for (TxnLogModel model : models) {
                model.setXid(xid);
                int offset = adder.intValue();
                // the next beginning
                adder.set(offset + model.getSize());
                LogCommitEvent event = new LogCommitEvent(txnFileWrite.getName(), offset);
                model.setLogCommitEvent(event);
                log.info("id {}, xid {}, event {}", model.getId(), model.getXid(), event.toString());

                /**
                 * Step 1: write to file
                 * Log Block:
                 * data length - 4 bytes
                 * txn status - 1 byte
                 * txn checksum - 8 bytes
                 * txn xid - 8 bytes
                 * txn entry - x bytes
                 * EOR - 1 byte
                 */
                // write
                Util.writeInt(dos, model.getData().length, Const.TAG_DATA_LENGTH);
                Util.writeByte(dos, Const.STATUS_PREPARE, Const.TAG_TXN_STATUS);
                Util.writeLong(dos, model.getCrcValue(), Const.TAG_CRC_VALUE);
                // xid
                Util.writeLong(dos, model.getXid(), Const.TAG_TXN_XID);
                Util.write(bos, model.getData(), Const.TAG_TXN_ENTRY);
                // EOR - 'EOR'
                Util.writeByte(dos, Const.EOR, Const.TAG_END_OF_REEL);

                //prefetch next id
                xid = getNextXid();
            }

            dos.flush();
            bos.flush();
            fos.flush();
            // write to disk
            fos.getFD().sync();

            long end = System.nanoTime();

            log.info("batch flush prepare log, flush log number {}, cost {}ns {}ms", models.size(), end - start, (end - start) / 1000000);

        } catch (Exception ex) {
            log.error("batch prepare log exception", ex);
            // reset, keep adder correct, then the write pos will be updated correctly
            txnFileWrite = null;
            adder.set(0L);
            throw ex;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long populateModels(List<TxnLogModel> models) {
        long wholeSize = 0;
        for (TxnLogModel model : models) {
            long start = System.nanoTime();
            byte[] data = ProtostuffUtils.serialize(model);
            long end = System.nanoTime();
            log.info("serialize model, id {}, cost {}ns {}ms", model.getId(), end - start, (end - start)/1000000);
            model.setData(data);

            // checksum
            Checksum crc = Util.makeChecksumAlgorithm();
            crc.update(data, 0, data.length);
            model.setCrcValue(crc.getValue());

            // data size
            int size = Util.calcTxnLogBlockSize(data.length);
            model.setSize(size);
            wholeSize = wholeSize + size;
        }

        return wholeSize;
    }

    private void createNewFile(long xid) throws IOException {
        long start = System.nanoTime();
        log.info("switch log file");
        String fileName = Util.makeLogName(xid);
        txnFileWrite = new File(txnDir, fileName);
        fos = new FileOutputStream(txnFileWrite);
        bos = new BufferedOutputStream(fos);
        dos = new DataOutputStream(bos);
        log.info("padding file");
        filePadding.padFile(fos.getChannel());
        txnsToFlush.add(fileName);
        long end = System.nanoTime();
        log.info("padding log file, cost {}ns {}ms", end - start, (end - start) / 1000000);

    }

    @Override
    public long getLastLoggedXid() throws IOException{
        File[] files = getTxnFiles(txnDir.listFiles(), 0);
        long maxLog = files.length > 0 ? Util.getXidFromName(files[files.length - 1].getName(), Const.LOG_FILE_PREFIX) : 0;
        long xid = maxLog;
        TxnIterator itr = null;
        try {
            itr = new FileTxnIterator(txnDir, maxLog);
            while (true) {
                if(itr.getTxn()!=null) {
                    xid = itr.getTxn().getXid();
                }
                if (!itr.next()) {
                    break;
                }
            }
        } catch (IOException ex) {
            log.warn("Unexpected exception", ex);
            throw ex;
        } finally {
            close(itr);
        }
        return xid;
    }

    @Override
    public List<TxnLogModel> extractTxnLogsAfter(long lastSnapShotXid) {
        List<TxnLogModel> target = new ArrayList<>();
        TxnIterator itr = null;
        try {
            itr = new FileTxnIterator(txnDir, lastSnapShotXid);
            while (true) {
                TxnLogModel model = itr.getTxn();
                if (model!=null && model.getXid() > lastSnapShotXid) {
                    target.add(model);
                }
                if (!itr.next()) {
                    break;
                }
            }
        } catch (IOException ex) {
            log.warn("Unexpected exception", ex);
        } finally {
            close(itr);
        }
        return target;
    }

    @Override
    public List<TxnLogModel> extractTxnLogsAfter(String fileName, long lastSnapShotXid) {
        List<TxnLogModel> target = new ArrayList<>();
        TxnIterator itr = null;
        try {
            File logFile = new File(txnDir, fileName);
            itr = new FileTxnIterator(logFile, lastSnapShotXid, true);
            while (true) {
                if (!itr.next()) {
                    break;
                }
                TxnLogModel model = itr.getTxn();
                if (model.getXid() > lastSnapShotXid) {
                    target.add(model);
                }
            }
        } catch (IOException ex) {
            log.error("extract txn logs unexpected exception", ex);
        } finally {
            close(itr);
        }
        return target;
    }

    private void close(TxnIterator itr) {
        if (itr != null) {
            try {
                itr.close();
            } catch (IOException ioe) {
                log.warn("Error closing file iterator", ioe);
            }
        }
    }


    public static File[] getTxnFiles(File[] txnDirList, long snapshotXid){
        List<File> files = Util.sortDataDir(txnDirList, Const.LOG_FILE_PREFIX, true);
        long logXid = 0;
        for (File f : files) {
            long fxid = Util.getXidFromName(f.getName(), Const.LOG_FILE_PREFIX);
            if (fxid > snapshotXid) {
                continue;
            }
            if (fxid > logXid) {
                logXid = fxid;
            }
        }
        List<File> v=new ArrayList<File>(5);
        for (File f : files) {
            long fxid = Util.getXidFromName(f.getName(), Const.LOG_FILE_PREFIX);
            if (fxid < logXid) {
                continue;
            }
            v.add(f);
        }
        return v.toArray(new File[0]);
    }




    private void onlyRollLog(long batchSize) throws IOException {
        long expectedSize = adder.longValue() + batchSize;
        if (expectedSize > filePadding.getPreAllocSize()) {
            log.info("expectedSize {} preAllocSize {} reset roll file, txnFileWrite {}",
                    expectedSize, filePadding.getPreAllocSize(), txnFileWrite == null ? "empty" : txnFileWrite.getName());
            txnFileWrite = null;
            if (dos != null) {
                dos.close();
            }
            if (bos != null) {
                bos.close();
            }
            // create new file, offset is from 0
            adder.set(0L);
        }
    }


    @Override
    public void close() throws IOException {
        if(bos!=null){
            bos.close();
        }

        if(fos!=null){
            fos.close();
        }
    }

    @Override
    public void archive(File backupDir, String suffix){
        Util.moveTo(backupDir,txnDir.listFiles(),Const.LOG_FILE_PREFIX,suffix);
    }


    @Override
    public long getNextXid(){
        return xidSource.incrementAndGet();
    }

    public long getCurrentXid(){
        return xidSource.longValue();
    }

    @Override
    public void setBaseXid(long maxLoggedXid){
        xidSource = new AtomicLong(maxLoggedXid);
        // prefetch next xid
        xidSource.getAndIncrement();
    }

    @Override
    public List<String> getTxnsToFlush(){
        return txnsToFlush;
    }

    @Override
    public void removeTxnFromFlushList(int index) {
        txnsToFlush.remove(index);
    }
}
