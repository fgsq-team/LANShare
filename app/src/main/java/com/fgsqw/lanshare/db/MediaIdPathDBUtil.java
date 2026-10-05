package com.fgsqw.lanshare.db;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.fgsqw.lanshare.pojo.MediaIdPath;
import com.fgsqw.lanshare.utils.DateUtils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 媒体 ID 路径数据库工具类
 * <p>负责管理媒体文件 ID 与路径的映射关系,用于媒体同步功能</p>
 *
 * @author fgsq
 * @version 1.0
 */
@SuppressLint("Range")
public class MediaIdPathDBUtil extends SQLiteOpenHelper {

    /** 数据库版本 */
    private static final int DB_VERSION = 1;
    
    /** 数据库名称 */
    private static final String DB_NAME = "media_id_path_list.db";
    
    /** 表名 */
    public static final String TABLE_NAME = "media_id_path";

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public MediaIdPathDBUtil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 添加媒体 ID 路径映射
     *
     * @param id           媒体 ID
     * @param name         文件名
     * @param path         文件路径
     * @param creationTime 创建时间
     * @param isReceived   是否接收
     */
    public void addMediaIdPath(long id, String name, String path, Date creationTime, boolean isReceived) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            values.put("path", path);
            values.put("creation_time", DateUtils.formatDate(creationTime, "yyyy-MM-dd HH:mm:ss")); // Format date before inserting
            values.put("is_received", isReceived ? 1 : 0);
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询媒体 ID 路径映射
     *
     * @param id 媒体 ID
     * @return 媒体 ID 路径对象,如果未找到则返回 null
     */
    public MediaIdPath queryMediaIdPath(long id) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {"name", "path", "creation_time", "is_received"};
        String selection = "id = ?";
        String[] selectionArgs = {String.valueOf(id)};
        try (Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String path = cursor.getString(cursor.getColumnIndex("path"));
                Date creationTime = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex("creation_time")), "yyyy-MM-dd HH:mm:ss");
                boolean isReceived = cursor.getInt(cursor.getColumnIndex("is_received")) == 1;
                return new MediaIdPath(id, name, path, creationTime, isReceived);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 查询媒体 ID 路径列表
     *
     * @return 媒体 ID 路径列表
     */
    public List<MediaIdPath> queryList() {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {"id", "name", "path", "creation_time", "is_received"};
        try (Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null, null)) {
            List<MediaIdPath> list = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex("id"));
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    String path = cursor.getString(cursor.getColumnIndex("path"));
                    Date creationTime = DateUtils.parseDate(cursor.getString(cursor.getColumnIndex("creation_time")), "yyyy-MM-dd HH:mm:ss");
                    boolean isReceived = cursor.getInt(cursor.getColumnIndex("is_received")) == 1;
                    MediaIdPath mediaIdPath = new MediaIdPath(id, name, path, creationTime, isReceived);
                    list.add(mediaIdPath);
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除媒体 ID 路径映射
     *
     * @param id 媒体 ID
     */
    public void deleteMediaIdPath(long id) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = "id = ?";
        String[] whereArgs = {String.valueOf(id)};
        db.delete(TABLE_NAME, whereClause, whereArgs);
    }

    /**
     * 检查媒体 ID 是否存在
     *
     * @param id 媒体 ID
     * @return 是否存在
     */
    public boolean isIdExists(long id) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {"id"};
        String selection = "id = ?";
        String[] selectionArgs = {String.valueOf(id)};
        try (Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (id INTEGER PRIMARY KEY, name TEXT, path TEXT, creation_time TEXT, is_received INTEGER)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

}
