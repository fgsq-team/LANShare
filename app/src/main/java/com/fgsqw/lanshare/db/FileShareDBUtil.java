package com.fgsqw.lanshare.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.fgsqw.lanshare.pojo.Token;
import com.fgsqw.lanshare.pojo.message.MessageDownloadInfoContent;
import com.fgsqw.lanshare.utils.LLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件分享数据库工具类
 * <p>负责管理文件分享记录,存储和查询文件分享信息</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class FileShareDBUtil extends SQLiteOpenHelper {

    /** 数据库版本 */
    private static final int DB_VERSION = 2;
    
    /** 数据库名称 */
    private static final String DB_NAME = "file_share.db";
    
    /** 表名 */
    public static final String TABLE_NAME = "file_share";

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public FileShareDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 添加分享记录
     *
     * @param id         分享 ID
     * @param name       文件名
     * @param path       文件路径
     * @param downloaded 是否已下载
     * @param days       有效天数
     */
    public void addShare(String id, String name, String path, boolean downloaded, int days) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            values.put("path", path);
            values.put("downloaded", downloaded ? 1 : 0);
            values.put("days", days);
            values.put("time", new Date().getTime());
            values.put("isdel", 0);
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error("error", e);
        }
    }

    /**
     * 查询分享记录
     *
     * @param id 分享 ID
     * @return 分享信息,如果未找到则返回 null
     */
    @SuppressLint("Range")
    public MessageDownloadInfoContent queryShare(String id) {
        SQLiteDatabase db = getWritableDatabase();
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where id = ?1 and isdel = 0 ", new String[]{id});
        if (cursor != null) {
            if (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex("path"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                boolean downloaded = cursor.getInt(cursor.getColumnIndex("downloaded")) == 1;
                int days = cursor.getInt(cursor.getColumnIndex("days"));
                long time = cursor.getInt(cursor.getColumnIndex("time"));
                MessageDownloadInfoContent fileInfo = new MessageDownloadInfoContent();
                fileInfo.setFileId(id);
                fileInfo.setName(name);
                fileInfo.setPath(path);
                fileInfo.setDays(days);
                fileInfo.setTime(time);
                fileInfo.setDownloaded(downloaded);
                return fileInfo;
            }
        }
        return null;
    }

    /**
     * 更新下载状态
     *
     * @param id         分享 ID
     * @param downloaded 是否已下载
     * @return 是否更新成功
     */
    public boolean updateDownloaded(String id, boolean downloaded) {
        String table = TABLE_NAME;
        ContentValues values = new ContentValues();
        values.put("downloaded", downloaded ? 1 : 0);
        String whereClause = "id=?";
        String[] whereArgs = new String[]{id};
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = db.update(table, values, whereClause, whereArgs);
        return rowsAffected == 1;
    }

    /**
     * 查询分享列表
     *
     * @return 分享列表
     */
    public List<Token> queryList() {
        SQLiteDatabase db = getWritableDatabase();
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where isdel = 0 ", new String[]{});
        List<Token> list = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int index = 0;
                String id = cursor.getString(index++);
                String ip = cursor.getString(index++);
                boolean custom = cursor.getInt(index++) == 1;
                Token token = new Token();
                token.setToken(id);
                token.setIp(ip);
                token.setCustom(custom);
                list.add(token);
            }
        }
        return list;
    }

    /**
     * 删除分享记录
     *
     * @param id 分享 ID
     */
    public void delToken(String id) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "delete from " + TABLE_NAME + " where id = ?1 ";
        db.execSQL(sql, new Object[]{id});
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key,name VARCHAR(255),path VARCHAR(255),downloaded INTEGER,days INTEGER,time INTEGER, isdel INTEGER)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

}
