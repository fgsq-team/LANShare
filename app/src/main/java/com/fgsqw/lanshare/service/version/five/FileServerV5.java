package com.fgsqw.lanshare.service.version.five;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.service.RecvFileCallback;
import com.fgsqw.lanshare.service.version.SyncFeatureHandler;
import com.fgsqw.lanshare.service.version.four.FileTransfer;
import com.fgsqw.lanshare.service.version.four.ProgressCallback;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;
import com.fgsqw.lanshare.utils.mUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * V5 文件服务器(断点续传)
 * <p>V5 拥有独立的协议入口: 发送端使用 {@code NEW_VERSION_5} 魔法数建连,
 * 接收端由 {@link #handleVersion} 自行完成命令分发, 存在版本差异的文件传输处理
 * 自包含于本类, 与 V4 {@code FileServer} 互不掺入; 版本无关的消息接收、媒体同步、远程绘图同步
 * 由 V4/V5 共用扩展 {@link SyncFeatureHandler} 统一实现。</p>
 * <p>续传流程:同意接收后先根据已存在的 {@code .part} 分段计算每个 fileId 的已完成偏移并回传给发送端,
 * 发送端据此跳过已完成字节;接收端将后续字节追加写入分段文件,完成后再落地为最终文件。</p>
 *
 * @author fgsq
 * @version 5.0
 */
public class FileServerV5 {

    private static final Logger logger = LoggerFactory.getLogger(FileServerV5.class);

    private final LANService lanService;

    public FileServerV5(LANService lanService) {
        this.lanService = lanService;
        // 启动时清理过期分段
        ResumeStore.cleanupExpired();
    }

    /**
     * V5 协议入口:读取命令并分发(独立于 V4 的 handleVersion1)
     *
     * @param device       发送方设备
     * @param socket       Socket 连接
     * @param inputStream  输入流
     * @param outputStream 输出流
     */
    public void handleVersion(Device device, Socket socket, CustomDataInputStream inputStream, CustomDataOutputStream outputStream) throws IOException {
        int cmd = inputStream.readInt();
        switch (cmd) {
            case LCmd.FS_SHARE_FILE: // 文件传输(断点续传)
                handleFileShare(device, socket, inputStream, outputStream);
                break;
            case LCmd.FS_MESSAGE: // 接收消息(V4/V5 共用扩展功能)
                SyncFeatureHandler.handleMessage(lanService, device, socket, inputStream);
                break;
            case LCmd.FS_GET_NO_SYNC_MEDIA: // 媒体同步(V4/V5 共用扩展功能)
                SyncFeatureHandler.handleMediaSync(lanService, device, socket, inputStream, outputStream);
                break;
            case LCmd.FS_DRAW_SYNC_REQUEST: // 远程绘图同步请求（长连接，V4/V5 共用扩展功能）
                SyncFeatureHandler.handleDrawSync(lanService, device, socket, inputStream, outputStream);
                break;
        }
    }

    /**
     * V5 文件传输入口:读取 encData 标记与请求 JSON 后进入续传接收流程
     */
    private void handleFileShare(Device fromDevice, Socket socket, CustomDataInputStream inputStream,
                                 CustomDataOutputStream outputStream) throws IOException {
        boolean encData = inputStream.readBoolean();
        String json = inputStream.readString();
        JSONObject data = JSONObject.parseObject(json);
        handleFileTransfer(fromDevice, data, socket, inputStream, outputStream, encData);
    }

    /**
     * 处理 V5 断点续传文件传输请求
     *
     * @param fromDevice   发送方设备
     * @param data         已解析的请求 JSON(含 resume 字段)
     * @param socket       Socket 连接
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @param encData      是否加密数据
     */
    public void handleFileTransfer(Device fromDevice, JSONObject data, Socket socket,
                                   CustomDataInputStream inputStream, CustomDataOutputStream outputStream,
                                   boolean encData) throws IOException {
        JSONArray filesJson = data.getJSONArray("files");
        List<MessageFileContent> files = new ArrayList<>();
        String userName = fromDevice.getDevName();
        for (int i = 0; i < filesJson.size(); i++) {
            JSONObject jsonObject = filesJson.getJSONObject(i);
            int type = jsonObject.getIntValue("fileType");
            MessageFileContent fileContent;
            if (type == MessageFileContent.FILE_TYPE_FOLDER) {
                fileContent = new MessageFolderContent();
                JSONArray children = jsonObject.getJSONArray("children");
                List<MessageFileContent> cd = new ArrayList<>();
                for (int j = 0; j < children.size(); j++) {
                    JSONObject child = children.getJSONObject(j);
                    MessageFileContent childFileContent = new MessageFileContent();
                    childFileContent.setFileId(child.getString("fileId"));
                    childFileContent.setName(child.getString("name"));
                    childFileContent.setLength(child.getLong("length"));
                    childFileContent.setFileType(child.getIntValue("fileType"));
                    cd.add(childFileContent);
                }
                ((MessageFolderContent) fileContent).setChildren(cd);
            } else if (type == MessageFileContent.FILE_TYPE_IMAGE || type == MessageFileContent.FILE_TYPE_VIDEO) {
                MessageMediaContent messageMediaContent = new MessageMediaContent();
                messageMediaContent.setVideo(type == MessageFileContent.FILE_TYPE_VIDEO);
                messageMediaContent.setVideoTime(jsonObject.getString("videoTime"));
                messageMediaContent.setGif(jsonObject.getBooleanValue("gif"));
                fileContent = messageMediaContent;
            } else {
                fileContent = new MessageFileContent();
            }
            fileContent.setLeft(true);
            fileContent.setUserName(userName);
            fileContent.setStatus(MessageContent.IN);
            fileContent.setDevMode(fromDevice.getDevMode());
            fileContent.setFileType(type);
            fileContent.setName(jsonObject.getString("name"));
            fileContent.setLength(jsonObject.getLong("length"));
            fileContent.setFileId(jsonObject.getString("fileId"));
            // V5: 接收端用发送端下发的稳定 fileId 作为记录主键 id, 保证续传复用同一条记录
            String recvFileId = fileContent.getFileId();
            fileContent.setId(StringUtils.isEmpty(recvFileId) ? StringUtils.getUUID() : recvFileId);
            files.add(fileContent);
        }
        FileTransferV5 fileTransfer = new FileTransferV5();
        fileTransfer.setFromDevice(fromDevice);
        fileTransfer.setFiles(files);
        fileTransfer.setResumeEnabled(true);

        RecvFileCallback recvFileCallback = new RecvFileCallback(fileTransfer, files, socket, inputStream, outputStream, encData) {
            @Override
            public void receviceFile(boolean isAgree) {
                ThreadUtils.runThread(() -> {
                    try {
                        outputStream.writeInt(isAgree ? LCmd.FS_AGREE : LCmd.FS_NOT_AGREE);
                        outputStream.flush();
                        if (!isAgree) {
                            return;
                        }
                        // 回传续传偏移量表(fileId -> 已完成字节)
                        String resumeMapJson = buildResumeMapJson(files);
                        outputStream.writeString(resumeMapJson);
                        outputStream.flush();
                        startReceiveFile(fromDevice, fileTransfer, files, socket, inputStream, outputStream, encData);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                    }
                });
            }
        };
        boolean isNotRecvDialog = App.getPrefUtil().getBoolean(PreConfig.NOT_RECV_DIALOG, true);
        if (isNotRecvDialog) {
            recvFileCallback.receviceFile(true);
        } else {
            lanService.sendIfReceiveFilesMsg(recvFileCallback, files.size());
        }
    }

    /**
     * 开始接收文件(逐文件/文件夹)
     */
    public void startReceiveFile(Device fromDevice, FileTransfer fileTransfer, List<MessageFileContent> files,
                                 Socket client, CustomDataInputStream input, CustomDataOutputStream output,
                                 boolean encData) throws IOException {
        progressCallback.onStart(fileTransfer);
        for (MessageFileContent fileContent : files) {
            fileContent.setOutputStream(output);
        }
        for (MessageFileContent fileItem : files) {
            int type = fileItem.getFileType();
            if (type == MessageFileContent.FILE_TYPE_FOLDER) {
                handleFolder(input, fileTransfer, (MessageFolderContent) fileItem, encData);
            } else {
                handleFile(input, fileTransfer, fileItem, encData);
            }
        }
        IOUtil.closeIO(input, output, client);
    }

    // ==================== 单文件 ====================

    private void handleFile(CustomDataInputStream inputStream, FileTransfer fileTransfer,
                            MessageFileContent fileItem, boolean encData) throws IOException {
        int type = fileItem.getFileType();
        String fileId = fileItem.getFileId();
        long length = fileItem.getLength();
        // 不可续传类型:直接落地
        if (!isResumable(type) || StringUtils.isEmpty(fileId)) {
            receiveDirect(inputStream, fileTransfer, fileItem, encData);
            return;
        }
        if (length <= 0) {
            File f = classifiedFile(fileItem);
            f.createNewFile();
            fileItem.setPath(f.getPath());
            progressCallback.onItemFinish(fileTransfer, fileItem, true);
            return;
        }
        File part = ResumeStore.filePart(fileId);
        long offset = ResumeStore.readOffset(part, length);
        long received;
        if (offset >= length) {
            received = length;
        } else {
            OutputStream out = ResumeStore.openAppend(part, offset);
            try {
                if (encData) {
                    received = recvResumeDec(inputStream, fileTransfer, out, length, fileItem, fileItem, offset, offset);
                } else {
                    received = recvResume(inputStream, fileTransfer, out, length, fileItem, fileItem, offset, offset);
                }
            } finally {
                out.flush();
                IOUtil.closeIO(out);
            }
        }
        if (received >= length) {
            File finalFile = ResumeStore.finalizePart(part, classifiedFile(fileItem));
            fileItem.setPath(finalFile.getPath());
            progressCallback.onItemFinish(fileTransfer, fileItem, true);
        } else {
            progressCallback.onItemFinish(fileTransfer, fileItem, false);
        }
    }

    /**
     * 不可续传文件:直接写入最终文件(V4 行为)
     */
    private void receiveDirect(CustomDataInputStream inputStream, FileTransfer fileTransfer,
                               MessageFileContent fileItem, boolean encData) throws IOException {
        File filePath = classifiedFile(fileItem);
        long length = fileItem.getLength();
        long total = 0;
        if (length > 0) {
            OutputStream out = new FileOutputStream(filePath);
            try {
                if (encData) {
                    total = recvResumeDec(inputStream, fileTransfer, out, length, fileItem, fileItem, 0, 0);
                } else {
                    total = recvResume(inputStream, fileTransfer, out, length, fileItem, fileItem, 0, 0);
                }
            } finally {
                out.flush();
                IOUtil.closeIO(out);
            }
            fileItem.setPath(filePath.getPath());
        } else {
            filePath.createNewFile();
            fileItem.setPath(filePath.getPath());
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, length == total);
    }

    // ==================== 文件夹 ====================

    private void handleFolder(CustomDataInputStream inputStream, FileTransfer fileTransfer,
                              MessageFolderContent fileItem, boolean encData) throws IOException {
        String folderFileId = fileItem.getFileId();
        if (StringUtils.isEmpty(folderFileId)) {
            handleFolderDirect(inputStream, fileTransfer, fileItem, encData);
            return;
        }
        long fileSize = fileItem.getLength();
        List<MessageFileContent> children = fileItem.getChildren();
        File stage = ResumeStore.folderStageDir(folderFileId);
        File finalDir = ResumeStore.readTarget(stage);
        if (finalDir == null) {
            String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER);
            finalDir = FileUtil.avoidDuplication(new File(path, fileItem.getName()));
            if (!finalDir.exists()) {
                finalDir.mkdirs();
            }
            ResumeStore.writeTarget(stage, finalDir.getPath());
        }
        long total = 0;
        boolean failed = false;
        for (MessageFileContent child : children) {
            if (child.getLength() <= 0) {
                File ef = new File(finalDir, child.getName());
                File parent = ef.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                if (!ef.exists()) {
                    ef.createNewFile();
                }
                continue;
            }
            File done = new File(finalDir, child.getName());
            File childPart = ResumeStore.childPart(folderFileId, child.getFileId());
            long offset;
            if (done.exists() && done.length() == child.getLength()) {
                offset = child.getLength();
            } else {
                offset = ResumeStore.readOffset(childPart, child.getLength());
            }
            long received;
            if (offset >= child.getLength()) {
                received = child.getLength();
            } else {
                OutputStream out = ResumeStore.openAppend(childPart, offset);
                long progressBase = total + offset;
                try {
                    if (encData) {
                        received = recvResumeDec(inputStream, fileTransfer, out, fileSize, fileItem, child, offset, progressBase);
                    } else {
                        received = recvResume(inputStream, fileTransfer, out, fileSize, fileItem, child, offset, progressBase);
                    }
                } finally {
                    out.flush();
                    IOUtil.closeIO(out);
                }
            }
            if (received < 0) {
                failed = true;
                break;
            }
            if (received >= child.getLength()) {
                ResumeStore.movePartToFinal(childPart, finalDir, child.getName());
            }
            total += received;
        }
        boolean success = !failed && total == fileSize;
        if (success) {
            ResumeStore.deleteDir(stage);
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, success);
    }

    /**
     * 无 fileId 的文件夹:直接落地(V4 行为)
     */
    private void handleFolderDirect(CustomDataInputStream inputStream, FileTransfer fileTransfer,
                                    MessageFolderContent fileItem, boolean encData) throws IOException {
        long fileSize = fileItem.getLength();
        List<MessageFileContent> children = fileItem.getChildren();
        String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER);
        File folderFile = new File(path, fileItem.getName());
        folderFile = FileUtil.avoidDuplication(folderFile);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        long total = 0;
        for (MessageFileContent child : children) {
            File filePath = FileUtil.avoidDuplication(new File(folderFile, child.getName()));
            File parentFile = filePath.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (child.getLength() <= 0) {
                filePath.createNewFile();
                continue;
            }
            long received;
            OutputStream out = new FileOutputStream(filePath);
            try {
                if (encData) {
                    received = recvResumeDec(inputStream, fileTransfer, out, fileSize, fileItem, child, 0, total);
                } else {
                    received = recvResume(inputStream, fileTransfer, out, fileSize, fileItem, child, 0, total);
                }
            } finally {
                out.flush();
                IOUtil.closeIO(out);
            }
            if (received < 0) {
                total = 0;
                break;
            }
            total += received;
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, total == fileSize);
    }

    // ==================== 流接收核心(带续传偏移) ====================

    /**
     * 接收未加密流到输出流
     *
     * @param finalSize    进度分母(单文件为自身长度,文件夹为文件夹总长)
     * @param startOffset  该文件内已完成的绝对字节偏移
     * @param progressBase 进度累加基准
     * @return 成功返回该文件绝对完成字节数(==fileItem.getLength()),失败返回 -1
     */
    private long recvResume(CustomDataInputStream inputStream, FileTransfer fileTransfer, OutputStream out, long finalSize,
                            MessageFileContent baseFileItem, MessageFileContent fileItem,
                            long startOffset, long progressBase) throws IOException {
        long fileIndex = startOffset;
        long sent = 0;
        long targetSize = fileItem.getLength() - startOffset;
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024];
        while (true) {
            int length = inputStream.readInt();
            if (length <= 0) {
                logger.debug("cmd error: {}", length);
                return -1;
            }
            int ten = inputStream.readFully(buffer, 0, length);
            if (ten <= 0) {
                break;
            }
            out.write(buffer, 0, ten);
            fileIndex += ten;
            sent += ten;
            targetSize -= ten;
            progress = (int) ((progressBase + sent) * 100 / finalSize);
            if (progress != lastProgress) {
                lastProgress = progress;
                baseFileItem.setProgress(progress);
                progressCallback.onProgress(fileTransfer, baseFileItem);
            }
            if (targetSize <= 0) {
                break;
            }
        }
        if (fileIndex != fileItem.getLength()) {
            return -1;
        }
        return fileIndex;
    }

    /**
     * 接收加密流到输出流(index 为文件内绝对偏移,与发送端加密对齐)
     */
    private long recvResumeDec(CustomDataInputStream inputStream, FileTransfer fileTransfer, OutputStream out, long finalSize,
                               MessageFileContent baseFileItem, MessageFileContent fileItem,
                               long startOffset, long progressBase) throws IOException {
        long fileIndex = startOffset;
        long sent = 0;
        long targetSize = fileItem.getLength() - startOffset;
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024];
        while (true) {
            int length = inputStream.readInt();
            if (length <= 0) {
                logger.debug("cmd error: {}", length);
                return -1;
            }
            int ten = inputStream.readFully(buffer, 0, length);
            if (ten <= 0) {
                break;
            }
            mUtil.decData(buffer, ten, 0, fileIndex);
            out.write(buffer, 0, ten);
            fileIndex += ten;
            sent += ten;
            targetSize -= ten;
            progress = (int) ((progressBase + sent) * 100 / finalSize);
            if (progress != lastProgress) {
                lastProgress = progress;
                baseFileItem.setProgress(progress);
                progressCallback.onProgress(fileTransfer, baseFileItem);
            }
            if (targetSize <= 0) {
                break;
            }
        }
        if (fileIndex != fileItem.getLength()) {
            return -1;
        }
        return fileIndex;
    }

    // ==================== 偏移量表构建 ====================

    private String buildResumeMapJson(List<MessageFileContent> files) {
        JSONObject map = new JSONObject();
        for (MessageFileContent fileItem : files) {
            int type = fileItem.getFileType();
            String fileId = fileItem.getFileId();
            if (!isResumable(type) || StringUtils.isEmpty(fileId)) {
                continue;
            }
            if (type == MessageFileContent.FILE_TYPE_FOLDER) {
                File stage = new File(ResumeStore.partRoot(), fileId);
                if (!stage.exists()) {
                    continue;
                }
                File finalDir = ResumeStore.readTarget(stage);
                List<MessageFileContent> children = ((MessageFolderContent) fileItem).getChildren();
                if (children == null) {
                    continue;
                }
                for (MessageFileContent child : children) {
                    if (child.getLength() <= 0 || StringUtils.isEmpty(child.getFileId())) {
                        continue;
                    }
                    long off;
                    File done = finalDir == null ? null : new File(finalDir, child.getName());
                    if (done != null && done.exists() && done.length() == child.getLength()) {
                        off = child.getLength();
                    } else {
                        off = ResumeStore.readOffset(ResumeStore.childPart(fileId, child.getFileId()), child.getLength());
                    }
                    if (off > 0) {
                        map.put(child.getFileId(), off);
                    }
                }
            } else {
                long length = fileItem.getLength();
                if (length <= 0) {
                    continue;
                }
                long off = ResumeStore.readOffset(ResumeStore.filePart(fileId), length);
                if (off > 0) {
                    map.put(fileId, off);
                }
            }
        }
        return map.toJSONString();
    }

    // ==================== 进度回调 ====================

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
                fileItem.setStateMessage("接收成功");
                if (fileItem.getFileType() == MessageFileContent.FILE_TYPE_VIDEO
                        || fileItem.getFileType() == MessageFileContent.FILE_TYPE_IMAGE) {
                    Long mediaId = ((MessageMediaContent) fileItem).getMediaId();
                    if (mediaId > -1) {
                        lanService.getMediaIdPathDBUtil().addMediaIdPath(mediaId, fileItem.getContent(), fileItem.getPath(), new Date(), true);
                    }
                }
            } else {
                fileItem.setStatus(MessageContent.ERROR);
                fileItem.setStateMessage("接收失败");
            }
            lanService.sendCloseProgressMeg(fileItem);
        }
    };

    // ==================== 辅助方法 ====================

    private File classifiedFile(MessageFileContent fileItem) {
        String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileItem.getName()));
        File folderFile = new File(path);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        return FileUtil.avoidDuplication(new File(folderFile, fileItem.getName()));
    }

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
}
