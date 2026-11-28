package com.fgsqw.lanshare.config;

public class LVersion {
    // 数据版本/协议版本1
    public static final int DATA_VERSION_1 = 1;
    public static final int DATA_VERSION_2 = 2;
    public static final int DATA_VERSION_3 = 3;
    public static final int DATA_VERSION_4 = 4;
    // 数据版本/协议版本5: 在 V4 基础上增量支持文件/文件夹断点续传(V5 为 V4 超集,复用 NEW_VERSION_4 头)
    public static final int DATA_VERSION_5 = 5;
}
