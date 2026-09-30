package com.fgsqw.lanshare.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.database.sqlite.SQLiteOpenHelper;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.utils.ByteUtil;
import com.fgsqw.lanshare.utils.LLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 消息持久化
 */
public class MesssageDButil extends SQLiteOpenHelper {

    //    private final DBHelper dbHelper;
    private static final int DB_VERSION = 3;
    private static final String DB_NAME = "msg.db";
    public static final String TABLE_NAME = "LANShre_Msg";
    private static final int MESSASGE_TIME = -1;
    private static final int MESSASGE = 1;
    private static final int MESSASGE_MEDIA = 2;
    private static final int MESSASGE_FILE = 3;

    public MesssageDButil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void addMessage(MessageContent messageContent) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String sql = "insert into " + TABLE_NAME + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            if (messageContent instanceof MessageMediaContent) {
                MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
                db.execSQL(sql, new Object[]{
                        mediaContent.getId(),
                        mediaContent.getContent(),
                        MESSASGE_MEDIA,
                        mediaContent.isLeft(),
                        mediaContent.getDevMode(),
                        mediaContent.getUserName(),
                        mediaContent.getToUser(),
                        mediaContent.getCreateTime().getTime(),
                        mediaContent.getStatus(),
                        mediaContent.getDataVersion(),
                        mediaContent.getPath(),
                        mediaContent.getLength(),
                        mediaContent.getIndex(),
                        mediaContent.getStateMessage(),
                        mediaContent.isNextStep(),
                        mediaContent.isVideo(),
                        mediaContent.getVideoTime(),
                        false,
                        false
                });
            } else if (messageContent instanceof MessageFolderContent) {
                MessageFolderContent messageFolderContent = (MessageFolderContent) messageContent;
                db.execSQL(sql, new Object[]{
                        messageFolderContent.getId(),
                        messageFolderContent.getContent(),
                        MESSASGE_FILE,
                        messageFolderContent.isLeft(),
                        messageFolderContent.getDevMode(),
                        messageFolderContent.getUserName(),
                        messageFolderContent.getToUser(),
                        messageFolderContent.getCreateTime().getTime(),
                        messageFolderContent.getStatus(),
                        messageFolderContent.getDataVersion(),
                        messageFolderContent.getPath(),
                        messageFolderContent.getLength(),
                        messageFolderContent.getIndex(),
                        messageFolderContent.getStateMessage(),
                        messageFolderContent.isNextStep(),
                        null,
                        null,
                        true,
                        false
                });
            } else if (messageContent instanceof MessageFileContent) {
                MessageFileContent messageFileContent = (MessageFileContent) messageContent;
                db.execSQL(sql, new Object[]{
                        messageFileContent.getId(),
                        messageFileContent.getContent(),
                        MESSASGE_FILE,
                        messageFileContent.isLeft(),
                        messageFileContent.getDevMode(),
                        messageFileContent.getUserName(),
                        messageFileContent.getToUser(),
                        messageFileContent.getCreateTime().getTime(),
                        messageFileContent.getStatus(),
                        messageFileContent.getDataVersion(),
                        messageFileContent.getPath(),
                        messageFileContent.getLength(),
                        messageFileContent.getIndex(),
                        messageFileContent.getStateMessage(),
                        messageFileContent.isNextStep(),
                        null,
                        null,
                        false,
                        false
                });
            } else if (messageContent instanceof MessageTimeContent) {
                db.execSQL(sql, new Object[]{
                        messageContent.getId(),
                        null,
                        MESSASGE_TIME,
                        null,
                        null,
                        null,
                        ((MessageTimeContent) messageContent).getBindId(),
                        messageContent.getCreateTime().getTime(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                });
            } else {
                db.execSQL(sql, new Object[]{
                        messageContent.getId(),
                        messageContent.getContent(),
                        MESSASGE,
                        messageContent.isLeft(),
                        messageContent.getDevMode(),
                        messageContent.getUserName(),
                        messageContent.getToUser(),
                        messageContent.getCreateTime().getTime(),
                        messageContent.getStatus(),
                        messageContent.getDataVersion(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error("error", e);
        }
    }


    public void updateMessage(MessageContent messageContent) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            String sql = "update " + TABLE_NAME +
                    " set content = ?2," +
                    "messageType = ?3," +
                    "isLeft = ?4," +
                    "header = ?5," +
                    "userName = ?6," +
                    "toUser = ?7," +
                    "createTime = ?8," +
                    "status = ?9," +
                    "dataVersion = ?10," +
                    "path = ?11," +
                    "length = ?12," +
                    "_index = ?13," +
                    "stateMessage = ?14," +
                    "nextStep = ?15," +
                    "isVideo = ?16," +
                    "videoTime = ?17," +
                    "isDir = ?18," +
                    "isdel = ?19 " +
                    "where id = ?1 ";
            if (messageContent instanceof MessageMediaContent) {
                MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
                db.execSQL(sql, new Object[]{
                        mediaContent.getId(),
                        mediaContent.getContent(),
                        MESSASGE_MEDIA,
                        mediaContent.isLeft(),
                        mediaContent.getDevMode(),
                        mediaContent.getUserName(),
                        mediaContent.getToUser(),
                        mediaContent.getCreateTime().getTime(),
                        mediaContent.getStatus(),
                        mediaContent.getDataVersion(),
                        mediaContent.getPath(),
                        mediaContent.getLength(),
                        mediaContent.getIndex(),
                        mediaContent.getStateMessage(),
                        mediaContent.isNextStep(),
                        mediaContent.isVideo(),
                        mediaContent.getVideoTime(),
                        false,
                        false
                });
            } else if (messageContent instanceof MessageFolderContent) {
                MessageFolderContent messageFolderContent = (MessageFolderContent) messageContent;
                db.execSQL(sql, new Object[]{
                        messageFolderContent.getId(),
                        messageFolderContent.getContent(),
                        MESSASGE_FILE,
                        messageFolderContent.isLeft(),
                        messageFolderContent.getDevMode(),
                        messageFolderContent.getUserName(),
                        messageFolderContent.getToUser(),
                        messageFolderContent.getCreateTime().getTime(),
                        messageFolderContent.getStatus(),
                        messageFolderContent.getDataVersion(),
                        messageFolderContent.getPath(),
                        messageFolderContent.getLength(),
                        messageFolderContent.getIndex(),
                        messageFolderContent.getStateMessage(),
                        messageFolderContent.isNextStep(),
                        null,
                        null,
                        true,
                        false
                });
            } else if (messageContent instanceof MessageFileContent) {
                MessageFileContent messageFileContent = (MessageFileContent) messageContent;
                db.execSQL(sql, new Object[]{
                        messageFileContent.getId(),
                        messageFileContent.getContent(),
                        MESSASGE_FILE,
                        messageFileContent.isLeft(),
                        messageFileContent.getDevMode(),
                        messageFileContent.getUserName(),
                        messageFileContent.getToUser(),
                        messageFileContent.getCreateTime().getTime(),
                        messageFileContent.getStatus(),
                        messageFileContent.getDataVersion(),
                        messageFileContent.getPath(),
                        messageFileContent.getLength(),
                        messageFileContent.getIndex(),
                        messageFileContent.getStateMessage(),
                        messageFileContent.isNextStep(),
                        null,
                        null,
                        false,
                        false
                });
            } else if (messageContent instanceof MessageTimeContent) {
                db.execSQL(sql, new Object[]{
                        messageContent.getId(),
                        null,
                        MESSASGE_TIME,
                        null,
                        null,
                        null,
                        ((MessageTimeContent) messageContent).getBindId(),
                        messageContent.getCreateTime().getTime(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                });
            } else {
                db.execSQL(sql, new Object[]{
                        messageContent.getId(),
                        messageContent.getContent(),
                        MESSASGE,
                        messageContent.isLeft(),
                        messageContent.getDevMode(),
                        messageContent.getUserName(),
                        messageContent.getToUser(),
                        messageContent.getCreateTime().getTime(),
                        messageContent.getStatus(),
                        messageContent.getDataVersion(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error("error", e);
        }
    }


    public void addListMessage(List<? extends MessageContent> messageContent) {
        for (MessageContent content : messageContent) {
            addMessage(content);
        }
    }

    public void delMessage(String id) {
//        SQLiteDatabase db = ordergetWritableDatabase();
//        db.delete(OrderTABLE_NAME, "id = ?", new String[]{messageContent.getId()});
        SQLiteDatabase db = getWritableDatabase();
        String sql = "update " + TABLE_NAME + " set isdel = 1 where id = ?1 ";
        db.execSQL(sql, new Object[]{id});
    }

    public void delListMessage(List<MessageContent> messageContent) {
        for (MessageContent content : messageContent) {
            delMessage(content.getId());
        }
    }


    public long getLastMessageTime() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select createTime from " + TABLE_NAME + " where isdel = 0 and messageType = -1 ORDER BY createTime DESC LIMIT 1 ", null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        }
        return -1;
    }

    public String queryByBindId(String bingId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select id from " + TABLE_NAME + " where isdel = 0 and messageType = -1 and toUser =?1 LIMIT 1 ", new String[]{bingId});
        if (cursor != null) {
            if (cursor.moveToNext()) {
                return cursor.getString(0);
            }
        }
        return null;
    }

    public List<MessageContent> queryMessage() {
        List<MessageContent> messageContents = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where isdel = 0 ", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    int index = 0;
                    String id = cursor.getString(index++);
                    String content = cursor.getString(index++);
                    int messageType = cursor.getInt(index++);
                    int isLeft = cursor.getInt(index++);
                    int devMode = cursor.getInt(index++);
                    String userName = cursor.getString(index++);
                    String toUser = cursor.getString(index++);
                    long createTime = cursor.getLong(index++);
                    int status = cursor.getInt(index++);
                    int dataVersion = cursor.getInt(index++);
                    String path = cursor.getString(index++);
                    long length = cursor.getLong(index++);
                    int _index = cursor.getInt(index++);
                    String stateMessage = cursor.getString(index++);
                    int nextStep = cursor.getInt(index++);
                    int isVideo = cursor.getInt(index++);
                    String videoTime = cursor.getString(index++);
                    int isDir = cursor.getInt(index++);
                    int isdel = cursor.getInt(index++);
                    if (messageType == MESSASGE) {
                        MessageContent messageContent = new MessageContent();
                        messageContent.setId(id);
                        messageContent.setContent(content);
                        messageContent.setLeft(isLeft == 1);
                        messageContent.setDevMode(devMode);
                        messageContent.setUserName(userName);
                        messageContent.setToUser(toUser);
                        messageContent.setCreateTime(new Date(createTime));
                        messageContent.setStatus(status);
                        messageContent.setDataVersion(dataVersion);
                        messageContents.add(messageContent);
                    } else if (messageType == MESSASGE_MEDIA) {
                        MessageMediaContent messageMediaContent = new MessageMediaContent();
                        messageMediaContent.setId(id);
                        messageMediaContent.setContent(content);
                        messageMediaContent.setLeft(isLeft == 1);
                        messageMediaContent.setDevMode(devMode);
                        messageMediaContent.setUserName(userName);
                        messageMediaContent.setToUser(toUser);
                        messageMediaContent.setCreateTime(new Date(createTime));
                        messageMediaContent.setStatus(status);
                        messageMediaContent.setDataVersion(dataVersion);
                        messageMediaContent.setPath(path);
                        messageMediaContent.setLength(length);
                        messageMediaContent.setIndex(_index);
                        messageMediaContent.setStateMessage(stateMessage);
                        messageMediaContent.setNextStep(nextStep == 1);
                        messageMediaContent.setVideo(isVideo == 1);
                        messageMediaContent.setVideoTime(videoTime);
                        messageContents.add(messageMediaContent);
                        if (messageMediaContent.existStatus(MessageContent.IN)) {
                            messageMediaContent.setStatus(MessageContent.ERROR);
                            messageMediaContent.setStateMessage("未完成");
                        } else if (messageMediaContent.existStatus(MessageContent.SUCCESS)) {
                            File file = new File(messageMediaContent.getPath());
                            if (file.canRead() && !file.exists()) {
                                messageMediaContent.setStatus(MessageContent.ERROR | MessageContent.FILE_NOT_EXIST);
                                messageMediaContent.setStateMessage("文件已被删除");
                            }
                        }
                    } else if (messageType == MESSASGE_FILE) {
                        MessageFileContent messageFileContent;
                        if (isDir == 1) {
                            messageFileContent = new MessageFolderContent();
                        } else {
                            messageFileContent = new MessageFileContent();
                        }
                        messageFileContent.setId(id);
                        messageFileContent.setContent(content);
                        messageFileContent.setLeft(isLeft == 1);
                        messageFileContent.setDevMode(devMode);
                        messageFileContent.setUserName(userName);
                        messageFileContent.setToUser(toUser);
                        messageFileContent.setCreateTime(new Date(createTime));
                        messageFileContent.setStatus(status);
                        messageFileContent.setDataVersion(dataVersion);
                        messageFileContent.setPath(path);
                        messageFileContent.setLength(length);
                        messageFileContent.setIndex(_index);
                        messageFileContent.setStateMessage(stateMessage);
                        messageFileContent.setNextStep(nextStep == 1);
                        messageContents.add(messageFileContent);
                        if (messageFileContent.existStatus(MessageContent.IN)) {
                            messageFileContent.setStatus(MessageContent.ERROR);
                            messageFileContent.setStateMessage("未完成");
                        } else if (messageFileContent.existStatus(MessageContent.SUCCESS)) {
                            File file = new File(messageFileContent.getPath());
                            if (file.canRead() && !file.exists()) {
                                messageFileContent.setStatus(MessageContent.ERROR | MessageContent.FILE_NOT_EXIST);
                                messageFileContent.setStateMessage("文件已被删除");
                            }
                        }
                    } else if (messageType == MESSASGE_TIME) {
                        MessageTimeContent messageContent = new MessageTimeContent();
                        messageContent.setId(id);
                        messageContent.setCreateTime(new Date(createTime));
                        messageContent.setBindId(toUser);
                        messageContents.add(messageContent);
                    }
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
//        String sql = "create table if not exists " + TABLE_NAME + " (id VARCHAR(32) primary key, message blob,isdel INTEGER)";
        String sql1 = "create table if not exists "
                + TABLE_NAME
                + " (id VARCHAR(32) primary key," +
                " content text," +
                " messageType INTEGER," +
                "isLeft INTEGER," +
                "header INTEGER," +
                "userName text," +
                "toUser text," +
                "createTime INTEGER," +
                "status INTEGER," +
                "dataVersion INTEGER," +
                "path VARCHAR(255)," +
                "length INTEGER," +
                "_index INTEGER," +
                "stateMessage text," +
                "nextStep INTEGER," +
                "isVideo INTEGER," +
                "videoTime VARCHAR(19)," +
                "isDir INTEGER," +
                "isdel INTEGER)";
        db.execSQL(sql1);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
