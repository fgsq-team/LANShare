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

/**
 * Token 数据库工具类
 * <p>负责管理设备访问令牌,存储和查询设备 Token 信息</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class TokenDBUtil extends SQLiteOpenHelper {

    /** 数据库版本 */
    private static final int DB_VERSION = 1;
    
    /** 数据库名称 */
    private static final String DB_NAME = "token_list_new_v1.db";
    
    /** 表名 */
    public static final String TABLE_NAME = "token";

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public TokenDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    /**
     * 添加 Token
     *
     * @param token  Token 字符串
     * @param custom 是否自定义
     * @param pass   密码
     * @param ip     IP 地址
     * @param name   设备名称
     */
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

    /**
     * 根据 Token 查询
     *
     * @param token Token 字符串
     * @return Token 对象,如果未找到则返回 null
     */
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

    /**
     * 设置密码
     *
     * @param token Token 字符串
     * @param pass  密码
     * @return 是否设置成功
     */
    public boolean setPass(String token, int pass) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pass", pass);
        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(token)};
        int rowsUpdated = db.update(TABLE_NAME, values, whereClause, whereArgs);
        return rowsUpdated > 0;
    }

    /**
     * 更新设备名称
     *
     * @param token Token 字符串
     * @param name  设备名称
     * @return 是否更新成功
     */
    public boolean updateName(String token, String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(token)};
        int rowsUpdated = db.update(TABLE_NAME, values, whereClause, whereArgs);
        return rowsUpdated > 0;
    }

    /**
     * 查询自定义 IP
     *
     * @param customIp 自定义 IP
     * @return Token 对象,如果未找到则返回 null
     */
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

    /**
     * 查询 Token 列表
     *
     * @return Token 列表
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

    /**
     * 删除 Token
     *
     * @param id Token ID
     */
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
