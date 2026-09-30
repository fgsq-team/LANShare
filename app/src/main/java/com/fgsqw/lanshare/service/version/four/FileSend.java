package com.fgsqw.lanshare.service.version.four;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.SendTask;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.pojo.message.MessageStreamContent;
import com.fgsqw.lanshare.pojo.message.MessageUriContent;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.FileSearchUtils;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.utils.ParameterizedTaskQueue;
import com.fgsqw.lanshare.utils.StringLockManager;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;
import com.fgsqw.lanshare.utils.mUtil;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class FileSend implements ParameterizedTaskQueue.TaskProcessor<SendTask> {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(FileSend.class);

    private final LANService lanService;
    private final ParameterizedTaskQueue<SendTask> taskQueue = new ParameterizedTaskQueue<>(this);

    public FileSend(LANService lanService) {
        this.lanService = lanService;
        ThreadUtils.runThread(taskQueue::consumeTasks);
    }

    public void startSyncingMedias(Device device, List<MessageMediaContent> mediaList) {
        Lock lock = StringLockManager.getStringLock(device.getUniqueUUid());
        if (lock.tryLock()) {
            try {
                Socket socket;
                Device d;
                if (device.isIPv4()) {
                    d = lanService.getDevice(device);
                } else {
                    d = lanService.makeIPv6Device();
                }
                try {
                    socket = LANService.getInstance().makeSocket(device);
                } catch (IOException e) {
                    logger.error("error: ", e);
                    return;
                }
                if (socket == null || !socket.isConnected()) {
                    T.s(String.format(lanService.getString(R.string.connection_to_device_failed), device.getDevName()));
                    return;
                }
                List<MessageFileContent> fileList = new ArrayList<>();
                InputStream input = null;
                OutputStream output = null;
                try {
                    CustomDataInputStream inputStream = new CustomDataInputStream(socket.getInputStream());
                    CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
                    sendNewVersionFlag(device, outputStream);
                    outputStream.writeInt(LCmd.FS_GET_NO_SYNC_MEDIA);
                    JSONArray mediaSyncJson = new JSONArray();
                    for (MessageMediaContent mediaInfo : mediaList) {
                        mediaSyncJson.add(mediaInfo.getMediaId());
                    }
                    // 发送媒体id
                    outputStream.writeString(mediaSyncJson.toJSONString());
                    outputStream.flush();
                    // 接收媒体id
                    String waitSyncMediaJsonArray = inputStream.readString();
                    JSONArray waitSyncMediaJsonArrayData = JSON.parseArray(waitSyncMediaJsonArray);
                    for (int i = 0; i < waitSyncMediaJsonArrayData.size(); i++) {
                        long mediaId = waitSyncMediaJsonArrayData.getLong(i);
                        MessageMediaContent mediaInfo = AnyData.mediaResult.getMediaInfoMap().get(mediaId);
                        if (mediaInfo != null) {
                            fileList.add(mediaInfo);
                        }
                        logger.debug("mediaId: {}", mediaId);
                    }
                } catch (Exception e) {
                    logger.error("error: ", e);
                    T.s(R.string.connection_to_device_failed, device.getDevName());
                } finally {
                    IOUtil.closeIO(input, output, socket);
                }
                if (fileList.isEmpty()) {
                    T.s(R.string.no_new_media_to_sync);
                    return;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ignored) {
                }
                try {
                    socket = lanService.createSocket(device);
                    input = socket.getInputStream();
                    output = socket.getOutputStream();
                    send(d, device, socket, new CustomDataInputStream(input), new CustomDataOutputStream(output), fileList);
                } catch (Exception e) {
                    logger.error("error: ", e);
                    T.s(R.string.connection_to_device_failed, device.getDevName());
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void sendMessage(Device device, String message, boolean isClip) {
        Socket socket = null;
        try {
            socket = lanService.createSocket(device);
            CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
            sendNewVersionFlag(device, outputStream);
            outputStream.writeInt(LCmd.FS_MESSAGE);
            outputStream.writeBoolean(isClip);
            outputStream.writeString(message);
            outputStream.flush();
        } catch (Exception e) {
            logger.error("error: ", e);
            T.s(R.string.connection_to_device_failed, device.getDevName());
        }
        IOUtil.closeIO(socket);
    }

    public void send(Device fromDevice, Device toDevice, Socket socket, CustomDataInputStream inputStream, CustomDataOutputStream outputStream, List<MessageFileContent> fileList) {
        try {
            sendNewVersionFlag(fromDevice, outputStream);
            String userName = fromDevice.getDevName();
            JSONArray jsonArray = new JSONArray();
            for (MessageFileContent fileContent : fileList) {
                JSONObject jsonObject = new JSONObject();
                if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    List<MessageFileContent> fileInfos = new LinkedList<>();
                    File file = new File(fileContent.getPath());
                    // 扫描文件并返回扫描到的文件总大小
                    fileContent.setLength(0);
                    FileSearchUtils.createFileItem(file, "", (MessageFolderContent) fileContent, fileInfos);
                    // 创建Message实体类
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                    folderContent.setFileCount(fileInfos.size());
                    folderContent.setChildren(fileInfos);
                    JSONArray children = new JSONArray();
                    for (MessageFileContent fileInfo : fileInfos) {
                        JSONObject child = new JSONObject();
                        child.put("fileId", fileInfo.getFileId());
                        child.put("name", fileInfo.getName());
                        child.put("length", fileInfo.getLength());
                        child.put("fileType", fileInfo.getFileType());
                        children.add(child);
                    }
                    jsonObject.put("fileCount", fileInfos.size());
                    jsonObject.put("children", children);
                } else if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_IMAGE
                        || fileContent.getFileType() == MessageFileContent.FILE_TYPE_VIDEO) {
                    MessageMediaContent mediaInfo = (MessageMediaContent) fileContent;
                    jsonObject.put("video", mediaInfo.isVideo());
                    jsonObject.put("videoTime", mediaInfo.getVideoTime());
                    jsonObject.put("gif", mediaInfo.isGif());
                }
                fileContent.setFileId(StringUtils.getUUID());
                fileContent.setId(StringUtils.getUUID());
                fileContent.setLeft(false);
                fileContent.setUserName(userName);
                fileContent.setToUser(toDevice.getDevName());
                jsonObject.put("fileId", fileContent.getFileId());
                jsonObject.put("name", fileContent.getName());
                jsonObject.put("length", fileContent.getLength());
                jsonObject.put("fileType", fileContent.getFileType());
                jsonObject.put("toUser", toDevice.getDevName());
                jsonArray.add(jsonObject);
            }
            // 发送命令类型
            // 构造文件传输对象
            FileTransfer fileTransfer = new FileTransfer();
            fileTransfer.setFromDevice(fromDevice);
            fileTransfer.setFiles(fileList);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fromDevice", fromDevice.toJsonObject());
            jsonObject.put("type", 0);
            jsonObject.put("groupId", 0);
            jsonObject.put("files", jsonArray);
            // 是否加密数据
            boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
            outputStream.writeInt(LCmd.FS_SHARE_FILE);
            outputStream.writeBoolean(encData); // 加密文件
            // 发送文件传输对象
            outputStream.writeString(jsonObject.toJSONString());
            outputStream.flush();

            int response = inputStream.readInt();
            if (response != LCmd.FS_AGREE) {
                LLog.debug("对方拒绝接收文件");
                return;
            }

            progressCallback.onStart(fileTransfer);
            SendTask sendTask = new SendTask(socket, fileTransfer, encData);
            taskQueue.addTask(sendTask);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(SendTask taskParameter) {
        try {
            handleFileTransfer(taskParameter.getFileTransfer(), taskParameter.getSocket(), taskParameter.isEncData());
        } catch (Exception e) {
            logger.error("error: ", e);
            T.s(R.string.file_send_failed);
        }
        IOUtil.closeIO(taskParameter.getSocket());
    }


    /**
     * 处理文件传输
     */
    public void handleFileTransfer(FileTransfer fileTransfer, Socket socket, boolean encData) throws IOException {
        CustomDataInputStream inputStream = new CustomDataInputStream(socket.getInputStream());
        CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
        // 取消接收文件指令接收线程
        ThreadUtils.runThread(() -> {
            int countFlag = 0;
            while (true) {
                try {
                    String fileId = inputStream.readString();
                    if (fileId == null) {
                        return;
                    }
                    if (StringUtils.isEmpty(fileId)) {
                        continue;
                    }
                    for (MessageFileContent item : fileTransfer.getFiles()) {
                        if (fileId.equals(item.getFileId())) {
                            item.setTransfer(false);
                            break;
                        }
                    }
                } catch (IOException e) {
                    LLog.error("error: ", e);
                    return;
                } catch (Exception e) {
                    LLog.error("error: ", e);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    countFlag++;
                }
                if (countFlag > 10) {
                    return;
                }
            }
        });
        // 处理每个文件项
        for (MessageFileContent fileItem : fileTransfer.getFiles()) {
            LLog.debug("发送文件:" + fileItem.getName());
            int type = fileItem.getFileType();
            if (type == MessageFileContent.FILE_TYPE_FOLDER) {
                sendFolder(fileTransfer, outputStream, (MessageFolderContent) fileItem, encData);
            } else {
                sendFile(fileTransfer, outputStream, fileItem, encData);
            }
        }
    }

    /**
     * 发送文件夹
     */
    public void sendFolder(FileTransfer fileTransfer, CustomDataOutputStream outputStream, MessageFolderContent fileItem, boolean encData) throws IOException {
        long fileSize = fileItem.getLength();
        List<MessageFileContent> children = fileItem.getChildren();
        long subTotal = 0;
        for (MessageFileContent child : children) {
            if (child.getLength() <= 0) {
                continue;
            }
            InputStream inputStream;
            if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_STREAM) {
                MessageStreamContent streamContent = (MessageStreamContent) child;
                inputStream = streamContent.getInputStream();
            } else if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_URI) {
                MessageUriContent uriContent = (MessageUriContent) child;
                inputStream = FileUtil.getInputStreamFromUri(uriContent.getUri());
            } else {
                inputStream = new FileInputStream(child.getPath());
            }
            long ten;
            if (encData) {
                ten = sendFileStreamEnc(fileTransfer, subTotal, fileSize, fileItem, child, outputStream, inputStream);
            } else {
                ten = sendFileStream(fileTransfer, subTotal, fileSize, fileItem, child, outputStream, inputStream);
            }
            IOUtil.closeIO(inputStream);
            if (ten < 0) {
                break;
            }
            subTotal += ten;
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, subTotal == fileSize);
    }

    /**
     * 发送文件
     */
    private void sendFile(FileTransfer fileTransfer, CustomDataOutputStream outputStream, MessageFileContent fileItem, boolean encData) throws IOException {
        // 服务端同意接收文件，开始发送文件内容
        String filePath = fileItem.getPath();
        long total = 0;
        if (fileItem.getLength() > 0) {
            InputStream inputStream = null;
            try {
                if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_STREAM) {
                    MessageStreamContent streamContent = (MessageStreamContent) fileItem;
                    inputStream = streamContent.getInputStream();
                } else if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_URI) {
                    MessageUriContent uriContent = (MessageUriContent) fileItem;
                    inputStream = FileUtil.getInputStreamFromUri(uriContent.getUri());
                } else {
                    inputStream = new FileInputStream(filePath);
                }
                if (encData) {
                    total = sendFileStreamEnc(fileTransfer, 0, fileItem.getLength(), fileItem, fileItem, outputStream, inputStream);
                } else {
                    total = sendFileStream(fileTransfer, 0, fileItem.getLength(), fileItem, fileItem, outputStream, inputStream);
                }
            } finally {
                // 自定义流特殊处理不要关闭
                if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_STREAM) {
                    MessageStreamContent streamContent = (MessageStreamContent) fileItem;
                    Semaphore semaphore = streamContent.getSemaphore();
                    if (semaphore != null) {
                        semaphore.release();
                    }
                } else {
                    IOUtil.closeIO(inputStream);
                }
            }

        }
        progressCallback.onItemFinish(fileTransfer, fileItem, fileItem.getLength() == total);

    }

    /**
     * 发送文件流
     */
    private long sendFileStream(FileTransfer fileTransfer, long total, long folderSize, MessageFileContent baseFileItem, MessageFileContent fileItem, CustomDataOutputStream outputStream, InputStream inputStream) {
        int len;
        long subTotal = 0;
        long targetSize = fileItem.getLength();
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
        try {
            while (true) {
                if (!baseFileItem.isTransfer()) {
                    outputStream.writeInt(LCmd.NEW_FS_BREAK);
                    return -1;
                }
                len = inputStream.read(buffer);
                if (len <= 0) {
                    break;
                }
                outputStream.writeInt(len);
                // 发送数据
                outputStream.write(buffer, 0, len);
                outputStream.flush();
                subTotal += len;
                total += len;
                targetSize -= len;
                progress = (int) (total * 100 / folderSize);
                if (progress > lastProgress) {
                    lastProgress = progress;
                    if (progressCallback != null) {
                        baseFileItem.setProgress(progress);
                        progressCallback.onProgress(fileTransfer, baseFileItem);
                    }
                }
                if (targetSize <= 0) {
                    break;
                }
            }
        } catch (Exception e) {
            LLog.error("异常 ", e);
            return -1;
        }
        if (subTotal != fileItem.getLength()) {
            subTotal = -1;
        }
        return subTotal;
    }

    /**
     * 发送文件流(加密)
     */
    private long sendFileStreamEnc(FileTransfer fileTransfer, long total, long folderSize, MessageFileContent baseFileItem, MessageFileContent fileItem, CustomDataOutputStream outputStream, InputStream inputStream) {
        int len;
        long subTotal = 0;
        long targetSize = fileItem.getLength();
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
        try {
            while (true) {
                if (!baseFileItem.isTransfer()) {
                    outputStream.writeInt(LCmd.NEW_FS_BREAK);
                    return -1;
                }
                len = inputStream.read(buffer);
                if (len <= 0) {
                    break;
                }
                // 加密
                mUtil.encData(buffer, len, 0, subTotal);
                outputStream.writeInt(len);
                // 发送数据
                outputStream.write(buffer, 0, len);
                outputStream.flush();
                subTotal += len;
                total += len;
                targetSize -= len;
                progress = (int) (total * 100 / folderSize);
                if (progress > lastProgress) {
                    lastProgress = progress;
                    if (progressCallback != null) {
                        baseFileItem.setProgress(progress);
                        progressCallback.onProgress(fileTransfer, baseFileItem);
                    }
                }
                if (targetSize <= 0) {
                    break;
                }
            }
        } catch (Exception e) {
            LLog.error("异常 ", e);
            return -1;
        }
        if (subTotal != fileItem.getLength()) {
            subTotal = -1;
        }
        return subTotal;
    }

    public ProgressCallback progressCallback = new ProgressCallback() {
        @Override
        public void onStart(FileTransfer fileTransfer) {
            lanService.sendShowProgressMeg(fileTransfer.getFiles());
        }

        @Override
        public void onProgress(FileTransfer fileTransfer, MessageFileContent fileItem) {
//            LLog.debug("progress: " + progress);
            lanService.sendProgressMeg(fileItem);
        }

        @Override
        public void onItemFinish(FileTransfer fileTransfer, MessageFileContent fileItem, boolean isSuccess) {
            if (isSuccess) {
                fileItem.setStatus(MessageContent.SUCCESS);
                fileItem.setStateMessage("发送成功");
            } else {
                fileItem.setStatus(MessageContent.ERROR);
                fileItem.setStateMessage("发送失败");
            }
            lanService.sendCloseProgressMeg(fileItem);
        }
    };

    public void sendNewVersionFlag(Device fromDevice, CustomDataOutputStream outputStream) throws IOException {
        outputStream.writeInt(LCmd.NEW_VERSION_4);
        // 发送设备信息
        outputStream.writeString(fromDevice.toJsonObject().toJSONString());
        outputStream.flush();
    }

}
