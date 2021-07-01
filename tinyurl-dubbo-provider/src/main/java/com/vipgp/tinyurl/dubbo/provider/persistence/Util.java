package com.vipgp.tinyurl.dubbo.provider.persistence;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/2 15:15
 */
@Slf4j
public class Util {

    public static String makeLogName(long xid) {
        return Const.LOG_FILE_PREFIX + "." + Long.toHexString(xid);
    }

    public static String makeSnapShotName(long xid) {
        return Const.SNAP_FILE_PREFIX + "." + Long.toHexString(xid);
    }

    /**
     * use 4 bytes(int) to keep txn record size
     */
    private static int LOG_BLOCK_DATA_LENGTH = 4;
    /**
     * use 1 byte(char) to keep TAG_END_OF_REEL
     */
    private static int LOG_BLOCK_EOR = 1;
    /**
     * use 1 byte(char) to keep status
     * p - prepare
     * c - commit
     */
    private static int LOG_BLOCK_STATUS = 1;

    /**
     * use 8 bytes(long) to keep checksum
     */
    private static int LOG_BLOCK_CHECKSUM=8;

    /**
     * use 8 bytes(long) to keep xid
     */
    private static int LOG_BLOCK_XID=8;


    public static List<File> sortDataDir(File[] files, String prefix, boolean ascending) {
        if (files == null) {
            return new ArrayList<File>(0);
        }
        List<File> filelist = Arrays.asList(files);
        Collections.sort(filelist, new DataDirFileComparator(prefix, ascending));
        return filelist;
    }

    private static class DataDirFileComparator
            implements Comparator<File>, Serializable {
        private static final long serialVersionUID = -2648639884525140318L;

        private String prefix;
        private boolean ascending;

        public DataDirFileComparator(String prefix, boolean ascending) {
            this.prefix = prefix;
            this.ascending = ascending;
        }

        @Override
        public int compare(File o1, File o2) {
            long z1 = getXidFromName(o1.getName(), prefix);
            long z2 = getXidFromName(o2.getName(), prefix);
            int result = z1 < z2 ? -1 : (z1 > z2 ? 1 : 0);
            return ascending ? result : -result;
        }
    }

    public static long getXidFromName(String name, String prefix) {
        long xid = -1;
        String nameParts[] = name.split("\\.");
        if (nameParts.length == 2 && nameParts[0].equals(prefix)) {
            try {
                xid = Long.parseLong(nameParts[1], 16);
            } catch (NumberFormatException ex) {
                log.error("number format exception name {} prefix {}", name, prefix, ex);
            }
        }
        return xid;
    }

    public static byte[] readTxnBytes(DataInputStream dis, byte[] bytes) throws IOException {
        try {
            // data
            readFully(dis, bytes, Const.TAG_TXN_ENTRY);
            if (bytes.length == 0) {
                //log.info("read fully is empty");
                return bytes;
            }
            // EOR
            int tag = readByte(dis, Const.TAG_END_OF_REEL);
            if (tag != Const.EOR) {
                log.error("reach the end of the file or the last transaction was partial, end tag {}", tag);
                return null;
            }

            return bytes;

        } catch (Exception ex) {
            log.error("read txn bytes exception", ex);
        }

        return null;
    }

    public static void moveTo(File backupDir, File[] files, String prefix, String suffix) {
        String fullName = prefix + suffix;
        File dir = new File(backupDir, fullName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (files != null && files.length > 0) {
            for (File file : files) {
                file.renameTo(new File(dir.getPath() + File.separator + file.getName()));
            }
        }
    }

    /**
     * log block bytes
     *
     * @param length
     * @return
     */
    public static int calcTxnLogBlockSize(int length) {
        return LOG_BLOCK_DATA_LENGTH + LOG_BLOCK_STATUS + LOG_BLOCK_CHECKSUM + LOG_BLOCK_XID + length  + LOG_BLOCK_EOR;
    }

    public static int calcStatusPosition(int beginPos){
        // start from 0
        return beginPos + LOG_BLOCK_DATA_LENGTH;
    }

    public static void writeInt(DataOutputStream dos, int data, String tag) throws IOException {
        dos.writeInt(data);
    }

    public static int readInt(DataInputStream dis,String tag) throws IOException{
        return dis.readInt();
    }

    public static void writeLong(DataOutputStream dos, long data, String tag) throws IOException {
        dos.writeLong(data);
    }

    public static long readLong(DataInputStream dis,String tag) throws IOException{
        return dis.readLong();
    }

    public static void writeByte(DataOutputStream dos, int data, String tag) throws IOException {
        dos.writeByte(data);
    }

    public static int readByte(DataInputStream dis, String tag) throws IOException {
        return (int)dis.readByte();
    }

    public static void write(BufferedOutputStream bos, byte[] data, String tag) throws IOException {
        bos.write(data);
    }

    public static void readFully(DataInputStream bis, byte[] data, String tag) throws IOException {
        bis.readFully(data);
    }

    public static Checksum makeChecksumAlgorithm(){
        return new Adler32();
    }
}
