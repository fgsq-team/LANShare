package com.fgsqw.lanshare.fragment.data;

import com.fgsqw.lanshare.pojo.file.ApkInfo;
import com.fgsqw.lanshare.pojo.file.MusicInfo;
import com.fgsqw.lanshare.pojo.network.MediaResult;

import java.util.List;

/**
 * 全局文件缓存
 */
public class AnyData {
    // 所有扫描到的Apk文件
    public static List<ApkInfo> apkFileList;
    // 所有媒体
    public static MediaResult mediaResult;
    // 音频文件
    public static List<MusicInfo> musicInfoList;
}
