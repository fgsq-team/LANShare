package com.fgsqw.lanshare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.fgsqw.lanshare.pojo.NetInfo;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetWorkUtil {

    private static final String TAG = "NetWorkUtil";

    // 有线网卡
    public static final String ETH_0 = "eth0";
    public static final String ETH_0_NAME = "有线网络";
    // wifi网卡
    public static final String WLAN_0 = "wlan0";
    public static final String WLAN_0_NAME = "WIFI";
    // 热点网卡
    public static final String WLAN_1 = "wlan1";
    public static final String WLAN_1_NAME = "热点";

    public static final String UNKNOWN = "未知";


    // 监听热点状态
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    public static final int WIFI_AP_STATE_DISABLING = 10;

    public static final int WIFI_AP_STATE_DISABLED = 11;

    public static final int WIFI_AP_STATE_ENABLING = 12;

    public static final int WIFI_AP_STATE_ENABLED = 13;

    public static final int WIFI_AP_STATE_FAILED = 14;

    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";


    public static boolean isWifiApEnabled(Context mContext) {
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get WiFi AP state" + e);
            return false;
        }
    }


    public static NetInfo createDefaultNetinfo() {
        NetInfo netInfo = new NetInfo();
        netInfo.setIp("127.0.0.1");
        netInfo.setMask(getMaskMap(24));
        netInfo.setName(UNKNOWN);
        netInfo.setBrodIp(getBroadcastAddress(24, "127.0.0.1"));
        return netInfo;
    }

    public static NetInfo getOneNetWorkInfo(Context context) {
        if (isWifiApEnabled(context)) {   //  如果有开启热点默认会使用热点的ip
            NetInfo netByName = getNetByName(WLAN_1);
            if (netByName == null)
                return createDefaultNetinfo();
            netByName.setName(WLAN_1_NAME);
            return netByName;
        } else {
            NetworkInfo info = ((ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                if (info.getType() == ConnectivityManager.TYPE_WIFI) {    // 当前使用无线网络
                    NetInfo netByName = getNetByName(WLAN_0);
                    if (netByName == null)
                        return createDefaultNetinfo();
                    netByName.setName(WLAN_0_NAME);
                    return netByName;
                } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) { // 当前使用的是有线网络
                    NetInfo netByName = getNetByName(ETH_0);
                    if (netByName == null)
                        return createDefaultNetinfo();
                    netByName.setName(ETH_0_NAME);
                    return netByName;
                }
            }
        }
        return createDefaultNetinfo();
    }

    public static NetInfo getNetByName(String interfaceName) {
        try {
            NetInfo netInfo = new NetInfo();
            // 获取本机所有的网络接口
            Enumeration<NetworkInterface> enNetworkInterface = NetworkInterface.getNetworkInterfaces();
            // 判断 Enumeration 对象中是否还有数据
            while (enNetworkInterface.hasMoreElements()) {
                // 获取 Enumeration 对象中的下一个数据
                NetworkInterface networkInterface = enNetworkInterface.nextElement();
                // 判断网口是否在使用
                if (!networkInterface.isUp()) {
                    continue;
                }
                if (!interfaceName.equals(networkInterface.getDisplayName())) {
                    // 判断是否时我们获取的网口
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress() instanceof Inet4Address) {
                        netInfo.setIp(interfaceAddress.getAddress().getHostAddress());
                        netInfo.setMask(getMaskMap(interfaceAddress.getNetworkPrefixLength()));
                        netInfo.setBrodIp(interfaceAddress.getBroadcast().getHostAddress());
                        return netInfo;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //  通过子网长度获取子网掩码
    public static String getMaskMap(int length) {

        int mask = 0xffffffff << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int maskParts[] = new int[partsNum];
        int selector = 0x000000ff;
        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }
        StringBuilder result = new StringBuilder();
        result.append(maskParts[0]);
        for (int i = 1; i < maskParts.length; i++) {
            result.append(".").append(maskParts[i]);
        }
        return result.toString();
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


    /*// 通过子网长度获取子网掩码
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
    }*/
}
