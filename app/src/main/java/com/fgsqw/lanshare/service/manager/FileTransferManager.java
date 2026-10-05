package com.fgsqw.lanshare.service.manager;

import android.os.Message;
import androidx.documentfile.provider.DocumentFile;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.LVersion;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.db.MediaIdPathDBUtil;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileSyncData;
import com.fgsqw.lanshare.pojo.SendTask;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.service.version.four.FileSend;
import com.fgsqw.lanshare.service.version.four.FileTransfer;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.web.LHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 文件传输管理器
 * <p>负责文件发送和接收的核心逻辑</p>
 * <p>主要功能:</p>
 * <ul>
 *   <li>文件发送和接收</li>
 *   <li>支持加密和未加密传输</li>
 *   <li>媒体文件同步</li>
 *   <li>应用更新管理</li>
 * </ul>
 *
 * @author fgsq
 * @version 1.0
 */
public class FileTransferManager {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(FileTransferManager.class);

    /** 接收文件缓冲区(2MB) */
    private static final byte[] recvBuffer = new byte[2 * 1024 * 1024];
    
    /** 发送文件缓冲区(2MB) */
    private static final byte[] sendBuffer = new byte[2 * 1024 * 1024];

    /** LAN 服务实例 */
    private final LANService service;
    
    /** 设备管理器 */
    private final DeviceManager deviceManager;
    
    /** 文件发送器 */
    private final FileSend fileSend;
    
    /** 媒体 ID 路径数据库工具 */
    private final MediaIdPathDBUtil mediaIdPathDBUtil;
    
    /** 文件发送等待列表 */
    private final List<SendTask> sendTasks = new Vector<>();

    /**
     * 构造函数
     *
     * @param service         LAN 服务实例
     * @param deviceManager   设备管理器
     * @param fileSend        文件发送器
     * @param mediaIdPathDBUtil 媒体 ID 路径数据库工具
     */
    public FileTransferManager(LANService service, DeviceManager deviceManager, 
                               FileSend fileSend, MediaIdPathDBUtil mediaIdPathDBUtil) {
        this.service = service;
        this.deviceManager = deviceManager;
        this.fileSend = fileSend;
        this.mediaIdPathDBUtil = mediaIdPathDBUtil;
    }

    /**
     * 开始接收文件
     */
    public void startReceivingFile(Device device, List<MessageFileContent> messageFileContents, 
                              Socket client, InputStream input, OutputStream out, 
                              boolean encData, boolean isAgree) {
        ThreadUtils.runThread(() -> {
            Message mMessage;
            DataEnc dataEnc = new DataEnc();
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
                logger.error("error: ", e);
                return;
            }
            service.sendShowProgressMeg(messageFileContents);
            synchronized (recvBuffer) {
                for (MessageFileContent fileContent : messageFileContents) {
                    long totalRecv = 0;
                    File file;
                    if (fileContent instanceof MessageFolderContent) {
                        MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                        DataDec dataDec = new DataDec(recvBuffer);
                        file = new File(FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER), folderContent.getContent());
                        file = FileUtil.avoidDuplication(file);
                        for (int j = 0; j < folderContent.getFileCount(); j++) {
                            try {
                                if (!IOUtil.read(input, dataDec)) break;
                            } catch (IOException e) {
                                logger.error("error: ", e);
                                break;
                            }
                            long fileLength = dataDec.getLong();
                            String fileName = dataDec.getString();
                            logger.debug("接收文件:" + fileName + " 大小:" + fileLength);
                            File outFile = new File(FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER), fileName);
                            long thatTotal = 0;
                            if (encData) {
                                thatTotal = baseRecvDec(client, input, out, dataDec,
                                        fileLength, totalRecv, folderContent.getLength(), outFile, folderContent);
                            } else {
                                thatTotal = baseRecv(client, input, out, dataDec,
                                        fileLength, totalRecv, folderContent.getLength(), outFile, folderContent);
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
                                service.messageSend(mMessage);
                            }
                            totalRecv += thatTotal;
                        }
                    } else {
                        DataDec dataDec = new DataDec(recvBuffer);
                        String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileContent.getContent()));
                        file = new File(path, fileContent.getContent());
                        file = FileUtil.avoidDuplication(file);
                        if (encData) {
                            totalRecv = baseRecvDec(client, input, out, dataDec,
                                    fileContent.getLength(), 0, fileContent.getLength(), file, fileContent);
                        } else {
                            totalRecv = baseRecv(client, input, out, dataDec,
                                    fileContent.getLength(), 0, fileContent.getLength(), file, fileContent);
                        }
                    }
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
                                mediaIdPathDBUtil.addMediaIdPath(mediaId, fileContent.getContent(), fileContent.getPath(), new Date(), true);
                            }
                        }
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        IOUtil.write(out, 2);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                    mMessage.obj = fileContent;
                    service.messageSend(mMessage);
                    if (Config.SAVE_MESSAGE) {
                        service.getMesssageDButil().updateMessage(fileContent);
                    }
                    LHttpServer.sendMessage(fileContent.getContent(), fileContent.getUserName(), fileContent.getPath(), 
                            fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), true, false);
                }
                IOUtil.closeIO(input, out, client);
            }
        });
    }

    /**
     * 基础接收（无加密）
     */
    public long baseRecv(Socket client, InputStream input, OutputStream out, DataDec dataDec,
                         long fileLength, long mTotalRecv, long totalLength, File outFile,
                         MessageFileContent fileContent) {
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
        } catch (IOException e) {
            logger.error("error: ", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            while (true) {
                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                    break;
                int cmd = dataDec.getByteCmd();
                if (cmd == LCmd.FS_DATA) {
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength)
                        break;
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progress = (int) (totalRecv * 100 / totalLength);
                    if (progress != p) {
                        fileContent.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        service.messageSend(mMessage);
                        p = progress;
                    }
                    if (fileContent.isTransfer()) {
                        IOUtil.write(out, LCmd.FS_NEXT);
                    } else {
                        thatTotal = -3;
                        IOUtil.write(out, LCmd.FS_BREAK);
                    }
                } else if (cmd == LCmd.FS_END) {
                    break;
                } else {
                    logger.debug("close");
                    T.s("接收：" + fileContent.getContent() + " 被中断");
                    thatTotal = -3;
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }

    /**
     * 基础接收（加密）
     */
    public long baseRecvDec(Socket client, InputStream input, OutputStream out, DataDec dataDec,
                            long fileLength, long mTotalRecv, long totalLength, File outFile,
                            MessageFileContent fileContent) {
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            logger.error("error: ", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            while (true) {
                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                    break;
                int cmd = dataDec.getByteCmd();
                if (cmd == LCmd.FS_DATA) {
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength)
                        break;
                    mUtil.decData(recvBuffer, thatLength, DataEnc.getHeaderSize(), thatTotal);
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progress = (int) (totalRecv * 100 / totalLength);
                    if (progress != p) {
                        fileContent.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        service.messageSend(mMessage);
                        p = progress;
                    }
                    if (fileContent.isTransfer()) {
                        IOUtil.write(out, LCmd.FS_NEXT);
                    } else {
                        thatTotal = -3;
                        IOUtil.write(out, LCmd.FS_BREAK);
                    }
                } else if (cmd == LCmd.FS_END) {
                    break;
                } else {
                    logger.debug("close");
                    T.s("接收：" + fileContent.getContent() + " 被中断");
                    thatTotal = -3;
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }

    /**
     * 文件发送
     */
    public void fileSend(Device fromDevice, Device device, List<MessageFileContent> fileList) {
        ThreadUtils.runThread(() -> fileSendSync(fromDevice, device, fileList));
    }

    /**
     * 文件发送同步
     */
    public void fileSendSync(Device fromDevice, Device device, List<MessageFileContent> fileList) {
        Socket socket = null;
        try {
            socket = service.createSocket(device);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();
            TimeUnit.MILLISECONDS.sleep(10);
            handleSend(fromDevice, device, socket, new CustomDataInputStream(input), 
                    new CustomDataOutputStream(output), fileList);
        } catch (Exception e) {
            logger.error("error: ", e);
            IOUtil.closeIO(input, output, socket);
        }
    }

    /**
     * 处理发送
     */
    public void handleSend(Device fromDevice, Device toDevice, Socket socket, 
                           CustomDataInputStream input, CustomDataOutputStream output, 
                           List<MessageFileContent> fileList) throws IOException {
        boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
        if (toDevice.getDataVersion() >= LVersion.DATA_VERSION_4) {
            fileSend.send(fromDevice, toDevice, socket, input, output, fileList);
        } else if (toDevice.getDataVersion() < LVersion.DATA_VERSION_4) {
            DataEnc dataEnc = deviceManager.makeDataEnc(fromDevice, toDevice, 1024 * 1024);
            dataEnc.setCmd(LCmd.FS_SHARE_FILE);
            dataEnc.setCount(fileList.size());
            dataEnc.putBool(encData);
            IOUtil.write(output, dataEnc);
            fileSendInternal(fromDevice, toDevice, socket, input, output, fileList);
            ThreadUtils.runThread(() -> {
                Lock sendLock = StringLockManager.getStringLock("sendBuffer");
                try {
                    sendLock.lock();
                    for (SendTask sendTask : sendTasks) {
                        writeFiles(sendTask.getSocket(), sendTask.getFileTransfer().getFromDevice(), 
                                sendTask.getFileTransfer().getFiles(), sendTask.isEncData());
                    }
                    sendTasks.clear();
                } catch (Exception e) {
                    logger.error("error: ", e);
                } finally {
                    sendLock.unlock();
                }
            });
        }
    }

    /**
     * 内部文件发送
     */
    private void fileSendInternal(Device fromDevice, Device toDevice, Socket socket, 
                                  InputStream input, OutputStream out, List<MessageFileContent> fileList) {
        boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
        try {
            Device mDevice = null;
            if (toDevice.isIPv4()) {
                for (Device d : deviceManager.localDevices) {
                    if (!toDevice.isIPv4() || NetWorkUtil.subNet(d.getDevIP(), toDevice.getDevIP(), d.getDevNetMask())) {
                        mDevice = d;
                        break;
                    }
                }
            } else {
                mDevice = deviceManager.makeIPv6Device();
            }
            if (mDevice == null) {
                return;
            }
            DataEnc dataEnc = deviceManager.makeDataEnc(mDevice, toDevice, 1024 * 1024);
            String userName = fromDevice.getDevName();
            for (int i = 0; i < fileList.size(); i++) {
                MessageFileContent fileInfo = fileList.get(i);
                dataEnc.reset();
                if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_STREAM) {
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    MessageStreamContent streamInfo = (MessageStreamContent) fileInfo;
                    streamInfo.setId(StringUtils.getUUID());
                    streamInfo.setContent(fileInfo.getName());
                    streamInfo.setLength(fileInfo.getLength());
                    streamInfo.setPath(fileInfo.getPath());
                    streamInfo.setIndex(i);
                    streamInfo.setLeft(false);
                    streamInfo.setUserName(userName);
                    streamInfo.setToUser(toDevice.getDevName());
                } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_IMAGE || 
                           fileInfo.getFileType() == MessageFileContent.FILE_TYPE_VIDEO) {
                    MessageMediaContent mediaInfo = (MessageMediaContent) fileInfo;
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    if (mediaInfo.isVideo()) {
                        dataEnc.putInt(LCmd.FILE_VIEDO);
                        mediaInfo.setVideo(true);
                    } else {
                        dataEnc.putInt(LCmd.FILE_IMAGE);
                        mediaInfo.setVideo(false);
                    }
                    String videoTime = mediaInfo.getVideoTime();
                    dataEnc.putString(videoTime == null ? "" : videoTime);
                    if (mediaInfo.getMediaId() > -1) {
                        dataEnc.putLong(mediaInfo.getMediaId());
                    }
                    mediaInfo.setId(StringUtils.getUUID());
                    mediaInfo.setContent(fileInfo.getName());
                    mediaInfo.setLength(fileInfo.getLength());
                    mediaInfo.setPath(fileInfo.getPath());
                    mediaInfo.setIndex(i);
                    mediaInfo.setLeft(false);
                    mediaInfo.setUserName(userName);
                    mediaInfo.setToUser(toDevice.getDevName());
                    mediaInfo.setVideoTime(videoTime);
                } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_URI) {
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    MessageUriContent fileContent = (MessageUriContent) fileInfo;
                    fileContent.setId(StringUtils.getUUID());
                    fileContent.setContent(fileInfo.getName());
                    fileContent.setLength(fileInfo.getLength());
                    fileContent.setPath(fileInfo.getPath());
                    fileContent.setIndex(i);
                    fileContent.setLeft(false);
                    fileContent.setUserName(userName);
                    fileContent.setToUser(toDevice.getDevName());
                } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    File file = new File(fileInfo.getPath());
                    if (fileInfo instanceof MessageUriContent) {
                        MessageUriContent uriFileInfo = (MessageUriContent) fileInfo;
                        DocumentFile fileRealNameFromUri = FileUtil.getDocumentFileFromTreeUri(uriFileInfo.getUri());
                        if (!fileRealNameFromUri.exists()) {
                            T.s((R.string.path_not_found));
                            continue;
                        }
                    } else {
                        if (!file.exists()) {
                            T.s((R.string.path_not_found));
                            continue;
                        }
                    }
                    List<MessageFileContent> fileInfos = new LinkedList<>();
                    long totalSize = 0;
                    T.s((R.string.scanning_files));
                    if (fileInfo instanceof MessageUriContent) {
                        MessageUriContent uriFileInfo = (MessageUriContent) fileInfo;
                        totalSize = DeviceDataScanner.calculateDocumentTreeSize(
                                FileUtil.getDocumentFileFromTreeUri(uriFileInfo.getUri()), 
                                new File(uriFileInfo.getPath()).getParent(), fileInfos);
                    } else {
                        totalSize = DeviceDataScanner.calculateDirectorySize(file, fileInfos);
                        MessageFolderContent folderContent = (MessageFolderContent) fileInfo;
                        folderContent.setId(StringUtils.getUUID());
                        folderContent.setFileCount(fileInfos.size());
                        folderContent.setLength(totalSize);
                        folderContent.setChildren(fileInfos);
                        folderContent.setPath(file.getPath());
                        folderContent.setLeft(false);
                        folderContent.setContent(file.getName());
                        folderContent.setIndex(i);
                        folderContent.setUserName(userName);
                        folderContent.setToUser(toDevice.getDevName());
                        folderContent.setPath(file.getPath());
                    }
                    T.s((R.string.file_scan_complete));
                    dataEnc.putLong(totalSize);
                    dataEnc.putString(file.getName());
                    dataEnc.putInt(LCmd.FILE_FOLDER);
                    dataEnc.putString("");
                    dataEnc.putInt(fileInfos.size());
                } else {
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    fileInfo.setId(StringUtils.getUUID());
                    fileInfo.setIndex(i);
                    fileInfo.setLeft(false);
                    fileInfo.setUserName(userName);
                    fileInfo.setToUser(toDevice.getDevName());
                }
                IOUtil.write(out, dataEnc);
            }
            IOUtil.read(input, dataEnc.getBuffer(), 0, DataEnc.getHeaderSize());
            DataDec dataDec = new DataDec(dataEnc.getBuffer(), DataEnc.getHeaderSize());
            if (dataDec.getCmd() == LCmd.FS_NOT_AGREE) {
                T.s(toDevice.getDevName() + " " + service.getString(R.string.cancel_file_reception));
                IOUtil.closeIO(input, out, socket);
                return;
            }
            service.sendShowProgressMeg(fileList);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        Lock sendLock = StringLockManager.getStringLock("sendBuffer");
        boolean tryLock = sendLock.tryLock();
        if (tryLock) {
            try {
                writeFiles(socket, toDevice, fileList, encData);
            } catch (Exception e) {
                logger.error("error: ", e);
            } finally {
                sendLock.unlock();
            }
        } else {
            FileTransfer fileTransfer = new FileTransfer();
            fileTransfer.setFiles(fileList);
            fileTransfer.setFromDevice(fromDevice);
            sendTasks.add(new SendTask(socket, fileTransfer, encData));
        }
    }

    /**
     * 写入文件
     */
    private void writeFiles(Socket socket, Device device, List<MessageFileContent> messageFileContents, boolean encData) {
        try {
            InputStream input = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            DataEnc dataEnc = new DataEnc(sendBuffer);
            for (MessageFileContent fileContent : messageFileContents) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException ignored) {
                }
                dataEnc.reset();
                long totalSend = 0;
                if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                    for (MessageFileContent fileInfo : folderContent.getChildren()) {
                        File file = new File(folderContent.getPath());
                        String relativePath = fileInfo.getPath().replace(file.getParent(), "");
                        dataEnc.reset();
                        dataEnc.putLong(fileInfo.getLength());
                        dataEnc.putString(relativePath);
                        logger.debug("发送文件:" + relativePath + " 大小:" + fileInfo.getLength());
                        try {
                            IOUtil.write(out, dataEnc);
                        } catch (IOException e) {
                            logger.error("error: ", e);
                            break;
                        }
                        InputStream fileIs;
                        try {
                            if (fileInfo instanceof MessageUriContent) {
                                fileIs = FileUtil.getInputStreamFromUri(((MessageUriContent) fileInfo).getUri());
                            } else {
                                fileIs = new FileInputStream(fileInfo.getPath());
                            }
                        } catch (FileNotFoundException e) {
                            logger.error("error: ", e);
                            continue;
                        }
                        long thatSend = 0;
                        if (encData) {
                            thatSend = baseSendEec(fileContent, fileIs, input, out,
                                    dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        } else {
                            thatSend = baseSend(fileContent, fileIs, input, out,
                                    dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
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
                            service.messageSend(mMessage);
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
                            fileIs = new FileInputStream(fileContent.getPath());
                        }
                    } catch (FileNotFoundException e) {
                        logger.error("error: ", e);
                        T.s("发送文件失败，文件：" + fileContent.getContent() + "不存在");
                        continue;
                    }
                    if (encData) {
                        totalSend += baseSendEec(fileContent, fileIs, input, out, dataEnc, 0, 
                                fileContent.getLength(), fileContent.getLength());
                    } else {
                        totalSend += baseSend(fileContent, fileIs, input, out, dataEnc, 0, 
                                fileContent.getLength(), fileContent.getLength());
                    }
                }
                try {
                    input.read();
                } catch (IOException e) {
                    logger.error("error: ", e);
                }
                if (totalSend != fileContent.getLength()) {
                    fileContent.setStatus(MessageContent.ERROR);
                    fileContent.setStateMessage("发送失败");
                } else {
                    fileContent.setStatus(MessageContent.SUCCESS);
                    fileContent.setStateMessage("发送成功");
                }
                LHttpServer.sendMessage(fileContent.getContent(), fileContent.getToUser() + " ← " + deviceManager.getDevName(), 
                        fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, 
                        FileUtil.computeSize(fileContent.getLength()), false, false);
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                mMessage.obj = fileContent;
                service.messageSend(mMessage);
                if (Config.SAVE_MESSAGE) {
                    service.getMesssageDButil().updateMessage(fileContent);
                }
                logger.debug("发送成功:" + totalSend);
            }
        } catch (Exception e) {
            logger.error("error: ", e);
        } finally {
            IOUtil.closeIO(socket);
        }
    }

    /**
     * 基础发送（无加密）
     */
    public long baseSend(MessageFileContent content, InputStream fileIs,
                         InputStream mInput, OutputStream mOut, DataEnc dataEnc,
                         long mTotalSend, long totalLength, long fileLength) {
        int ten;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(LCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        int read = 0;
        try {
            int dataLength = sendBuffer.length - DataEnc.getHeaderSize();
            content.setStatus(MessageContent.IN);
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), dataLength)) != -1) {
                dataEnc.setDataIndex(ten);
                IOUtil.write(mOut, dataEnc);
                read = mInput.read();
                if (read == LCmd.FS_BREAK) {
                    thatSend = 0;
                    T.s("发送文件：" + content.getContent() + " 被中断");
                    break;
                }
                if (!content.isTransfer()) {
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
                    service.messageSend(mMessage);
                    p = progress;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
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
            logger.error("error: ", e);
        }
        return thatSend;
    }

    /**
     * 基础发送（加密）
     */
    public long baseSendEec(MessageFileContent content, InputStream fileIs,
                            InputStream mInput, OutputStream mOut, DataEnc dataEnc,
                            long mTotalSend, long totalLength, long fileLength) {
        int ten;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(LCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        int read = 0;
        try {
            int dataLength = sendBuffer.length - DataEnc.getHeaderSize();
            content.setStatus(MessageContent.IN);
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), dataLength)) != -1) {
                dataEnc.setDataIndex(ten);
                mUtil.encData(sendBuffer, ten, DataEnc.getHeaderSize(), (int) thatSend);
                IOUtil.write(mOut, dataEnc);
                read = mInput.read();
                if (read == LCmd.FS_BREAK) {
                    thatSend = 0;
                    T.s("发送文件：" + content.getContent() + " 被中断");
                    break;
                }
                if (!content.isTransfer()) {
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
                    service.messageSend(mMessage);
                    p = progress;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
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
            logger.error("error: ", e);
        }
        return thatSend;
    }

    /**
     * 同步媒体文件
     */
    public void syncMedia() {
        List<com.fgsqw.lanshare.pojo.FileSyncData> fileSyncData = service.getFileSyncDBUtil().queryList();
        List<Device> deviceList = deviceManager.getDeviceList();
        List<Object[]> toDevice = new ArrayList<>();
        for (FileSyncData fileSyncDatum : fileSyncData) {
            for (Device device : deviceList) {
                if (fileSyncDatum.getDeviceId().equals(device.getUniqueUUid())) {
                    toDevice.add(new Object[]{device, fileSyncDatum});
                }
            }
        }
        if (!toDevice.isEmpty()) {
            for (com.fgsqw.lanshare.pojo.file.PhotoFolder folder : com.fgsqw.lanshare.fragment.data.AnyData.mediaResult.getmFolders()) {
                for (Object[] objects : toDevice) {
                    Device device = (Device) objects[0];
                    FileSyncData syncData = (FileSyncData) objects[1];
                    if (syncData.getFolderPath().equals(folder.getFolderPath())) {
                        List<MessageMediaContent> media = mUtil.deepCopyList(folder.getImages());
                        startSyncingMedias(device, media);
                    }
                }
            }
        }
    }

    /**
     * 开始同步媒体
     */
    public void startSyncingMedias(Device device, List<MessageMediaContent> media) {
        ThreadUtils.runThread(() -> {
            if (device.getDataVersion() >= LVersion.DATA_VERSION_4) {
                fileSend.startSyncingMedias(device, media);
                return;
            }
            Lock lock = StringLockManager.getStringLock(device.getUniqueUUid());
            if (lock.tryLock()) {
                try {
                    Socket socket;
                    Device d;
                    if (device.isIPv4()) {
                        d = deviceManager.getDevice(device);
                    } else {
                        d = deviceManager.makeIPv6Device();
                    }
                    try {
                        socket = service.makeSocket(device);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                        return;
                    }
                    DataEnc dataEnc = deviceManager.makeDataEnc(d, device, 1024 * 1024 * 4);
                    dataEnc.setCmd(LCmd.FS_GET_NO_SYNC_MEDIA);
                    dataEnc.setCount(media.size());
                    for (MessageMediaContent mediaInfo : media) {
                        dataEnc.putLong(mediaInfo.getMediaId());
                    }
                    if (socket == null || !socket.isConnected()) {
                        T.s(String.format(service.getString(R.string.connection_to_device_failed), device.getDevName()));
                        return;
                    }
                    List<MessageFileContent> fileList = new ArrayList<>();
                    InputStream input = null;
                    OutputStream output = null;
                    try {
                        input = socket.getInputStream();
                        output = socket.getOutputStream();
                        TimeUnit.MILLISECONDS.sleep(10);
                        IOUtil.write(output, dataEnc);
                        DataDec dataDec = new DataDec(dataEnc.getData());
                        if (IOUtil.read(input, dataEnc.getData(), 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                            return;
                        int thatLength = dataDec.getLength();
                        if (IOUtil.read(input, dataEnc.getData(), DataEnc.getHeaderSize(), thatLength) != thatLength)
                            return;
                        int count = dataDec.getCount();
                        for (int i = 0; i < count; i++) {
                            long mediaId = dataDec.getLong();
                            MessageMediaContent mediaInfo = com.fgsqw.lanshare.fragment.data.AnyData.mediaResult.getMediaInfoMap().get(mediaId);
                            if (mediaInfo != null) {
                                fileList.add(mediaInfo);
                            }
                            logger.debug("mediaId: " + mediaId);
                        }
                    } catch (Exception e) {
                        logger.error("error: ", e);
                    } finally {
                        IOUtil.closeIO(input, output, socket);
                    }
                    if (fileList.isEmpty()) {
                        return;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    fileSendSync(d, device, fileList);
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    /**
     * 更新应用
     */
    public void updateAppsFromOtherDevice(Device device) {
        ThreadUtils.runThread(() -> {
            try {
                Device fromDevice = deviceManager.getDevice(device);
                List<MessageApkContent> apkFileList = com.fgsqw.lanshare.fragment.data.AnyData.apkFileList;
                if (apkFileList == null || apkFileList.isEmpty()) {
                    T.s("APP列表为空");
                    return;
                }
                com.alibaba.fastjson.JSONArray apkArray = new com.alibaba.fastjson.JSONArray();
                for (MessageApkContent apkInfo : apkFileList) {
                    com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
                    jsonObject.put("packageName", apkInfo.getPackageName());
                    jsonObject.put("versionCode", apkInfo.getVersionCode());
                    jsonObject.put("versionName", apkInfo.getVersionName());
                    apkArray.add(jsonObject);
                }
                byte[] bytes = apkArray.toJSONString().getBytes(FileUtil.UTF_8);
                Socket socket = service.makeSocket(device);
                OutputStream outputStream = socket.getOutputStream();
                DataEnc dataEnc = deviceManager.makeDataEnc(fromDevice, device, bytes.length + 100);
                dataEnc.setCmd(LCmd.FS_UPDATE_APPS);
                IOUtil.write(outputStream, dataEnc);
                dataEnc.reset();
                dataEnc.putBytes(bytes);
                IOUtil.write(outputStream, dataEnc);
                byte[] data = dataEnc.getData();
                InputStream inputStream = socket.getInputStream();
                DataDec dataDec = new DataDec(data);
                try {
                    if (IOUtil.read(inputStream, data, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                        return;
                } catch (IOException e) {
                    logger.error("error: ", e);
                    IOUtil.closeIO(inputStream, socket.getInputStream(), socket);
                    return;
                }
                int length = dataDec.getLength();
                try {
                    if (IOUtil.read(inputStream, data, DataEnc.getHeaderSize(), length) != length)
                        return;
                } catch (IOException e) {
                    logger.error("error: ", e);
                    IOUtil.closeIO(outputStream, inputStream, socket);
                    return;
                }
                IOUtil.closeIO(outputStream, inputStream, socket);
                String string = dataDec.getString();
                apkArray = com.alibaba.fastjson.JSON.parseArray(string);
                if (apkArray.isEmpty()) {
                    T.s("没有在" + device.getDevName() + "中找到更新");
                } else {
                    T.s("在" + device.getDevName() + "中找到" + apkArray.size() + "个更新");
                }
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_UPDATE_APPS;
                mMessage.obj = new Object[]{device, apkArray};
                service.messageSend(mMessage);
            } catch (IOException e) {
                logger.error("updateAppsFromOtherDevice:", e);
            }
        });
    }

    /**
     * 更新应用
     */
    public void updateApp(Device device, com.alibaba.fastjson.JSONArray jsonArray) {
        ThreadUtils.runThread(() -> {
            byte[] bytes = jsonArray.toString().getBytes();
            Socket socket = null;
            try {
                socket = service.makeSocket(device);
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();
                Device fromDevice = deviceManager.getDevice(device);
                DataEnc dataEnc = deviceManager.makeDataEnc(fromDevice, device, bytes.length + 1024);
                dataEnc.setCmd(LCmd.FS_GET_APPS);
                IOUtil.write(outputStream, dataEnc);
                dataEnc.reset();
                dataEnc.putBytes(bytes);
                IOUtil.write(outputStream, dataEnc);
                IOUtil.closeIO(outputStream, inputStream, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
