package com.fgsqw.lanshare.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fgsqw.lanshare.utils.LLog;

/**
 * APK 图标数据库工具类
 * <p>负责管理应用图标的缓存,存储和查询 APK 图标数据</p>
 *
 * @author fgsq
 * @version 1.0
 */
@SuppressLint("Range")
public class ApkIconDBUtil extends SQLiteOpenHelper {

    /** 数据库版本 */
    private static final int DB_VERSION = 1;
    
    /** 数据库名称 */
    private static final String DB_NAME = "apk_icon.db";
    
    /** 表名 */
    public static final String TABLE_NAME = "icon";

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public ApkIconDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 添加图标
     *
     * @param packageName 包名
     * @param path        图标路径
     * @param bytes       图标字节数据
     */
    public void addIcon(String packageName, String path, byte[] bytes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String sql = "insert into " + TABLE_NAME + " values (?,?,?,1)";
            db.execSQL(sql, new Object[]{packageName, bytes, path});
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error(e);
        }
    }

    /**
     * 根据包名查询图标
     *
     * @param packageName 包名
     * @return 图标字节数据,如果未找到则返回 null
     */
    public byte[] queryIconByPackageName(String packageName) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where id = ?1 and isdel = 1 ", new String[]{packageName});
            if (cursor != null) {
                try {
                    if (cursor.moveToNext()) {
                        return cursor.getBlob(cursor.getColumnIndex("data"));
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据包名查询图标路径
     *
     * @param packageName 包名
     * @return 图标路径,如果未找到则返回 null
     */
    public String queryPathByPackageName(String packageName) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where id = ?1 and isdel = 1 ", new String[]{packageName});
            if (cursor != null) {
                try {
                    if (cursor.moveToNext()) {
                        return cursor.getString(cursor.getColumnIndex("path"));
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key, data blob,path VARCHAR(255),isdel INTEGER)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
