package com.fgsqw.lanshare.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import com.fgsqw.lanshare.pojo.StorageInfo;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StorageUtils {

    /**
     * 获取存储设备列表
     */
    public static List<StorageInfo> getStorageList(Context context) {
        List<StorageInfo> storageInfoList = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            StorageManager storageManager = context.getSystemService(StorageManager.class);
            List<StorageVolume> volumeList = storageManager.getStorageVolumes();
            for (StorageVolume volume : volumeList) {
                if (null != volume/* && volume.isRemovable()*/) {
                    String label = volume.getDescription(context);
                    String status = volume.getState();
                    if (!status.equals("mounted")) {
                        continue;
                    }
                    String uuid = volume.getUuid();
                    boolean isEmulated = volume.isEmulated();
                    boolean isRemovable = volume.isRemovable();
                    try {
                        String mPath = "";
                        File directory = null;
                        // 获取磁盘路径
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // 安卓 30以上
                            directory = volume.getDirectory();
                            if (directory != null) {
                                mPath = directory.getPath();
                            }
                        } else { // 安卓 30以下
                            Class<?> myclass = Class.forName(volume.getClass().getName());
                            Method path = myclass.getDeclaredMethod("getPath");
                            path.setAccessible(true);
                            mPath = (String) path.invoke(volume);
                        }
                        if (StringUtils.isEmpty(mPath)) {
                            continue;
                        }
                        StorageInfo storageInfo = new StorageInfo();
                        try {
                            // 获取磁盘空间
                            StatFs statFs = new StatFs(mPath);
                            long avaibleSize = statFs.getAvailableBytes();//获取U盘可用空间
                            long totalSize = statFs.getTotalBytes();//获取U盘总空间
                            storageInfo.setAvaibleSize(avaibleSize);
                            storageInfo.setTotalSize(totalSize);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        storageInfo.setPath(mPath);
                        storageInfo.setName(label);
                        storageInfo.setUuid(uuid);
                        storageInfo.setRemovable(isRemovable);
                        storageInfo.setEmulated(isEmulated);
                        storageInfoList.add(storageInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            StorageManager mStorageManager = (StorageManager) context
                    .getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClazz;
            try {
                storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
                Method getPath = storageVolumeClazz.getMethod("getPath");
                Object result = getVolumeList.invoke(mStorageManager);
                final int length = Array.getLength(result);
                Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
                Method getState = storageVolumeClazz.getMethod("getState");
                for (int i = 0; i < length; i++) {
                    Object storageVolumeElement = Array.get(result, i);
                    String path = (String) getPath.invoke(storageVolumeElement);
                    Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);
                    String state = (String) getState.invoke(storageVolumeElement);
                    if (StringUtils.isEmpty(state) || !state.equals("mounted")) {
                        continue;
                    }
                    StorageInfo storageInfo = new StorageInfo();
                    storageInfo.setPath(path);
                    storageInfo.setName(primary ? "内置储存" : "外置储存");
                    storageInfo.setUuid(path);
                    storageInfo.setRemovable(false);
                    storageInfo.setEmulated(false);
                    storageInfoList.add(storageInfo);
                }
            } catch (Exception e) {
                LLog.error(e);
            }
        }
        return storageInfoList;
    }


}
