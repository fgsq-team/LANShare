package com.fgsqw.lanshare.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fgsqw.lanshare.utils.LLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int DB_VERSION = 2;
    
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

    /**
     * 批量查询所有已缓存的图标
     * <p>一次数据库查询替代 N 次单独查询,大幅提升加载速度</p>
     *
     * @return 缓存图标映射表(包名 → 图标字节数据)
     */
    public Map<String, byte[]> queryAllIcons() {
        Map<String, byte[]> result = new HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("select id, data from " + TABLE_NAME + " where isdel = 1", null);
            if (cursor != null) {
                try {
                    int idIndex = cursor.getColumnIndex("id");
                    int dataIndex = cursor.getColumnIndex("data");
                    while (cursor.moveToNext()) {
                        String packageName = cursor.getString(idIndex);
                        byte[] data = cursor.getBlob(dataIndex);
                        if (packageName != null && data != null) {
                            result.put(packageName, data);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            LLog.error("Error querying all icons", e);
        }
        return result;
    }

    /**
     * 批量保存图标到数据库
     * <p>使用事务批量写入,比逐条插入快数倍</p>
     *
     * @param icons 待保存的图标列表(每项包含包名、路径、图标数据)
     */
    public void batchAddIcons(List<IconEntry> icons) {
        if (icons == null || icons.isEmpty()) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                for (IconEntry entry : icons) {
                    ContentValues values = new ContentValues();
                    values.put("id", entry.packageName);
                    values.put("data", entry.iconBytes);
                    values.put("path", entry.path);
                    values.put("isdel", 1);
                    db.insertWithOnConflict(TABLE_NAME, null, values,
                            SQLiteDatabase.CONFLICT_REPLACE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            LLog.error("Error batch adding icons", e);
        }
    }

    /**
     * 图标数据条目(用于批量插入)
     */
    public static class IconEntry {
        public final String packageName;
        public final String path;
        public final byte[] iconBytes;

        public IconEntry(String packageName, String path, byte[] iconBytes) {
            this.packageName = packageName;
            this.path = path;
            this.iconBytes = iconBytes;
        }
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
