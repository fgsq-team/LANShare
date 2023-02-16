package com.fgsqw.lanshare.pojo;

public class mCmd {

    // 局域网通讯命令
    public static final int UDP_GET_DEVICES = 1001;        // 获取设备
    public static final int UDP_SET_DEVICES = 1002;        // 设置设备
    public static final int UDP_DEVICES_OFF_LINE = 1003;   // 设备下线
    public static final int UDP_DEVICES_MESSAGE = 1004;    // 广播消息
    public static final int UDP_DEVICES_MESSAGE_TO_CLIPBOARD = 1005;    // 广播消息到剪切板

    // 文件服务命令
    public static final int FS_SHARE_FILE = 1101;    // 发送文件
    public static final int FS_AGREE = 1102;         // 同意
    public static final int FS_NOT_AGREE = 1103;     // 不同意

    public static final int FS_ADD_DEVICE = 1102;    // 添加设备

    // 使用byte命令避免像int命令那样需要两边转换为byte
    public static final byte FS_DATA = 1;            // 数据
    public static final byte FS_END = 2;             // 接收结束
    public static final byte FS_CLOSE = 3;           // 取消

    // Service 连接命令
    public static final int SERVICE_IF_RECIVE_FILES = 1201;    // 是否接收文件
    public static final int SERVICE_SHOW_PROGRESS = 1202;      // 显示文件进度框
    public static final int SERVICE_PROGRESS = 1203;           // 文件进度
    public static final int SERVICE_CLOSE_PROGRESS = 1204;     // 关闭文件进度框
    public static final int SERVICE_UPDATE_DEVICES = 1205;     // 更新设备列表
    public static final int SERVICE_ADD_MESSGAGE = 1206;       // 添加一条消息
    public static final int SERVICE_COMPLETE_COUNT = 1207;     // 文件夹传输文完成数量
    public static final int SERVICE_NETWORK_CHANGES = 1208;    // 网络变化


    public static final int FILE_IMAGE = 3001;       // 图片
    public static final int FILE_VIEDO = 3002;       // 视频
    public static final int FILE_FILE = 3003;        // 文件
    public static final int FILE_FOLDER = 3004;      // 文件夹

}
