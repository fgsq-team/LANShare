package com.fgsqw.lanshare.config;

import android.os.Environment;

import com.alibaba.fastjson.JSONArray;
import com.fgsqw.lanshare.utils.AESUtils;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.StringUtils;

import java.io.File;

public class Config {
    // 默认文件服务端口
    public static final int DEFAULT_FILE_SERVER_PORT = 5856;
    // 默认udp端口
    public static final int DEFAULT_UDP_PORT = 4573;
    // app名称
    public static final String APP_NAME = "LANShare";
    // 解密消息密钥的密钥
    public static final String KEY = "6c9b%8ErII@Rc&f";
    // 消息加密密钥(已加密)
    public static final String DEFAULT_MESSAGE_KEY = "e4be1373272c69e0932651d97187b746c6725b17bbe84ad0b0fe2d4e81fc1d6c0c633d8ebd7f0fea65a57a9d5529d214";
    // 消息加密密钥(已解密)
    public static String MESSAGE_KEY;
    // 魔法数字
    public static final int MAGIC_NUM = 0x66677371;
    // 数据版本/协议版本
    public static final int DATA_VERSION = LVersion.DATA_VERSION_5;

    /**
     * 分段并行传输：把一个文件切成几段、每段一条独立连接同时发。
     * 设为 1 即完全关闭，永远走原来的单流路径。
     * 单条 TCP 流的吞吐在无线局域网上有上限，用更多流去叠加带宽：
     * 实测同一条 WLAN 链路上 4 段聚合 52.5MB/s、8 段聚合 66~70MB/s（每流 8MB/s），
     * 说明每流调度是限制之一；16 段是继续抬聚合上限的实验值。
     * 注意：段数越多接收端缓冲越多（每段约 6MB），配合 manifest 的 largeHeap；
     * 且文件须 ≥ PARALLEL_MIN_SIZE × 段数 才可并行（否则回落单流）。
     */
    public static final int PARALLEL_SEGS = 16;
    // 小于这个大小不切段：连接握手和线程开销大于收益
    public static final long PARALLEL_MIN_SIZE = 8 * 1024 * 1024;
    // UDP端口
    public static int UDP_PORT = 4573;
    // 隐私协议版本
    public static final int PRIVACY_VERSION = 1;
    // 接受文件服务端口号
    public static int FILE_SERVER_PORT = 5856;
    // 局域网设备扫描时间间隔
    public static final int SCANN_TIME = 5;
    // 文件保存路径
    public static final String DEFAULT_FILE_SAVE_PATH = "/LANShare/";
    // 文件储存路径
    public static String FILE_SAVE_PATH = DEFAULT_FILE_SAVE_PATH;
    // 设备名称
    public static String USER_NAME = "";
    // 是否保存消息
    public static boolean SAVE_MESSAGE = true;
    // 接收媒体保存到相册
    public static boolean SAVE_TO_GALLERY = true;
    // 网页服务
    public static boolean WEB_SERVICE = true;
    // 网页开放访问
    public static boolean WEB_OPEN = false;
    // 新消息通知
    public static boolean MESSAGE_NOTIFICAION = false;
    // 保存文件分类
    public static boolean SAVE_FILES_CATEGORY = true;
    // 上次文件路径
    public static boolean LAST_FILE_PATH = false;
    // 同步通知消息
    public static boolean SYNC_NOTIFICATION = false;
    // 媒体同步功能
    public static boolean MEDIA_SYNC = false;
    // 文件搜索类型
    public static boolean[] SEARCH_FLAG = {true, true, true, true};
    // 日志保存
    public static boolean SAVE_LOG = false;
    public static  boolean DEFAULT_SELECT_ONLY_ONE_DEVICE = false;
    // 日志保存路径
    public static String SAVE_LOG_PATH;
    // 文件夹名称
    public static String FOLDER = "文件夹";
    // 设备唯一id
    public static String uniqueUUid;
    public static long lastMessageTime;
    // 文件分类
    public static final String[][] fileTypes =
            {
                    {"zip", "压缩包"},
                    {"rar", "压缩包"},
                    {"7z", "压缩包"},
                    {"apk", "软件"},
                    {"mp4", "视频"},
                    {"avi", "视频"},
                    {"rmvb", "视频"},
                    {"3gp", "视频"},
                    {"aac", "音频"},
                    {"m4a", "音频"},
                    {"ape", "音频"},
                    {"flac", "音频"},
                    {"wav", "音频"},
                    {"png", "图片"},
                    {"jpg", "图片"},
                    {"jpeg", "图片"},
                    {"gif", "图片"},
                    {"txt", "文档"},
                    {"doc", "文档"},
                    {"docx", "文档"},
                    {"obt", "文档"},
                    {"xls", "表格"},
                    {"xlsx", "表格"},
            };
    //    public static String sgin(String str) {
//        return MD5Utils.md5(str + ":" + APP_NAME + ":" + KEY);
//    }
    // 服务器地址
    public static final String SERVER = "http://fgsqw.top";

    public static void initConfig(PrefUtil prefUtil) {
        String filePath = prefUtil.getString(PreConfig.FILE_PATH);
        if (filePath.isEmpty()) {
            prefUtil.saveString(PreConfig.FILE_PATH, Config.DEFAULT_FILE_SAVE_PATH);
            filePath = Config.DEFAULT_FILE_SAVE_PATH;
        }
        String userName = prefUtil.getString(PreConfig.USER_NAME);
        if (userName.isEmpty()) {
            prefUtil.saveString(PreConfig.USER_NAME, android.os.Build.MODEL);
        }
        Config.FILE_SAVE_PATH = new File(Environment.getExternalStorageDirectory(), filePath).getPath() + File.separator;
        File file = new File(Config.FILE_SAVE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        Config.SAVE_MESSAGE = prefUtil.getBoolean(PreConfig.SAVE_MESSAGE, true);
        Config.SAVE_TO_GALLERY = prefUtil.getBoolean(PreConfig.SAVE_TO_GALLERY, true);
        Config.FILE_SERVER_PORT = prefUtil.getInt(PreConfig.TCP_PORT, Config.DEFAULT_FILE_SERVER_PORT);
        Config.UDP_PORT = prefUtil.getInt(PreConfig.UDP_PORT, Config.DEFAULT_UDP_PORT);
        Config.UDP_PORT = prefUtil.getInt(PreConfig.UDP_PORT, Config.DEFAULT_UDP_PORT);
        String messageKey = prefUtil.getString(PreConfig.MESSAGE_KEY, Config.DEFAULT_MESSAGE_KEY);
        Config.WEB_SERVICE = prefUtil.getBoolean(PreConfig.WEB_SERCICE, true);
        Config.WEB_OPEN = prefUtil.getBoolean(PreConfig.WEB_OPEN, false);
        Config.MESSAGE_NOTIFICAION = prefUtil.getBoolean(PreConfig.MESSAGE_NOTIFICAION, false);
        Config.LAST_FILE_PATH = prefUtil.getBoolean(PreConfig.S_LAST_FILE_PATH, false);
        Config.SAVE_FILES_CATEGORY = !prefUtil.getBoolean(PreConfig.SAVE_FILES_CATEGORY, false);
        Config.SYNC_NOTIFICATION = prefUtil.getBoolean(PreConfig.SYNC_NOTIFICATION, false);
        Config.MEDIA_SYNC = prefUtil.getBoolean(PreConfig.MEDIA_SYNC, false);
        Config.DEFAULT_SELECT_ONLY_ONE_DEVICE = prefUtil.getBoolean(PreConfig.DEFAULT_SELECT_ONLY_ONE_DEVICE, false);
        Config.USER_NAME = prefUtil.getString(PreConfig.USER_NAME, "");
        String decrypt = null;
        try {
            decrypt = AESUtils.decrypt(messageKey, Config.KEY);
            Config.MESSAGE_KEY = decrypt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Config.uniqueUUid = prefUtil.getString(PreConfig.UNIQUE_UUID);
        if (StringUtils.isEmpty(Config.uniqueUUid)) {
            Config.uniqueUUid = StringUtils.getUUID();
            prefUtil.saveString(PreConfig.UNIQUE_UUID, Config.uniqueUUid);
        }
        Config.SEARCH_FLAG =
                JSONArray.parseObject(
                        prefUtil.getString(PreConfig.SEARCH_FILE_TYPES,
                                "[true, true, true, true]"), boolean[].class
                );
    }
//    public static final String SERVER = "http://192.168.0.224:8881";

}
