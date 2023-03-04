package com.fgsqw.lanshare.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.database.sqlite.SQLiteOpenHelper;
import com.fgsqw.lanshare.pojo.MessageContent;
import com.fgsqw.lanshare.pojo.MessageFileContent;
import com.fgsqw.lanshare.pojo.MessageUriContent;
import com.fgsqw.lanshare.utils.ByteUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息持久化
 */
public class MesssageDButil  extends SQLiteOpenHelper {

//    private final DBHelper dbHelper;

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "msg.db";
    public static final String TABLE_NAME = "LANShre_Msg";


    public MesssageDButil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    public void addMessage(MessageContent messageContent) {
        try {
            byte[] data = ByteUtil.objectToByte(messageContent);
            SQLiteDatabase db = getWritableDatabase();
            String sql = "insert into " + TABLE_NAME + " values (?,?,1)";
            db.execSQL(sql, new Object[]{messageContent.getId(), data});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateMessage(MessageContent messageContent) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            byte[] data = ByteUtil.objectToByte(messageContent);
            String sql = "update " + TABLE_NAME + " set message = ?2 where id = ?1 ";
            db.execSQL(sql, new Object[]{messageContent.getId(), data});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addListMessage(List<MessageContent> messageContent) {
        for (MessageContent content : messageContent) {
            addMessage(content);
        }
    }

    public void delMessage(MessageContent messageContent) {
//        SQLiteDatabase db = ordergetWritableDatabase();
//        db.delete(OrderTABLE_NAME, "id = ?", new String[]{messageContent.getId()});
        SQLiteDatabase db = getWritableDatabase();
        String sql = "update " + TABLE_NAME + " set isdel = -1 where id = ?1 ";
        db.execSQL(sql, new Object[]{messageContent.getId()});
    }

    public void delListMessage(List<MessageContent> messageContent) {
        for (MessageContent content : messageContent) {
            delMessage(content);
        }
    }


    public List<MessageContent> queryMessage() {
        List<MessageContent> messageContents = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where isdel = 1 ", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                byte[] data = cursor.getBlob(cursor.getColumnIndex("message"));
                try {
                    MessageContent messageContent = ByteUtil.byteToObject(data);
                    if (messageContent == null) continue;
                    if (messageContent instanceof MessageUriContent) {
                        MessageUriContent messageUriContent = (MessageUriContent) messageContent;
                        if (messageUriContent.existStatus(MessageContent.IN)) {
                            messageUriContent.setStatus(MessageContent.ERROR);
                            messageUriContent.setStateMessage("未完成");
                        } else if (messageUriContent.existStatus(MessageContent.SUCCESS)) {
                            messageUriContent.setStateMessage("已完成");
                        }
                    } else if (messageContent instanceof MessageFileContent) {
                        MessageFileContent messageFileContent = (MessageFileContent) messageContent;
                        if (messageFileContent.existStatus(MessageContent.IN)) {
                            messageFileContent.setStatus(MessageContent.ERROR);
                            messageFileContent.setStateMessage("未完成");
                        } else if (messageFileContent.existStatus(MessageContent.SUCCESS)) {
                            File file = new File(messageFileContent.getPath());
                            if (!file.exists()) {
                                messageFileContent.setStatus(MessageContent.ERROR | MessageContent.FILE_NOT_EXIST);
                                messageFileContent.setStateMessage("文件已被删除");
                            }
                        }
                    }
                    messageContents.add(messageContent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return messageContents;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key, message blob,isdel INTEGER)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
