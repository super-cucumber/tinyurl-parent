package com.vipgp.tinyurl.dubbo.provider.util;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/9 12:05
 */
@Slf4j
public class CommonUtil {

    /**
     * 拼接短链接
     * @param base62Code
     * @return
     */
    public static String appendTinyUrl(String baseUrl, String base62Code){
        StringBuilder sb=new StringBuilder();
        sb.append(baseUrl);
        sb.append("/");
        sb.append(base62Code);

        return sb.toString();
    }

    /**
     * generate tiny url key
     * @param baseUrlKey
     * @param code
     * @return
     */
    public static String getTinyurlKey(String baseUrlKey, String code){
        return baseUrlKey+"|"+code;
    }

    public static String getTinyurlUpdateTimeKey(String baseUrlKey, String code) {
        return baseUrlKey + "|" + code + "|" + "updated";
    }

    public static String[] splitTinyUrlKey(String tinyUrlKey){
        return tinyUrlKey.split("|");
    }

    /**
     * generate txn log xid key
     * @param baseUrlKey
     * @param code
     * @param workerId
     * @param xid
     * @return
     */
    public static String getTxnLogKey(String baseUrlKey, String code, String workerId, long xid) {
        return baseUrlKey + "|" + code + "|" + workerId + "|" + xid;
    }

    public static String getTxnLogEndKey(String baseUrlKey, String code, String workerId, long xid) {
        return baseUrlKey + "|" + code + "|" + workerId + "|" + xid + "|" + "end";
    }

    public static String getRollbackKey(String workerId, long xid, String aliasCode) {
        return "rollback" + "_" + workerId + "_" + xid + "_" + aliasCode;
    }

    /**
     * get raw url key
     * @param baseUrlKey
     * @param rawUrl
     * @return
     */
    public static String getRawurlKey(String baseUrlKey, String rawUrl){
        return baseUrlKey+"|"+rawUrl;
    }

    public static String getLockKey(String baseUrlKey, String aliasCode){
        return baseUrlKey+"|"+aliasCode;
    }

    public static String getIp() {
        String ip;
        try {
            List<String> ipList = getHostAddress(null);
            // default the first
            ip = (!ipList.isEmpty()) ? ipList.get(0) : "";
        } catch (Exception ex) {
            ip = "";
            log.warn("Utils get IP warn", ex);
        }
        return ip;
    }

    /**
     * 获取已激活网卡的IP地址
     *
     * @param interfaceName 可指定网卡名称,null则获取全部
     * @return List<String>
     */
    private static List<String> getHostAddress(String interfaceName) throws SocketException {
        List<String> ipList = new ArrayList<String>(5);
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            Enumeration<InetAddress> allAddress = ni.getInetAddresses();
            while (allAddress.hasMoreElements()) {
                InetAddress address = allAddress.nextElement();
                if (address.isLoopbackAddress()) {
                    // skip the loopback addr
                    continue;
                }
                if (address instanceof Inet6Address) {
                    // skip the IPv6 addr
                    continue;
                }
                String hostAddress = address.getHostAddress();
                if (null == interfaceName) {
                    ipList.add(hostAddress);
                } else if (interfaceName.equals(ni.getDisplayName())) {
                    ipList.add(hostAddress);
                }
            }
        }
        return ipList;
    }


    public static int random6(){
        ThreadLocalRandom random=ThreadLocalRandom.current();
        return random.nextInt(100000,999999);
    }

    public static void sleep(long millis){
        try{
            Thread.sleep(millis);
        }catch (Exception ex){
            log.error("sleep exception", ex);
        }
    }
}
