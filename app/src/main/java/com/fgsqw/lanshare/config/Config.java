package com.fgsqw.lanshare.config;

@SuppressWarnings("all")
public class Config {

    public static final int UDP_PORT = 4573;
    public static final int FILE_SERVER_PORT = 5856;
    public static final int SCANN_TIME = 5;            // 扫描时间
    public static String FILE_SAVE_PATH = "/sdcard/LANShare/";            // 文件保存路径

    public static final String[][] fileType =
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

}
