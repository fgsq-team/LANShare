package com.fgsqw.lanshare.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.fgsqw.lanshare.pojo.network.NetInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String LOCALHOST = "127.0.0.1";
    public static final String LOCALHOST_V6 = "::1";
    public static final String LOCALHOST_V6_FAST = "fe80";


    // 监听热点状态
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    public static final int WIFI_AP_STATE_DISABLING = 10;

    public static final int WIFI_AP_STATE_DISABLED = 11;

    public static final int WIFI_AP_STATE_ENABLING = 12;

    public static final int WIFI_AP_STATE_ENABLED = 13;

    public static final int WIFI_AP_STATE_FAILED = 14;

    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";

    public static final int ALL_BIT = 32 /* ip address have 32 bits */;


    public static boolean isIPv4Address(String input) {
        // 简化的IPv4地址正则表达式
        String ipv4Pattern = "^(\\d{1,3}\\.){3}\\d{1,3}$";
        // 使用正则表达式验证输入
        Pattern pattern = Pattern.compile(ipv4Pattern);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public static boolean isWifiApEnabled(Context mContext) {
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get WiFi AP state" + e);
            LLog.error("error", e);
            return false;
        }
    }


    public static NetInfo createDefaultNetinfo() {
        NetInfo netInfo = new NetInfo();
        netInfo.setIp(LOCALHOST);
        netInfo.setMask(getMaskMap(24));
        netInfo.setName(UNKNOWN);
        netInfo.setBrodIp(getBroadcastAddress(24, LOCALHOST));
        return netInfo;
    }

   /* public static NetInfo getOneNetWorkInfo(Context context) {
        if (isWifiApEnabled(context)) {   //  如果有开启热点默认会使用热点的ip
            NetInfo netByName = getNetByName(WLAN_1);
            if (netByName == null) {
                netByName = getNetByName(WLAN_0);
            }
            if (netByName == null) {
                return createDefaultNetinfo();
            }
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
    }*/

    public static List<NetInfo> getNetInfoList() {
        List<NetInfo> netInfos = new ArrayList<>();
        try {
            // 获取本机所有的网络接口
            Enumeration<NetworkInterface> enNetworkInterface = NetworkInterface.getNetworkInterfaces();
            // 判断 Enumeration 对象中是否还有数据
            while (enNetworkInterface.hasMoreElements()) {
                NetworkInterface networkInterface = enNetworkInterface.nextElement();
                // 判断网口是否在使用并且支持多播
                if (!networkInterface.isUp() || !networkInterface.supportsMulticast()) {
                    continue;
                }
                String interfaceName = networkInterface.getName();
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (address instanceof Inet4Address) {
                        String hostAddress = address.getHostAddress();
                        if (StringUtils.isEmpty(hostAddress) || hostAddress.equals(LOCALHOST)) {
                            continue;
                        }
                        String maskMap = getMaskMap(interfaceAddress.getNetworkPrefixLength());
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast == null) {
                            continue;
                        }
                        String broadcastAddress = broadcast.getHostAddress();
                        NetInfo netInfo = new NetInfo();
                        netInfo.setIp(hostAddress);
                        netInfo.setMask(maskMap);
                        netInfo.setBrodIp(broadcastAddress);
                        netInfo.setInterfaceName(interfaceName);
                        netInfos.add(netInfo);
                    }
                }
            }
        } catch (Exception e) {
            LLog.error("error", e);
        }
        return netInfos;
    }

    public static List<NetInfo> getOpenIpv6() {
        List<NetInfo> netInfos = new ArrayList<>();
        List<NetInfo> ipv6NetInfoList = getIpv6NetInfoList();
        for (NetInfo netInfo : ipv6NetInfoList) {
            // 创建Socket
            Socket socket = new Socket();
            try {
                // 创建InetSocketAddress，指定IP地址和端口
                InetSocketAddress socketAddress = new InetSocketAddress("ipv6.baidu.com", 80);
                // 这里可以指定具体的网络接口，例如 "wlan0" 或 "eth0"
                // 如果不指定接口，系统将自动选择默认接口
                InetSocketAddress inetSocketAddress = new InetSocketAddress(netInfo.getIp(), 0);
                socket.bind(inetSocketAddress);
                // 连接到服务器
                socket.connect(socketAddress, 1000);
                netInfos.add(netInfo);
                // 在这里可以进行读写操作
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 关闭Socket
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return netInfos;
    }

    public static List<NetInfo> getIpv6NetInfoList() {
        List<NetInfo> netInfos = new ArrayList<>();
        try {
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
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (address instanceof Inet6Address) {
                        Inet6Address inet6Address = (Inet6Address) address;
                        String hostAddress = inet6Address.getHostAddress();
                        if (StringUtils.isEmpty(hostAddress)
                                || hostAddress.equals(LOCALHOST_V6)
                                || hostAddress.startsWith(LOCALHOST_V6_FAST)) {
                            continue;
                        }
                        NetInfo netInfo = new NetInfo();
                        netInfo.setIp(hostAddress);
                        netInfo.setIPV6(true);
                        netInfo.setName(networkInterface.getName());
                        netInfos.add(netInfo);
                        Log.d(TAG, "hostAddress:" + hostAddress);
                    }
                }
            }
        } catch (Exception e) {
            LLog.error("error", e);
        }
        return netInfos;
    }


    public static List<String> getNetInfoList1() {
        List<String> netInfos = new ArrayList<>();
        try {
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
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress() instanceof Inet6Address) {
                        String hostAddress = interfaceAddress.getAddress().getHostAddress();
                        if (hostAddress.equals(LOCALHOST)) {
                            continue;
                        }
                    /*    String maskMap = getMaskMap(interfaceAddress.getNetworkPrefixLength());
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast == null) {
                            continue;
                        }
                        String broadcastAddress = broadcast.getHostAddress();*/
                        netInfos.add(hostAddress);
                    }
                }
            }
        } catch (Exception e) {
            LLog.error("error", e);
        }
        return netInfos;
    }


    public static String getOneIPv6() {


        AtomicBoolean over = new AtomicBoolean(false);
        StringBuilder result = new StringBuilder();
        ThreadUtils.runThread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("http://api6.ipify.org/");
                connection = (HttpURLConnection) url.openConnection();
                //设置请求方法
                connection.setRequestMethod("GET");
                //设置连接超时时间（毫秒）
                connection.setConnectTimeout(5000);
                //设置读取超时时间（毫秒）
                connection.setReadTimeout(5000);
                //返回输入流
                InputStream in = connection.getInputStream();
                //读取输入流
                reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception e) {
                LLog.error("error", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        LLog.error("error", e);
                    }
                }
                if (connection != null) {//关闭连接
                    connection.disconnect();
                }
                over.set(true);
            }
        });
        while (!over.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        return result.toString();
    }


    //  通过子网长度获取子网掩码
    public static String getMaskMap(int length) {
        // Calculate the mask
        int mask = 0xffffffff << (32 - length);
        // Initialize a StringBuilder with an estimated capacity to avoid resizing
        StringBuilder result = new StringBuilder(15);
        // Extract each byte from the mask and append it to the result
        for (int i = 3; i >= 0; i--) {
            int part = (mask >> (i * 8)) & 0xff;
            result.append(part);
            if (i > 0) {
                result.append(".");
            }
        }
        return result.toString();
    }

    /**
     * 将int类型的IP转换为String类型
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    /**
     * 判断字符串是否为合法的 IPv4 地址（4 段 0-255）。
     * IPv6 地址含冒号、分段数不为 4，返回 false。
     */
    public static boolean isIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        try {
            for (String p : parts) {
                int n = Integer.parseInt(p);
                if (n < 0 || n > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * 将String类型的IP转换为int类型
     * 传入非 IPv4（如 IPv6）时返回 0，不抛 NumberFormatException。
     */
    public static int stringIP2intIP(String ip) {
        String[] ips = ip.split("\\.");
        if (ips.length != 4) {
            return 0;
        }
        try {
            return (Integer.parseInt(ips[0]) << 24)
                    | (Integer.parseInt(ips[1]) << 16)
                    | (Integer.parseInt(ips[2]) << 8)
                    | Integer.parseInt(ips[3]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int getMaskMapLength(String netmask) {
        String[] data = netmask.split("\\.");
        int len = 0;
        for (String n : data) {
            len += (int) (8 - Math.log(256 - Integer.parseInt(n)) / Math.log(2));
        }
        return len;
    }

    public static boolean subNet(String ip1, String ip2, String sub_mask) {
        // 任一地址不是 IPv4 直接返回 false，避免 IPv6 进入掩码/整数解析崩溃
        if (!isIPv4(ip1) || !isIPv4(ip2)) {
            return false;
        }
        return subNet(ip1, ip2, getMaskMapLength(sub_mask));
    }

    public static boolean subNet(String ip1, String ip2, int sub_mask) {
        if (!isIPv4(ip1) || !isIPv4(ip2)) {
            return false;
        }
        int mask = 0xFFFFFFFF;
        mask = mask << (ALL_BIT - sub_mask);
        int ipA = stringIP2intIP(ip1) & mask;
        int ipB = stringIP2intIP(ip2) & mask;
        return ipA == ipB;
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
}
