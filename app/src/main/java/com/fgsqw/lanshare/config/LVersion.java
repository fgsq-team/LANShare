package com.fgsqw.lanshare.config;

public class LVersion {
    // 数据版本/协议版本1
    public static final int DATA_VERSION_1 = 1;
    public static final int DATA_VERSION_2 = 2;
    public static final int DATA_VERSION_3 = 3;
    public static final int DATA_VERSION_4 = 4;
    // v5：支持 4 段并行传输（FS_SHARE_SEG）。
    // 与 v1~v3 的区别只在「新协议可选」：老版本接收端收到 v5 声明后，
    // 发送端会先判断对端版本，不支持就自动回落到单流 FS_SHARE_FILE。
    public static final int DATA_VERSION_5 = 5;
}
