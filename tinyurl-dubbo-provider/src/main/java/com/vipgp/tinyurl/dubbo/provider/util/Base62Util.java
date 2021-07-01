package com.vipgp.tinyurl.dubbo.provider.util;


import org.apache.commons.lang3.StringUtils;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/26 0:06
 */
public class Base62Util {

    /**
     * 初始化 62 进制数据，索引位置代表字符的数值，比如 A代表10，z代表61等
     */
    private static String chars = "012349ABCDEFGHIJKL5678MNOTUVWXYZabcdefghijklmnoPQRSpqrstuvwxyz";
    private static int scale = 62;

    /**
     * 将数字转为62进制
     *
     * @param num Long 型数字
     * @param length 转换后的字符串长度，不足则左侧补0
     * @return 62进制字符串
     */
    public static String encode(long num, int length) {
        StringBuilder sb = new StringBuilder();
        int remainder = 0;

        while (num > scale - 1) {
            /**
             * 对 scale 进行求余，然后将余数追加至 sb 中，由于是从末位开始追加的，因此最后需要反转（reverse）字符串
             */
            remainder = Long.valueOf(num % scale).intValue();
            sb.append(chars.charAt(remainder));

            num = num / scale;
        }

        sb.append(chars.charAt(Long.valueOf(num).intValue()));
        String value = sb.reverse().toString();
        // 用0补位
        return StringUtils.leftPad(value, length, '0');
    }

    /**
     * 62进制字符串转为数字
     *
     * @param str 编码后的62进制字符串
     * @return 解码后的 10 进制字符串
     */
    public static long decode(String str) {
        long num = 0;
        int index = 0;
        for (int i = 0; i < str.length(); i++) {
            /**
             * 查找字符的索引位置
             */
            index = chars.indexOf(str.charAt(i));
            /**
             * 索引位置代表字符的数值
             */
            num += (long) (index * (Math.pow(scale, str.length() - i - 1)));
        }

        return num;
    }

    /**
     * 验证用户输入的code是否合法
     * @param base62Code
     * @return
     */
    public static boolean validateCode(String base62Code){
        if(StringUtils.isEmpty(base62Code)){
            return false;
        }
        for(int i=0;i<base62Code.length();i++){
            boolean isExist= chars.contains(String.valueOf(base62Code.charAt(i)));
            if(!isExist){
                return false;
            }
        }

        return true;
    }

	public static void main(String[] args) {

		System.out.println(Base62Util.encode(1025401L,6));

		System.out.println(Base62Util.decode(Base62Util.encode(1025401L,6)));
	}
}
