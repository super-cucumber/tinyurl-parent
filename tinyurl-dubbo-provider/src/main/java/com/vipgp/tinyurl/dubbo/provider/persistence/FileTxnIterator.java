package com.vipgp.tinyurl.dubbo.provider.persistence;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Checksum;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/21 12:11
 */
@Slf4j
public class FileTxnIterator implements TxnIterator{

    static final String CRC_ERROR="CRC check failed";
    File logDir;
    long xid;
    TxnLogModel model;
    File logFile;
    DataInputStream dis=null;
    ArrayList<File> storedFiles;
    boolean focusThisFile=false;

    public FileTxnIterator(File logDir, long xid) throws IOException {
        this.logDir = logDir;
        this.xid = xid;
        init();
        if (model != null) {
            while (model.getXid() < xid) {
                if (!next()) {
                    break;
                }
            }
        }
    }

    public FileTxnIterator(File logFile, long xid, boolean focusThisFile) throws IOException {
        this.logFile=logFile;
        this.xid=xid;
        this.focusThisFile=focusThisFile;
        createInputStream(logFile);
    }

    private void init() throws IOException{
        storedFiles=new ArrayList<>();
        // descending
        List<File> files=Util.sortDataDir(FileTxnLog.getTxnFiles(logDir.listFiles(),0), Const.LOG_FILE_PREFIX,false);
        for (File file : files) {
            if (Util.getXidFromName(file.getName(), Const.LOG_FILE_PREFIX) >= xid){
                storedFiles.add(file);
            }
            // add the last logfile that is less than the xid, e.g. max file xid is 45, but lastsnapshotid is 49
            // if there is no else block, then will be no file added, it is invalid, as the xid in the file name is the smallest xid
            else if (Util.getXidFromName(file.getName(), Const.LOG_FILE_PREFIX) < xid) {
                storedFiles.add(file);
                break;
            }
        }

        goToNextLog();
        next();
    }

    private boolean goToNextLog() throws IOException{
        if(storedFiles.size()>0){
            //loop from tail -> header
            this.logFile=storedFiles.remove(storedFiles.size()-1);
            createInputStream(this.logFile);
            return true;
        }

        return false;
    }

    private void createInputStream(File logFile) throws IOException {
        if (dis == null) {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(logFile)));
        }
    }

    /**
     * Log Block:
     * data length - 4 bytes
     * txn status - 1 byte
     * txn checksum - 8 bytes
     * txn entry - x bytes
     * EOR - 1 byte
     * @return
     * @throws IOException
     */
    @Override
    public boolean next() throws IOException {
        if (dis == null) {
            return false;
        }

        try {
            int len = Util.readInt(dis, Const.TAG_DATA_LENGTH);
            int status = Util.readByte(dis, Const.TAG_TXN_STATUS);
            long crcValue = Util.readLong(dis, Const.TAG_CRC_VALUE);
            // xid
            long xid = Util.readLong(dis,Const.TAG_TXN_XID);
            byte[] bytes = new byte[len];
            bytes = Util.readTxnBytes(dis, bytes);
            if (bytes == null || bytes.length == 0) {
                throw new EOFException("Failed to read " + logFile+", xid="+xid+",status="+status);
            }
            //validate crc, check corrupted record
            Checksum crc =Util.makeChecksumAlgorithm();
            crc.update(bytes, 0, bytes.length);
            if (crcValue != crc.getValue()) {
                throw new IOException(CRC_ERROR);
            }
            // valid data
            model = ProtostuffUtils.deserialize(bytes, TxnLogModel.class);
            model.setStatus(status);
            model.setXid(xid);
        } catch (EOFException ex) {
            //log.info("EOF exception " + ex);
            dis.close();
            dis = null;
            if (focusThisFile || !goToNextLog()) {
                return false;
            }

            return next();
        } catch (Exception ex) {
            dis.close();
            log.error("iterator exception",ex);
            throw ex;
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        if(dis!=null){
            dis.close();
        }
    }

    @Override
    public TxnLogModel getTxn() {
        return model;
    }
}