package com.fgsqw.lanshare.service.version.five;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.SendTask;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.pojo.message.MessageStreamContent;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.service.version.four.FileTransfer;
import com.fgsqw.lanshare.service.version.four.ProgressCallback;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.DeviceDataScanner;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.utils.ParameterizedTaskQueue;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;
import com.fgsqw.lanshare.utils.mUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * V5 文件发送器
 * 增加断点传输
 * @author fgsq
 * @version 5.0
 */
public class FileSenderV5 implements ParameterizedTaskQueue.TaskProcessor<SendTask> {

    private static final Logger logger = LoggerFactory.getLogger(FileSenderV5.class);

    private final LANService lanService;

    private final ParameterizedTaskQueue<SendTask> taskQueue = new ParameterizedTaskQueue<>(this);

    public FileSenderV5(LANService lanService) {
        this.lanService = lanService;
        ThreadUtils.runThread(taskQueue::consumeTasks);
    }

    /**
     * 发送文件到指定设备(V5,支持断点续传)
     */
    public void send(Device fromDevice, Device toDevice, Socket socket, CustomDataInputStream inputStream,
                     CustomDataOutputStream outputStream, List<MessageFileContent> fileList) {
        try {
            String senderUuid = fromDevice.getUniqueUUid();
            sendNewVersionFlag(fromDevice, outputStream);
            String userName = fromDevice.getDevName();
            JSONArray jsonArray = new JSONArray();
            for (MessageFileContent fileContent : fileList) {
                JSONObject jsonObject = new JSONObject();
                if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    List<MessageFileContent> fileInfos = new LinkedList<>();
                    File file = new File(fileContent.getPath());
                    fileContent.setLength(0);
                    DeviceDataScanner.buildFileItemTree(file, "", (MessageFolderContent) fileContent, fileInfos);
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                    folderContent.setFileCount(fileInfos.size());
                    folderContent.setChildren(fileInfos);
                    JSONArray children = new JSONArray();
                    for (MessageFileContent fileInfo : fileInfos) {
                        if (fileInfo.getLength() > 0 && !StringUtils.isEmpty(fileInfo.getPath())) {
                            fileInfo.setFileId(ResumeStore.stableId(senderUuid + "|" + fileInfo.getPath() + "|" + fileInfo.getLength()));
                        } else if (StringUtils.isEmpty(fileInfo.getFileId())) {
                            fileInfo.setFileId(StringUtils.getUUID());
                        }
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

                // 顶层文件 fileId: 每次新发送生成新的随机 UUID, 避免同一文件重复发送被并入之前的记录导致进度串台;
                // 仅当记录本身已携带 fileId(在列表里对该记录重新点击发送=续传)时才复用原来的 uuid。
                if (StringUtils.isEmpty(fileContent.getFileId())) {
                    fileContent.setFileId(StringUtils.getUUID());
                }
                // 记录主键 id = fileId, 与接收端/数据库/进度匹配保持一致
                fileContent.setId(fileContent.getFileId());
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

            FileTransferV5 fileTransfer = new FileTransferV5();
            fileTransfer.setFromDevice(fromDevice);
            fileTransfer.setFiles(fileList);
            fileTransfer.setResumeEnabled(true);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fromDevice", fromDevice.toJsonObject());
            jsonObject.put("type", 0);
            jsonObject.put("groupId", 0);
            jsonObject.put("files", jsonArray);
            boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
            outputStream.writeInt(LCmd.FS_SHARE_FILE);
            outputStream.writeBoolean(encData);
            outputStream.writeString(jsonObject.toJSONString());
            outputStream.flush();

            int response = inputStream.readInt();
            if (response != LCmd.FS_AGREE) {
                LLog.debug("对方拒绝接收文件");
                return;
            }

            fileTransfer.setResumeMap(readResumeMap(inputStream));

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
     * 处理文件传输任务
     */
    public void handleFileTransfer(FileTransfer fileTransfer, Socket socket, boolean encData) throws IOException {
        CustomDataInputStream inputStream = new CustomDataInputStream(socket.getInputStream());
        CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
        startCancelFileReceiveListener(inputStream, fileTransfer);
        Map<String, Long> resumeMap = fileTransfer instanceof FileTransferV5
                ? fileTransferV5(fileTransfer).getResumeMap() : new HashMap<>();
        for (MessageFileContent fileItem : fileTransfer.getFiles()) {
            LLog.debug("发送文件:" + fileItem.getName());
            int type = fileItem.getFileType();
            if (type == MessageFileContent.FILE_TYPE_FOLDER) {
                sendFolder(fileTransfer, outputStream, (MessageFolderContent) fileItem, encData, resumeMap);
            } else {
                sendFile(fileTransfer, outputStream, fileItem, encData, resumeMap);
            }
        }
    }

    /**
     * 发送文件夹(支持逐子文件续传)
     */
    private void sendFolder(FileTransfer fileTransfer, CustomDataOutputStream outputStream,
                            MessageFolderContent fileItem, boolean encData, Map<String, Long> resumeMap) throws IOException {
        long fileSize = fileItem.getLength();
        List<MessageFileContent> children = fileItem.getChildren();
        long subTotal = 0;
        for (MessageFileContent child : children) {
            if (child.getLength() <= 0) {
                continue;
            }
            long childOffset = clamp(resumeOffset(resumeMap, child.getFileId()), child.getLength());
            if (childOffset >= child.getLength()) {
                // 该子文件接收端已完整持有,跳过传输
                subTotal += child.getLength();
                continue;
            }
            InputStream inputStream = new FileInputStream(child.getPath());
            long ten;
            try {
                ResumeStore.skipFully(inputStream, childOffset);
                long progressBase = subTotal + childOffset;
                if (encData) {
                    ten = sendFileStreamEnc(fileTransfer, progressBase, fileSize, fileItem, child, outputStream, inputStream, childOffset);
                } else {
                    ten = sendFileStream(fileTransfer, progressBase, fileSize, fileItem, child, outputStream, inputStream, childOffset);
                }
            } finally {
                IOUtil.closeIO(inputStream);
            }
            if (ten < 0) {
                break;
            }
            subTotal += ten;
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, subTotal == fileSize);
    }

    /**
     * 发送单个文件(支持续传)
     */
    private void sendFile(FileTransfer fileTransfer, CustomDataOutputStream outputStream,
                          MessageFileContent fileItem, boolean encData, Map<String, Long> resumeMap) throws IOException {
        long length = fileItem.getLength();
        long total = 0;
        if (length > 0) {
            long startOffset = clamp(resumeOffset(resumeMap, fileItem.getFileId()), length);
            if (startOffset >= length) {
                total = length;
            } else {
                InputStream inputStream = null;
                try {
                    if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_STREAM) {
                        MessageStreamContent streamContent = (MessageStreamContent) fileItem;
                        inputStream = streamContent.getInputStream();
                    } else {
                        inputStream = new FileInputStream(fileItem.getPath());
                    }
                    ResumeStore.skipFully(inputStream, startOffset);
                    if (encData) {
                        total = sendFileStreamEnc(fileTransfer, startOffset, length, fileItem, fileItem, outputStream, inputStream, startOffset);
                    } else {
                        total = sendFileStream(fileTransfer, startOffset, length, fileItem, fileItem, outputStream, inputStream, startOffset);
                    }
                } finally {
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
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, length == total);
    }

    /**
     * 发送文件流(未加密,带续传起点)
     *
     * @param progressBase 进度基准(文件夹为已累加字节数,单文件为续传偏移)
     * @param folderSize   进度分母(单文件为自身长度,文件夹为文件夹总长)
     * @param startOffset  该文件内已完成的绝对字节偏移
     * @return 成功返回该文件的绝对完成字节数(==length),失败返回 -1
     */
    private long sendFileStream(FileTransfer fileTransfer, long progressBase, long folderSize,
                                MessageFileContent baseFileItem, MessageFileContent fileItem,
                                CustomDataOutputStream outputStream, InputStream inputStream, long startOffset) {
        int len;
        long fileIndex = startOffset;
        long sent = 0;
        long targetSize = fileItem.getLength() - startOffset;
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024];
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
                outputStream.write(buffer, 0, len);
                outputStream.flush();
                fileIndex += len;
                sent += len;
                targetSize -= len;
                // 全 long 运算, 避免大文件 (progressBase+sent)*100 溢出 int 导致进度卡在 0
                progress = (int) ((progressBase + sent) * 100 / folderSize);
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
        if (fileIndex != fileItem.getLength()) {
            return -1;
        }
        return fileIndex;
    }

    /**
     * 发送文件流(加密,带续传起点)
     */
    private long sendFileStreamEnc(FileTransfer fileTransfer, long progressBase, long folderSize,
                                   MessageFileContent baseFileItem, MessageFileContent fileItem,
                                   CustomDataOutputStream outputStream, InputStream inputStream, long startOffset) {
        int len;
        long fileIndex = startOffset;
        long sent = 0;
        long targetSize = fileItem.getLength() - startOffset;
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024];
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
                // 加密:index 为文件内绝对字节偏移,与接收端解密对齐
                mUtil.encData(buffer, len, 0, fileIndex);
                outputStream.writeInt(len);
                outputStream.write(buffer, 0, len);
                outputStream.flush();
                fileIndex += len;
                sent += len;
                targetSize -= len;
                // 全 long 运算, 避免大文件 (progressBase+sent)*100 溢出 int 导致进度卡在 0
                progress = (int) ((progressBase + sent) * 100 / folderSize);
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
        if (fileIndex != fileItem.getLength()) {
            return -1;
        }
        return fileIndex;
    }

    /** 进度回调 */
    public ProgressCallback progressCallback = new ProgressCallback() {
        @Override
        public void onStart(FileTransfer fileTransfer) {
            lanService.sendShowProgressMeg(fileTransfer.getFiles());
        }

        @Override
        public void onProgress(FileTransfer fileTransfer, MessageFileContent fileItem) {
            fileItem.setStatus(MessageContent.IN);
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

    /**
     * 启动取消文件接收监听线程
     */
    private void startCancelFileReceiveListener(CustomDataInputStream inputStream, FileTransfer fileTransfer) {
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
    }

    /**
     * 发送 V5 协议头(独立魔法数 NEW_VERSION_5, 接收端直连 FileServerV5 处理链)
     */
    public void sendNewVersionFlag(Device fromDevice, CustomDataOutputStream outputStream) throws IOException {
        outputStream.writeInt(LCmd.NEW_VERSION_5);
        outputStream.writeString(fromDevice.toJsonObject().toJSONString());
        outputStream.flush();
    }

    // ==================== 辅助方法 ====================

    /**
     * 是否为可续传的文件类型(需本地路径的常规文件类型)
     */
    private static boolean isResumable(int fileType) {
        switch (fileType) {
            case MessageFileContent.FILE_TYPE_IMAGE:
            case MessageFileContent.FILE_TYPE_VIDEO:
            case MessageFileContent.FILE_TYPE_AUDIO:
            case MessageFileContent.FILE_TYPE_APK:
            case MessageFileContent.FILE_TYPE_FILE:
            case MessageFileContent.FILE_TYPE_FOLDER:
                return true;
            default:
                return false;
        }
    }

    private static long resumeOffset(Map<String, Long> resumeMap, String fileId) {
        if (resumeMap == null || fileId == null) {
            return 0;
        }
        Long offset = resumeMap.get(fileId);
        return offset == null ? 0 : offset;
    }

    private static long clamp(long offset, long length) {
        if (offset < 0) {
            return 0;
        }
        return Math.min(offset, length);
    }

    private static FileTransferV5 fileTransferV5(FileTransfer fileTransfer) {
        return (FileTransferV5) fileTransfer;
    }

    private Map<String, Long> readResumeMap(CustomDataInputStream inputStream) throws IOException {
        Map<String, Long> map = new HashMap<>();
        String json = inputStream.readString();
        if (StringUtils.isEmpty(json)) {
            return map;
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            for (String key : obj.keySet()) {
                map.put(key, obj.getLongValue(key));
            }
        } catch (Exception e) {
            logger.error("readResumeMap error", e);
        }
        return map;
    }
}
