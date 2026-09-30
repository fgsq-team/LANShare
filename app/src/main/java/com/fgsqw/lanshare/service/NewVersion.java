package com.fgsqw.lanshare.service;

import android.os.Message;
import android.util.Log;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.file.FileInfo;
import com.fgsqw.lanshare.pojo.file.UriFileInfo;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.web.LHttpServer;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NewVersion {
    private static final String TAG = "NewVersion";

    private void encData(byte[] buffer, int len, int off, long index) {
        int j = 0;
        for (int i = off; i < len + off; i++) {
            int v = (buffer[i] - 1) ^ (int) ((index + j) & 0xFF);
            buffer[i] = (byte) v;
            j++;
        }
    }

    private void decData(byte[] buffer, int len, int off, long index) {
        int j = 0;
        for (int i = off; i < len + off; i++) {
            int v = (buffer[i] ^ (int) ((index + j) & 0xFF)) + 1;
            buffer[i] = (byte) v;
            j++;
        }
    }

    public void test(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        List<MessageFileContent> fileContentList = new ArrayList<>();
        int count = dataDec.getCount();
        boolean encData = dataDec.getBool();
        dataDec = new DataDec(buffer);
        Message mMessage;
        for (int i = 0; i < count; i++) {
            // 读取头数据
            if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                throw new RuntimeException("read error");
            // 从头数据中获取数据包大小
            int length = dataDec.getLength();
            // 接收数据包
            if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
                throw new RuntimeException("read error");
            dataDec.setData(buffer, buffer.length);
            // 文件大小
            long fileSize = dataDec.getLong();
            // 文件名称
            String fileName = dataDec.getString();
            int fileType = dataDec.getInt();
            String videoTime = dataDec.getString();
            Log.d(TAG, "filename:" + fileName + " fileSize:" + fileSize);
            if (fileType == LCmd.FILE_IMAGE || fileType == LCmd.FILE_VIEDO) {
                long mediaId = dataDec.getLongDefault(-1);
                MessageMediaContent mediaContent = new MessageMediaContent();
                mediaContent.setId(StringUtils.getUUID());
                mediaContent.setDataVersion(device.getDataVersion());
                mediaContent.setStatus(MessageContent.IN);
                mediaContent.setContent(fileName);
                mediaContent.setLength(fileSize);
                mediaContent.setIndex(i);
                mediaContent.setLeft(true);
                mediaContent.setUserName(device.getDevName());
                mediaContent.setVideo(fileType == LCmd.FILE_VIEDO);
                mediaContent.setVideoTime(videoTime);
                mediaContent.setMediaId(mediaId);
                mediaContent.setDevMode(device.getDevMode());
                fileContentList.add(mediaContent);
            } else if (fileType == LCmd.FILE_FOLDER) {
                // 获取文件数量
                int fileCount = dataDec.getInt();
                MessageFolderContent folderContent = new MessageFolderContent();
                folderContent.setId(StringUtils.getUUID());
                folderContent.setStatus(MessageContent.IN);
                folderContent.setContent(fileName);
                folderContent.setLength(fileSize);
                folderContent.setIndex(i);
                folderContent.setLeft(true);
                folderContent.setUserName(device.getDevName());
                folderContent.setFileCount(fileCount);
                folderContent.setDataVersion(device.getDataVersion());
                folderContent.setDevMode(device.getDevMode());
                fileContentList.add(folderContent);
            } else {
                MessageFileContent fileContent = new MessageFileContent();
                fileContent.setId(StringUtils.getUUID());
                fileContent.setStatus(MessageContent.IN);
                fileContent.setContent(fileName);
                fileContent.setLength(fileSize);
                fileContent.setIndex(i);
                fileContent.setLeft(true);
                fileContent.setUserName(device.getDevName());
                fileContent.setDataVersion(device.getDataVersion());
                fileContent.setDevMode(device.getDevMode());
                fileContentList.add(fileContent);
            }
        }
        RecvFileCallback recvFileCallback = new RecvFileCallback(device, fileContentList, client, input, out, encData) {
            @Override
            public void receviceFile(boolean isAgree) {
                startRecvFile(device, fileContentList, client, input, out, encData, isAgree);
            }
        };
        // 是否弹出确认接收dialog（SP 无记录时默认 true：无需确认）
        boolean isNotRecvDialog = App.getPrefUtil().getBoolean("not_recv_dialog", true);
        if (isNotRecvDialog) {
            recvFileCallback.receviceFile(true);
        } else {
            // 弹出是否接收文件请求弹窗
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_IF_RECIVE_FILES;
            mMessage.arg1 = count;
            mMessage.obj = recvFileCallback;
            messageSend(mMessage);
        }
    }

    public void startRecvFile(Device device, List<MessageFileContent> messageFileContents, Socket client, InputStream input, OutputStream out, boolean encData, boolean isAgree) {
        ThreadUtils.runThread(() -> {
//            Device device = (Device) objects[5];
            Message mMessage;
            DataEnc dataEnc = new DataEnc();
            // 返回是否接收文件
            try {
                if (isAgree) {
                    dataEnc.setCmd(LCmd.FS_AGREE);
                    IOUtil.write(out, dataEnc);
                } else {
                    dataEnc.setCmd(LCmd.FS_NOT_AGREE);
                    IOUtil.write(out, dataEnc);
                    IOUtil.closeIO(input, out, client);
                    return;
                }
            } catch (IOException e) {
                IOUtil.closeIO(input, out, client);
                LLog.error("error", e);
                return;
            }
            // 通知视图添加文件列表
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = messageFileContents;
            messageSend(mMessage);
//            try {
//                TimeUnit.MILLISECONDS.sleep(500);
//            } catch (InterruptedException ignored) {
//            }
            synchronized (recvBuffer) {
                // 接收文件列表遍历
                for (MessageFileContent fileContent : messageFileContents) {
                    // 接收文件总大小
                    long totalRecv = 0;
                    File file;
                    // 接收文件夹
                    if (fileContent instanceof MessageFolderContent) {
                        MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                        DataDec dataDec = new DataDec(recvBuffer);
                        file = new File(FileUtil.createPath(Config.FILE_SAVE_PATH, Config.FOLDER) + "/" + folderContent.getContent() + "/");
                        for (int j = 0; j < folderContent.getFileCount(); j++) {
                            // 读取文件信息
                            try {
                                if (!IOUtil.read(input, dataDec)) break;
                            } catch (IOException e) {
                                LLog.error("error", e);
                                break;
                            }
                            // 从头数据中获取数据包大小
                            long fileLength = dataDec.getLong();
                            String fileName = dataDec.getString();
                            Log.d(TAG, "接收文件:" + fileName + " 大小:" + fileLength);
                            File outFile = new File(FileUtil.createPath(Config.FILE_SAVE_PATH, Config.FOLDER) + "/", fileName);
                            // 防止重名文件覆盖
                            outFile = FileUtil.avoidDuplication(outFile);
                            long thatTotal = 0;
                            if (encData) {
                                thatTotal = baseRecvDec(client, input, out, dataDec, fileLength, totalRecv, folderContent.getLength(), outFile, folderContent);
                            } else {
                                thatTotal = baseRecv(client, input, out, dataDec, fileLength, totalRecv, folderContent.getLength(), outFile, folderContent);
                            }
                            if (thatTotal == -3) {
                                break;
                            } else if (thatTotal <= 0) {
                                continue;
                            } else {
                                folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                                mMessage = Message.obtain();
                                mMessage.what = LCmd.SERVICE_COMPLETE_COUNT;
                                mMessage.obj = folderContent;
                                messageSend(mMessage);
                            }
                            totalRecv += thatTotal;
                        }
                    } else {
                        // 接收单个文件
                        DataDec dataDec = new DataDec(recvBuffer);
                        String path = FileUtil.createPath(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileContent.getContent()));
                        file = new File(path + "/", fileContent.getContent());
                        file = FileUtil.avoidDuplication(file);
                        if (encData) {
                            totalRecv = baseRecvDec(client, input, out, dataDec, fileContent.getLength(), 0, fileContent.getLength(), file, fileContent);
                        } else {
                            totalRecv = baseRecv(client, input, out, dataDec, fileContent.getLength(), 0, fileContent.getLength(), file, fileContent);
                        }
                    }
                    // 接收成功设置文件路径 失败则删除文件
                    if (totalRecv != fileContent.getLength()) {
                        fileContent.setStatus(MessageContent.ERROR);
                        fileContent.setStateMessage("接收失败");
                    } else {
                        fileContent.setPath(file.getPath());
                        fileContent.setStatus(MessageContent.SUCCESS);
                        fileContent.setStateMessage("接收成功");
                        if (fileContent instanceof MessageMediaContent) {
                            Long mediaId = ((MessageMediaContent) fileContent).getMediaId();
                            if (mediaId > -1) {
                                LANService.getInstance().mediaIdPathDBUtil.addMediaIdPath(mediaId, fileContent.getContent(), fileContent.getPath(), new Date(), true);
                            }
                        }
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        // 响应给发送方继续发送文件
                        IOUtil.write(out, 2);
                    } catch (IOException e) {
                        LLog.error(e);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    // 更新视图
                    mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                    mMessage.obj = fileContent;
                    messageSend(mMessage);
                    LHttpServer.sendMessage(fileContent.getContent(), fileContent.getUserName(), fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), true, false);
                }
                IOUtil.closeIO(input, out, client);
            }
        });
    }

    // 接收文件缓存
    private final static byte[] recvBuffer = new byte[2 * 1024 * 1024];
    // 发送文件缓存
    private final static byte[] sendBuffer = new byte[2 * 1024 * 1024];

    public long baseRecv(Socket client, InputStream input, OutputStream out, DataDec dataDec, long fileLength, long mTotalRecv, long totalLength, File outFile, MessageFileContent fileContent) {
        File parentFile = outFile.getParentFile();
        // 文件夹存在创建文件夹
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        // 文件输出流
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
//            outFileStream = new MappedByteBufferOutputStream(outFile.getPath(),fileLength,recvBuffer.length);
        } catch (IOException e) {
            LLog.error("error", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            // 接收文件
            while (true) {
                // 接收文件信息
                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize()) break;
                int cmd = dataDec.getByteCmd();
                if (cmd == LCmd.FS_DATA) {          // 数据
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength) break;
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progress = (int) (totalRecv * 100 / totalLength);
                    if (progress != p) {
                        // 更新视图进度条
                        fileContent.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        messageSend(mMessage);
                        p = progress;
                    }
                    if (!fileContent.isNextStep()) {
                        DataEnc dataEnc = new DataEnc();
                        dataEnc.setCount(fileContent.getIndex());
                        IOUtil.write(out, dataEnc);
                        thatTotal = -3;
                    }
                } else if (cmd == LCmd.FS_END) {    // 传输完毕
                    break;
                } else /*if (cmd == LCmd.FS_CLOSE)*/ {  // 被动关闭传输
                    Log.d(TAG, "close");
                    T.s("接收：" + fileContent.getContent() + " 被中断");
                    thatTotal = -3;
                    break;
                }
            }
        } catch (IOException e) {
            LLog.error("error", e);
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }

    public long baseRecvDec(Socket client, InputStream input, OutputStream out, DataDec dataDec, long fileLength, long mTotalRecv, long totalLength, File outFile, MessageFileContent fileContent) {
        File parentFile = outFile.getParentFile();
        // 文件夹存在创建文件夹
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        // 文件输出流
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
//            outFileStream = new MappedByteBufferOutputStream(outFile.getPath(),fileLength,recvBuffer.length);
        } catch (IOException e) {
            LLog.error("error", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            // 接收文件
            while (true) {
                // 接收文件信息
                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize()) break;
                int cmd = dataDec.getByteCmd();
                if (cmd == LCmd.FS_DATA) {          // 数据
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength) break;
                    decData(recvBuffer, thatLength, DataEnc.getHeaderSize(), thatTotal);
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progress = (int) (totalRecv * 100 / totalLength);
                    if (progress != p) {
                        // 更新视图进度条
                        fileContent.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        messageSend(mMessage);
                        p = progress;
                    }
                    if (!fileContent.isNextStep()) {
                        DataEnc dataEnc = new DataEnc();
                        dataEnc.setCount(fileContent.getIndex());
                        IOUtil.write(out, dataEnc);
                        thatTotal = -3;
                    }
                } else if (cmd == LCmd.FS_END) {    // 传输完毕
                    break;
                } else /*if (cmd == LCmd.FS_CLOSE)*/ {  // 被动关闭传输
                    Log.d(TAG, "close");
                    T.s("接收：" + fileContent.getContent() + " 被中断");
                    thatTotal = -3;
                    break;
                }
            }
        } catch (IOException e) {
            LLog.error("error", e);
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }


    public void writeFiles(Socket socket, InputStream input, OutputStream out, Device device, List<MessageFileContent> messageFileContents, boolean encData) {
        try {
            cancelSendFile(messageFileContents,socket,input);
            DataEnc dataEnc = new DataEnc(sendBuffer);
            // 开始发送文件
            for (MessageFileContent fileContent : messageFileContents) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException ignored) {
                }
                dataEnc.reset();
                // OutputStream 是用来判断文件取消的，下面写出文件必须要用它
                long totalSend = 0;
                Class<? extends MessageFileContent> aClass = fileContent.getClass();
                if (aClass.equals(MessageFolderContent.class)) {
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                    // 遍历需要传输的文件
                    for (FileInfo fileInfo : folderContent.getFileInfoList()) {
                        File file = new File(folderContent.getBasePath());
                        String relativePath = fileInfo.getPath().replace(file.getParent(), "");
                        dataEnc.reset();
                        dataEnc.putLong(fileInfo.getLength());
                        dataEnc.putString(relativePath);
                        Log.d(TAG, "发送文件:" + relativePath + " 大小:" + fileInfo.getLength());
                        try {
                            IOUtil.write(out, dataEnc);
                        } catch (IOException e) {
                            LLog.error(e);
                            break;
                        }
                        InputStream fileIs;
                        try {
                            // Uri文件转流
                            if (fileInfo instanceof UriFileInfo) {
                                fileIs = FileUtil.getInputStreamFromUri(((UriFileInfo) fileInfo).getUri());
                            } else {
                                fileIs = new FileInputStream(fileInfo.getPath());
                            }
                        } catch (FileNotFoundException e) {
                            LLog.error(e);
                            continue;
                        }
                        // 文件发送
                        long thatSend = 0;
                        if (encData) {
                            // 文件发送
                            thatSend = baseSendEec(fileContent, fileIs, input, out, dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        } else {
                            // 文件发送
                            thatSend = baseSend(fileContent, fileIs, input, out, dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        }
                        if (thatSend == -3) {
                            break;
                        } else if (thatSend <= 0) {
                            continue;
                        } else {
                            folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                            Message mMessage = Message.obtain();
                            mMessage.what = LCmd.SERVICE_COMPLETE_COUNT;
                            mMessage.obj = folderContent;
                            messageSend(mMessage);
                        }
                        totalSend += thatSend;
                    }
                } else {
                    InputStream fileIs;
                    try {
                        if (fileContent instanceof MessageStreamContent) {
                            fileIs = ((MessageStreamContent) fileContent).getInputStream();
                        } else if (fileContent instanceof MessageUriContent) {
                            fileIs = FileUtil.getInputStreamFromUri(((MessageUriContent) fileContent).getUri());
                        } else {
//                            fileIs = new FileInputStream(fileContent.getPath());
                            fileIs = new MappedByteBufferInputStream(fileContent.getPath(), sendBuffer.length);
                        }
                    } catch (FileNotFoundException e) {
                        LLog.error(e);
                        T.s("发送文件失败，文件：" + fileContent.getContent() + "不存在");
                        continue;
                    }
                    if (encData) {
                        // 文件接收
                        totalSend += baseSendEec(fileContent, fileIs, input, out, dataEnc, 0, fileContent.getLength(), fileContent.getLength());
                    } else {
                        // 文件接收
                        totalSend += baseSend(fileContent, fileIs, input, out, dataEnc, 0, fileContent.getLength(), fileContent.getLength());
                    }
                }
                try {
                    // 等待接收方响应再继续发送文件
                    int read = input.read();
                } catch (IOException e) {
                    LLog.error(e);
                }
                if (totalSend != fileContent.getLength()) {
                    fileContent.setStatus(MessageContent.ERROR);
                    fileContent.setStateMessage("发送失败");
                } else {
                    fileContent.setStatus(MessageContent.SUCCESS);
                    fileContent.setStateMessage("发送成功");
                }
                LHttpServer.sendMessage(fileContent.getContent(), fileContent.getToUser() + " ← " + LANService.getInstance().getDevName(), fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), false, false);
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                mMessage.obj = fileContent;
                messageSend(mMessage);
                Log.d(TAG, "发送成功:" + totalSend);
            }
        } catch (Exception e) {
            LLog.error(e);
        } finally {
            IOUtil.closeIO(input, out, socket);
        }
    }

    public void cancelSendFile(List<MessageFileContent> messageFileContents, Socket client, InputStream input) {
        ThreadUtils.runThread(() -> {
            byte[] buffer = new byte[1024];
            DataDec dataDec = new DataDec(buffer);
            while (true) {
                try {
                    int read = input.read(buffer, 0, DataDec.getHeaderSize());
                    if (read == -1) {
                        break;
                    }
                    messageFileContents.get(dataDec.getCount()).setNextStep(false);
                } catch (IOException e) {
                    LLog.error(e);
                }
            }
        });
    }


    public long baseSend(MessageFileContent content, InputStream fileIs, InputStream mInput, OutputStream mOut, DataEnc dataEnc, long mTotalSend, long totalLength, long fileLength) {
        // 发送文件
//        Log.d(TAG, "发送文件:" + filePath + " 大小:" + fileLength);
        int ten;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(LCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        try {
            int dataLength = sendBuffer.length - DataEnc.getHeaderSize();
            content.setStatus(MessageContent.IN);
            // 读取时偏移掉头的位置
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), dataLength)) != -1) {
                dataEnc.setDataIndex(ten);
                IOUtil.write(mOut, dataEnc);
                if (!content.isNextStep()) {
                    thatSend = -3;
                    break;
                }
                totalSend += ten;
                thatSend += ten;
                int progress = (int) (totalSend * 100 / totalLength);
                if (progress != p) {
                    content.setProgress(progress);
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_PROGRESS;
                    mMessage.obj = content;
                    messageSend(mMessage);
                    p = progress;
                }
            }
        } catch (IOException e) {
            LLog.error(e);
            thatSend = 0;
        }
        dataEnc.reset();
        if (thatSend != fileLength) {
            dataEnc.setByteCmd(LCmd.FS_CLOSE);
        } else {
            dataEnc.setByteCmd(LCmd.FS_END);
        }
        try {
            IOUtil.write(mOut, dataEnc);
        } catch (IOException e) {
            LLog.error(e);
        }
        return thatSend;
    }

    public long baseSendEec(MessageFileContent content, InputStream fileIs, InputStream mInput, OutputStream mOut, DataEnc dataEnc, long mTotalSend, long totalLength, long fileLength) {
        // 发送文件
//        Log.d(TAG, "发送文件:" + filePath + " 大小:" + fileLength);
        int ten;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(LCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        try {
            int dataLength = sendBuffer.length - DataEnc.getHeaderSize();
            content.setStatus(MessageContent.IN);
            // 读取时偏移掉头的位置
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), dataLength)) != -1) {
                dataEnc.setDataIndex(ten);
                encData(sendBuffer, ten, DataEnc.getHeaderSize(), (int) thatSend);
                IOUtil.write(mOut, dataEnc);
                if (!content.isNextStep()) {
                    thatSend = -3;
                    break;
                }
                totalSend += ten;
                thatSend += ten;
                int progress = (int) (totalSend * 100 / totalLength);
                if (progress != p) {
                    content.setProgress(progress);
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_PROGRESS;
                    mMessage.obj = content;
                    messageSend(mMessage);
                    p = progress;
                }
            }
        } catch (IOException e) {
            LLog.error(e);
            thatSend = 0;
        }
        dataEnc.reset();
        if (thatSend != fileLength) {
            dataEnc.setByteCmd(LCmd.FS_CLOSE);
        } else {
            dataEnc.setByteCmd(LCmd.FS_END);
        }
        try {
            IOUtil.write(mOut, dataEnc);
        } catch (IOException e) {
            LLog.error(e);
        }
        return thatSend;
    }

    public void messageSend(Message message) {
        LANService.getInstance().messageSend(message);
    }
}
