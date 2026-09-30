package com.fgsqw.lanshare.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.database.sqlite.SQLiteOpenHelper;

import com.fgsqw.lanshare.pojo.message.*;
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
    private static final int MESSAGE_TIME = -1;
    private static final int MESSAGE = 1;
    private static final int MESSAGE_MEDIA = 2;
    private static final int MESSAGE_FILE = 3;
    private static final int MESSAGE_GPS = 4;

    public MesssageDButil(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void addMessage(MessageContent messageContent) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String sql = "insert into " + TABLE_NAME + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            if (messageContent instanceof MessageFileContent) {
                MessageFileContent fileContent = (MessageFileContent) messageContent;
                if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_VIDEO
                        || fileContent.getFileType() == MessageFileContent.FILE_TYPE_IMAGE) {
                    MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
                    db.execSQL(sql, new Object[]{
                            mediaContent.getId(),
                            mediaContent.getContent(),
                            MESSAGE_MEDIA,
                            mediaContent.isLeft(),
                            mediaContent.getDevMode(),
                            mediaContent.getUserName(),
                            mediaContent.getToUser(),
                            new Date().getTime(),
                            mediaContent.getStatus(),
                            mediaContent.getDataVersion(),
                            mediaContent.getPath(),
                            mediaContent.getLength(),
                            mediaContent.getIndex(),
                            mediaContent.getStateMessage(),
                            mediaContent.isTransfer(),
                            mediaContent.isVideo(),
                            mediaContent.getVideoTime(),
                            false,
                            false
                    });
                } else if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    MessageFolderContent messageFolderContent = (MessageFolderContent) messageContent;
                    db.execSQL(sql, new Object[]{
                            messageFolderContent.getId(),
                            messageFolderContent.getContent(),
                            MESSAGE_FILE,
                            messageFolderContent.isLeft(),
                            messageFolderContent.getDevMode(),
                            messageFolderContent.getUserName(),
                            messageFolderContent.getToUser(),
                            new Date().getTime(),
                            messageFolderContent.getStatus(),
                            messageFolderContent.getDataVersion(),
                            messageFolderContent.getPath(),
                            messageFolderContent.getLength(),
                            messageFolderContent.getIndex(),
                            messageFolderContent.getStateMessage(),
                            messageFolderContent.isTransfer(),
                            null,
                            null,
                            true,
                            false
                    });
                } else {
                    MessageFileContent messageFileContent = (MessageFileContent) messageContent;
                    db.execSQL(sql, new Object[]{
                            messageFileContent.getId(),
                            messageFileContent.getContent(),
                            MESSAGE_FILE,
                            messageFileContent.isLeft(),
                            messageFileContent.getDevMode(),
                            messageFileContent.getUserName(),
                            messageFileContent.getToUser(),
                            new Date().getTime(),
                            messageFileContent.getStatus(),
                            messageFileContent.getDataVersion(),
                            messageFileContent.getPath(),
                            messageFileContent.getLength(),
                            messageFileContent.getIndex(),
                            messageFileContent.getStateMessage(),
                            messageFileContent.isTransfer(),
                            null,
                            null,
                            false,
                            false
                    });
                }
            } else if (messageContent instanceof MessageTimeContent) {
                db.execSQL(sql, new Object[]{
                        messageContent.getId(),
                        null,
                        MESSAGE_TIME,
                        null,
                        null,
                        null,
                        ((MessageTimeContent) messageContent).getBindId(),
                        new Date().getTime(),
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
                        MESSAGE,
                        messageContent.isLeft(),
                        messageContent.getDevMode(),
                        messageContent.getUserName(),
                        messageContent.getToUser(),
                        new Date().getTime(),
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
                    "status = ?8," +
                    "dataVersion = ?9," +
                    "path = ?10," +
                    "length = ?11," +
                    "_index = ?12," +
                    "stateMessage = ?13," +
                    "nextStep = ?14," +
                    "isVideo = ?15," +
                    "videoTime = ?16," +
                    "isDir = ?17," +
                    "isdel = ?18 " +
                    "where id = ?1 ";
            if (messageContent instanceof MessageFileContent) {
                MessageFileContent fileContent = (MessageFileContent) messageContent;
                if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_VIDEO
                        || fileContent.getFileType() == MessageFileContent.FILE_TYPE_IMAGE) {
                    MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
                    db.execSQL(sql, new Object[]{
                            mediaContent.getId(),
                            mediaContent.getContent(),
                            MESSAGE_MEDIA,
                            mediaContent.isLeft(),
                            mediaContent.getDevMode(),
                            mediaContent.getUserName(),
                            mediaContent.getToUser(),
                            mediaContent.getStatus(),
                            mediaContent.getDataVersion(),
                            mediaContent.getPath(),
                            mediaContent.getLength(),
                            mediaContent.getIndex(),
                            mediaContent.getStateMessage(),
                            mediaContent.isTransfer(),
                            mediaContent.isVideo(),
                            mediaContent.getVideoTime(),
                            false,
                            false
                    });
                } else if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    MessageFolderContent messageFolderContent = (MessageFolderContent) messageContent;
                    db.execSQL(sql, new Object[]{
                            messageFolderContent.getId(),
                            messageFolderContent.getContent(),
                            MESSAGE_FILE,
                            messageFolderContent.isLeft(),
                            messageFolderContent.getDevMode(),
                            messageFolderContent.getUserName(),
                            messageFolderContent.getToUser(),
                            messageFolderContent.getStatus(),
                            messageFolderContent.getDataVersion(),
                            messageFolderContent.getPath(),
                            messageFolderContent.getLength(),
                            messageFolderContent.getIndex(),
                            messageFolderContent.getStateMessage(),
                            messageFolderContent.isTransfer(),
                            null,
                            null,
                            true,
                            false
                    });
                } else {
                    db.execSQL(sql, new Object[]{
                            fileContent.getId(),
                            fileContent.getContent(),
                            MESSAGE_FILE,
                            fileContent.isLeft(),
                            fileContent.getDevMode(),
                            fileContent.getUserName(),
                            fileContent.getToUser(),
                            fileContent.getStatus(),
                            fileContent.getDataVersion(),
                            fileContent.getPath(),
                            fileContent.getLength(),
                            fileContent.getIndex(),
                            fileContent.getStateMessage(),
                            fileContent.isTransfer(),
                            null,
                            null,
                            false,
                            false
                    });
                }
            } else if (messageContent instanceof MessageTimeContent) {
                db.execSQL(sql, new Object[]{
                        messageContent.getId(),
                        null,
                        MESSAGE_TIME,
                        null,
                        null,
                        null,
                        ((MessageTimeContent) messageContent).getBindId(),
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
                        MESSAGE,
                        messageContent.isLeft(),
                        messageContent.getDevMode(),
                        messageContent.getUserName(),
                        messageContent.getToUser(),
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

    /**
     * 删除所有消息
     */
    public void deleteAllMessage() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("update " + TABLE_NAME + " set isdel = 1");
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
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " where isdel = 0 order by createTime asc ", null);
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
                    if (messageType == MESSAGE) {
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
                    } else if (messageType == MESSAGE_MEDIA) {
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
                        messageMediaContent.setTransfer(nextStep == 1);
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
                    } else if (messageType == MESSAGE_FILE) {
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
                        messageFileContent.setTransfer(nextStep == 1);
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
                    } else if (messageType == MESSAGE_TIME) {
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

    /**
     * 分页查询消息记录
     * @param pageSize 每页大小
     * @param pageNum 页码（从0开始）
     * @return 消息列表
     */
    public List<MessageContent> queryMessage(int pageSize, int pageNum) {
        List<MessageContent> messageContents = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();

        // 获取总记录数
        int totalCount = 0;
        Cursor countCursor = db.rawQuery("SELECT COUNT(1) FROM " + TABLE_NAME + " WHERE isdel = 0", null);
        if (countCursor != null) {
            if (countCursor.moveToNext()) {
                totalCount = countCursor.getInt(0);
            }
            countCursor.close();
        }
        // 计算总页数
        int totalPages = (int) Math.ceil(totalCount / (float) pageSize);
        // 计算偏移量
        int offset = (totalPages - pageNum - 1) * pageSize;
        if (offset < 0) {
            // 超出范围，返回空列表
            return messageContents;
        }
        // 执行分页查询，按时间倒序排列
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE isdel = 0 ORDER BY createTime ASC LIMIT ? OFFSET ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(pageSize), String.valueOf(offset)});

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
                    if (messageType == MESSAGE) {
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
                    } else if (messageType == MESSAGE_MEDIA) {
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
                        messageMediaContent.setTransfer(nextStep == 1);
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
                    } else if (messageType == MESSAGE_FILE) {
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
                        messageFileContent.setTransfer(nextStep == 1);
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
                    } else if (messageType == MESSAGE_TIME) {
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
