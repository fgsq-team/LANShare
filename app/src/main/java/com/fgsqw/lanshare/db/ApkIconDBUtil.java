package com.fgsqw.lanshare.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ApkIconDBUtil extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "apk_icon.db";
    public static final String TABLE_NAME = "icon";

    public ApkIconDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    public void addIcon(String packageName, byte[] bytes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String sql = "insert into " + TABLE_NAME + " values (?,?,1)";
            db.execSQL(sql, new Object[]{packageName, bytes});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] queryByPackageName(String packageName) {
        SQLiteDatabase db = getWritableDatabase();
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where id = ?1 and isdel = 1 ", new String[]{packageName});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                return cursor.getBlob(cursor.getColumnIndex("data"));
            }
        }
        return null;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key, data blob,isdel INTEGER)";
        db.execSQL(sql);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
