package com.fgsqw.lanshare.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.fgsqw.lanshare.pojo.FileSyncData;
import com.fgsqw.lanshare.utils.LLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件同步数据库工具类
 * <p>负责管理文件同步配置,存储和查询设备文件夹同步信息</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class FileSyncDBUtil extends SQLiteOpenHelper {

    /** 数据库版本 */
    private static final int DB_VERSION = 1;
    
    /** 数据库名称 */
    private static final String DB_NAME = "file_sync_data.db";
    
    /** 表名 */
    public static final String TABLE_NAME = "file_sync_table";

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public FileSyncDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 添加文件同步数据
     *
     * @param folderPath 文件同步数据对象
     */
    public void addFileSyncData(FileSyncData folderPath) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("device_id", folderPath.getDeviceId());
            values.put("device_name", folderPath.getDeviceName());
            values.put("folder_path", folderPath.getFolderPath());
            values.put("create_time", folderPath.getCreateTime());
            // 添加 "name" 字段
            values.put("name", folderPath.getName());
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error("error", e);
        }
    }

    /**
     * 查询文件同步数据
     *
     * @param deviceId 设备 ID
     * @return 文件同步数据,如果未找到则返回 null
     */
    @SuppressLint("Range")
    public FileSyncData queryFileSyncData(String deviceId) {
        SQLiteDatabase db = getWritableDatabase();
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where device_id = ?1", new String[]{deviceId});
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int index = 0;
                String fetchedDeviceId = cursor.getString(index++);
                String deviceName = cursor.getString(index++);
                String folderPath = cursor.getString(index++);
                long createTime = cursor.getLong(index++);
                // 从数据库中读取 "name" 字段
                String name = cursor.getString(index++);
                FileSyncData fileSyncData = new FileSyncData();
                fileSyncData.setDeviceId(fetchedDeviceId);
                fileSyncData.setDeviceName(deviceName);
                fileSyncData.setFolderPath(folderPath);
                fileSyncData.setCreateTime(createTime);
                fileSyncData.setName(name);
                return fileSyncData;
            }
        }
        return null;
    }

    /**
     * 查询文件同步列表
     *
     * @return 文件同步列表
     */
    public List<FileSyncData> queryList() {
        SQLiteDatabase db = getWritableDatabase();
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME, new String[]{});
        List<FileSyncData> list = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int index = 0;
                String deviceId = cursor.getString(index++);
                String deviceName = cursor.getString(index++);
                String folderPath = cursor.getString(index++);
                long createTime = cursor.getLong(index++);
                // 从数据库中读取 "name" 字段
                String name = cursor.getString(index++);
                FileSyncData fileSyncData = new FileSyncData();
                fileSyncData.setDeviceId(deviceId);
                fileSyncData.setDeviceName(deviceName);
                fileSyncData.setFolderPath(folderPath);
                fileSyncData.setCreateTime(createTime);
                fileSyncData.setName(name);
                list.add(fileSyncData);
            }
        }
        return list;
    }

    /**
     * 删除文件同步数据
     *
     * @param deviceId 设备 ID
     */
    public void deleteFileSyncData(String deviceId) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "delete from " + TABLE_NAME + " where device_id = ?1";
        db.execSQL(sql, new Object[]{deviceId});
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME +
                " (device_id VARCHAR(32) primary key, device_name VARCHAR(255), folder_path VARCHAR(255), create_time INTEGER, name VARCHAR(255))";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
