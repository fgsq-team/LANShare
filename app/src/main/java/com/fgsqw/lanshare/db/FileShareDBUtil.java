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

public class FileShareDBUtil extends SQLiteOpenHelper {

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "file_share.db";
    public static final String TABLE_NAME = "file_share";

    public FileShareDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

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
