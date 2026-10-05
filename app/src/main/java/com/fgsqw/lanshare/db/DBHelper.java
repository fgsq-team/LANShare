package com.fgsqw.lanshare.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类
 * <p>负责管理消息数据库的创建和升级</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class DBHelper extends SQLiteOpenHelper {

    /** 数据库版本 */
    private static final int DB_VERSION = 1;
    
    /** 数据库名称 */
    private static final String DB_NAME = "msg.db";
    
    /** 表名 */
    public static final String TABLE_NAME = "LANShre_Msg";

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key, message blob,isdel INTEGER)";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }
}
