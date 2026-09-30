package com.fgsqw.lanshare.constants;

public class LCmd {

    // 局域网通讯命令
    public static final int UDP_GET_DEVICES = 1001;        // 获取设备
    public static final int UDP_SET_DEVICES = 1002;        // 设置设备(心跳设置)
    public static final int UDP_DEVICES_OFF_LINE = 1003;   // 设备下线
    public static final int UDP_DEVICES_MESSAGE = 1004;    // 广播消息
    public static final int UDP_DEVICES_MESSAGE_TO_CLIPBOARD = 1005;    // 广播消息到剪切板


    public static final int UDP_SEND_MEDIA_MUTE = 1006;      // (媒体) 静音
    public static final int UDP_SEND_MEDIA_RESTORE = 1007;   // (媒体) 恢复音量
    public static final int UDP_SEND_MEDIA_PAUSE = 1008;     // (媒体) 暂停
    public static final int UDP_SEND_MEDIA_NEXT = 1009;      // (媒体) 下一曲
    public static final int UDP_SEND_MEDIA_PREVIOUS = 1010;  // (媒体) 上一曲
    public static final int UDP_SEND_MAP = 1011;             // 发送GPS位置


    // 文件服务命令
    public static final int FS_SHARE_FILE = 1101;    // 发送文件
    public static final int FS_AGREE = 1102;         // 同意
    public static final int FS_NOT_AGREE = 1103;     // 不同意
    public static final int FS_ADD_DEVICE = 1104;    // 添加设备
    public static final int FS_MESSAGE = 1105;       // 消息
    public static final int FS_GET_NO_SYNC_MEDIA = 1106;    // 获取没有同步的媒体列表
    public static final int FS_SYNC_MEDIA = 1107;     // 同步媒体
    public static final int FS_UPDATE_APPS = 1108;     // 同步媒体
    public static final int FS_GET_APPS = 1109;     // 同步媒体
    // 分段并行传输：一条连接只传文件的某一段，接收端按偏移落盘。
    // 只在未加密 + 对端 DATA_VERSION_4 及以上时使用，否则自动回落到单流 FS_SHARE_FILE。
    public static final int FS_SHARE_SEG = 1110;    // 分段发送文件


    // 使用byte命令避免像int命令那样需要两边转换为byte
    public static final byte FS_DATA = 1;            // 数据
    public static final byte FS_END = 2;             // 接收结束
    public static final byte FS_CLOSE = 3;           // 取消
    public static final int FS_DATA_RECEIVED = 4;    // 数据接收完毕
    public static final int FS_NEXT = 5;    // 下一步
    public static final int FS_BREAK = 6;    // 跳出

    // Service 连接命令
    public static final int SERVICE_IF_RECIVE_FILES = 1201;    // 是否接收文件
    public static final int SERVICE_SHOW_PROGRESS = 1202;      // 显示文件进度框
    public static final int SERVICE_PROGRESS = 1203;           // 文件进度
    public static final int SERVICE_CLOSE_PROGRESS = 1204;     // 关闭文件进度框
    public static final int SERVICE_UPDATE_DEVICES = 1205;     // 更新设备列表
    public static final int SERVICE_ADD_MESSGAGE = 1206;       // 添加一条消息
    public static final int SERVICE_COMPLETE_COUNT = 1207;     // 文件夹传输文完成数量
    public static final int SERVICE_NETWORK_CHANGES = 1208;    // 网络变化
    public static final int SERVICE_HTTP_NEW_CLIENT = 1209;     // http新客户端
    public static final int SERVICE_MEDIA_CHANGES = 1210;     // http新客户端
    public static final int SERVICE_SYNC_SORT = 1211;     //
    public static final int SERVICE_UPDATE_APPS = 1212;     //
    public static final int SERVICE_GET_APPS = 1213;     //


    public static final int FILE_IMAGE = 3001;       // 图片
    public static final int FILE_VIEDO = 3002;       // 视频
    public static final int FILE_FILE = 3003;        // 文件
    public static final int FILE_FOLDER = 3004;      // 文件夹
    public static final int FILE_MESSAGE_TO_CLIP = 3006;     // 剪切板消息


    public static final int FRAGMENT_PLAY_MUSIC = 11000;     // 播放音乐

}
