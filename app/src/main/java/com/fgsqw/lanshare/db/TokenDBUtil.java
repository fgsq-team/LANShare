package com.fgsqw.lanshare.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fgsqw.lanshare.pojo.Token;
import com.fgsqw.lanshare.utils.LLog;

import java.util.ArrayList;
import java.util.List;

public class TokenDBUtil extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "token_list_new_v1.db";
    public static final String TABLE_NAME = "token";

    public TokenDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    public void addToken(String token, boolean custom, int pass, String ip, String name) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("id", token);
            values.put("custom", custom ? 1 : 0);
            values.put("pass", pass);
            values.put("ip", ip);
            values.put("name", name);
            values.put("isdel", 0);
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error("error", e);
        }
    }

    @SuppressLint("Range")
    public Token queryByToken(String token) {
        SQLiteDatabase db = getWritableDatabase();
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where id = ?1 and isdel = 0 ", new String[]{token});
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int index = 0;
                String id = cursor.getString(index++);
                String ip = cursor.getString(index++);
                String name = cursor.getString(index++);
                boolean custom = cursor.getInt(index++) == 1;
                int pass = cursor.getInt(index++);
                Token t = new Token();
                t.setToken(id);
                t.setIp(ip);
                t.setName(name);
                t.setPass(pass);
                t.setCustom(custom);
                return t;
            }
        }
        return null;
    }

    public boolean setPass(String token, int pass) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pass", pass);
        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(token)};
        int rowsUpdated = db.update(TABLE_NAME, values, whereClause, whereArgs);
        return rowsUpdated > 0;
    }

    public boolean updateName(String token, String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(token)};
        int rowsUpdated = db.update(TABLE_NAME, values, whereClause, whereArgs);
        return rowsUpdated > 0;
    }

    public Token queryCustonIp(String customIp) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "select * from " + TABLE_NAME + " where ip = ?1 and custom = 1 and isdel = 0 ";
        Cursor cursor = db.rawQuery(sql, new String[]{customIp});
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int index = 0;
                String id = cursor.getString(index++);
                String ip = cursor.getString(index++);
                String name = cursor.getString(index++);
                boolean custom = cursor.getInt(index++) == 1;
                int pass = cursor.getInt(index++);

                Token token = new Token();
                token.setToken(id);
                token.setIp(ip);
                token.setName(name);
                token.setPass(pass);
                token.setCustom(custom);
                return token;
            }
        }
        return null;
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
                String name = cursor.getString(index++);
                boolean custom = cursor.getInt(index++) == 1;
                int pass = cursor.getInt(index++);
                Token token = new Token();
                token.setToken(id);
                token.setIp(ip);
                token.setName(name);
                token.setPass(pass);
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
        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key,ip VARCHAR(16),name VARCHAR(16),custom INTEGER, pass INTEGER, isdel INTEGER)";
        db.execSQL(sql);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
