package com.fgsqw.lanshare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.fgsqw.lanshare.toast.T;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetWorkUtil {


    // 有线网卡
    public static final String ETH_0 = "eth0";
    // wifi网卡
    public static final String WLAN_0 = "wlan0";

    // 获取本机IP地址
    public static String getLocAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {    // 当前使用2G/3G/4G网络直接返回 127.0.0.1
              /*  try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }*/
                return "127.0.0.1";
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {    // 当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return intIP2StringIP(wifiInfo.getIpAddress());
            } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) { // 当前使用的是有线网络
                return getIP(ETH_0);
            }
        }
        return null;
    }

    // 获取本机地址掩码
    public static String getLocMask(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {    // 当前使用2G/3G/4G网络直接返回 255.255.255.0
              /*  try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (InterfaceAddress interfaceAddress : intf.getInterfaceAddresses()) {
                            if (interfaceAddress.getAddress() instanceof Inet4Address) {
                                //获取掩码位数，通过 calcMaskByPrefixLength 转换为字符串
                                return calcMaskByPrefixLength(interfaceAddress.getNetworkPrefixLength());
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                */
                return getMaskMap(24);
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {    // 当前使用无线网络
              /*  DhcpInfo dhcpInfo = ((WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE)).getDhcpInfo();
                return intIP2StringIP(dhcpInfo.netmask);*/
                return getSubnetMask(WLAN_0);
            } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) { // 当前使用的是有线网络
                return getSubnetMask(ETH_0);
            }
        }
        return null;
    }


    /**
     * 获取ip地址
     *
     * @return
     */
    public static String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }


    // 获取连接的wifi子网掩码
    public static String getNetMask(Context context) {
        DhcpInfo dhcpInfo = ((WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE)).getDhcpInfo();
        return intIP2StringIP(dhcpInfo.netmask);
    }


    public static String getIP(String interfaceName) {
        try {
            Enumeration<NetworkInterface> enNetworkInterface = NetworkInterface.getNetworkInterfaces(); //获取本机所有的网络接口
            while (enNetworkInterface.hasMoreElements()) {  //判断 Enumeration 对象中是否还有数据
                NetworkInterface networkInterface = enNetworkInterface.nextElement();   //获取 Enumeration 对象中的下一个数据
                if (!networkInterface.isUp()) { // 判断网口是否在使用
                    continue;
                }
                if (!interfaceName.equals(networkInterface.getDisplayName())) {
                    // 判断是否时我们获取的网口
                    continue;
                }
                Enumeration<InetAddress> enInetAddress = networkInterface.getInetAddresses();   //getInetAddresses 方法返回绑定到该网卡的所有的 IP 地址。
                while (enInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enInetAddress.nextElement();
                    if (inetAddress instanceof Inet4Address) {  //判断是否未ipv4
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    // 获取接口子网掩码
    public static String getSubnetMask(String interfaceName) {
        try {
            //获取本机所有的网络接口
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            //判断 Enumeration 对象中是否还有数据
            while (networkInterfaceEnumeration.hasMoreElements()) {
                //获取 Enumeration 对象中的下一个数据
                NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
                if (!networkInterface.isUp()) {
                    // 判断网口是否在使用
                    continue;
                }
                if (!interfaceName.equals(networkInterface.getDisplayName())) {
                    // 判断是否时我们获取的网口
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress() instanceof Inet4Address) {
                        //仅仅处理ipv4
                        //获取掩码位数，通过 calcMaskByPrefixLength 转换为字符串
                        return calcMaskByPrefixLength(interfaceAddress.getNetworkPrefixLength());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*通过子网掩码的位数计算子网掩码*/
    private static String calcMaskByPrefixLength(int length) {

        int mask = 0xffffffff << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int maskParts[] = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {

            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }

        String result = "";
        result = result + maskParts[0];

        for (int i = 1; i < maskParts.length; i++) {
            result = result + "." + maskParts[i];
        }

        return result;
    }


    /**
     * 获取本机IP前缀
     *
     * @param devAddress // 本机IP地址
     * @return String
     */
    public static String getLocAddrIndex(String devAddress) {
        if (!devAddress.equals("")) {
            return devAddress.substring(0, devAddress.lastIndexOf(".") + 1);
        }
        return null;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }


    /**
     * 获取广播地址
     */
    public static String getBroadcastAddress(int maskBit, String ip) {
        return getBroadcastAddress(getMaskMap(maskBit), ip);
    }

    /**
     * 获取广播地址
     */
    public static String getBroadcastAddress(String maskStr, String ip) {
        String[] ips = ip.split("\\.");
        String[] subnets = maskStr.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ips.length; i++) {
            ips[i] = String.valueOf((~Integer.parseInt(subnets[i]))
                    | (Integer.parseInt(ips[i])));
            sb.append(turnToStr(Integer.parseInt(ips[i])));
            if (i != (ips.length - 1))
                sb.append(".");
        }
        return turnToIp(sb.toString());
    }

    private static String turnToStr(int num) {
        String str = Integer.toBinaryString(num);
        int len = 8 - str.length();
        for (int i = 0; i < len; i++) {
            str = "0" + str;
        }
        if (len < 0)
            str = str.substring(24, 32);
        return str;
    }

    /**
     * 转换成Str
     */
    private static String turnToIp(String str) {
        String[] ips = str.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String ip : ips) {
            sb.append(turnToInt(ip));
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();

    }

    /**
     * 转换成int
     */

    private static int turnToInt(String str) {
        int total = 0;
        int top = str.length();
        for (int i = 0; i < str.length(); i++) {
            String h = String.valueOf(str.charAt(i));
            top--;
            total += ((int) Math.pow(2, top)) * (Integer.parseInt(h));
        }
        return total;

    }


    // 通过子网长度获取子网掩码
    public static String getMaskMap(int maskBit) {
        if (maskBit == 1) return "128.0.0.0";
        if (maskBit == 2) return "192.0.0.0";
        if (maskBit == 3) return "224.0.0.0";
        if (maskBit == 4) return "240.0.0.0";
        if (maskBit == 5) return "248.0.0.0";
        if (maskBit == 6) return "252.0.0.0";
        if (maskBit == 7) return "254.0.0.0";
        if (maskBit == 8) return "255.0.0.0";
        if (maskBit == 9) return "255.128.0.0";
        if (maskBit == 10) return "255.192.0.0";
        if (maskBit == 11) return "255.224.0.0";
        if (maskBit == 12) return "255.240.0.0";
        if (maskBit == 13) return "255.248.0.0";
        if (maskBit == 14) return "255.252.0.0";
        if (maskBit == 15) return "255.254.0.0";
        if (maskBit == 16) return "255.255.0.0";
        if (maskBit == 17) return "255.255.128.0";
        if (maskBit == 18) return "255.255.192.0";
        if (maskBit == 19) return "255.255.224.0";
        if (maskBit == 20) return "255.255.240.0";
        if (maskBit == 21) return "255.255.248.0";
        if (maskBit == 22) return "255.255.252.0";
        if (maskBit == 23) return "255.255.254.0";
        if (maskBit == 24) return "255.255.255.0";
        if (maskBit == 25) return "255.255.255.128";
        if (maskBit == 26) return "255.255.255.192";
        if (maskBit == 27) return "255.255.255.224";
        if (maskBit == 28) return "255.255.255.240";
        if (maskBit == 29) return "255.255.255.248";
        if (maskBit == 30) return "255.255.255.252";
        if (maskBit == 31) return "255.255.255.254";
        if (maskBit == 32) return "255.255.255.255";
        return "-1";
    }
}
