package com.fgsqw.lanshare.service.version.four;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
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
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.AESUtils;
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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件服务器
 * <p>负责接收其他设备发送的文件、消息和媒体同步请求</p>
 * <p>支持 V4 版本的传输协议,提供加密和未加密两种传输模式</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class FileServer {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(FileServer.class);

    /** LAN 服务实例 */
    private final LANService lanService;

    /**
     * 构造函数
     *
     * @param lanService LAN 服务实例
     */
    public FileServer(LANService lanService) {
        this.lanService = lanService;
    }

    /**
     * 处理版本 1 协议请求
     *
     * @param device       发送方设备
     * @param socket       Socket 连接
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException            如果发生 I/O 错误
     * @throws ClassNotFoundException 如果类未找到
     */
    public void handleVersion1(Device device, Socket socket, CustomDataInputStream inputStream, CustomDataOutputStream outputStream) throws IOException, ClassNotFoundException {
        int cmd = inputStream.readInt();
        switch (cmd) {
            case LCmd.FS_SHARE_FILE: // 文件传输
                handleFileTransfer(device, socket, inputStream, outputStream);
                break;
            case LCmd.FS_MESSAGE: // 接收消息
                handleMessage(device, socket, inputStream);
                break;
            case LCmd.FS_GET_NO_SYNC_MEDIA: // 媒体同步
                handleMediaSync(device, socket, inputStream, outputStream);
                break;
        }
    }

    /**
     * 处理消息接收
     *
     * @param device      发送方设备
     * @param socket      Socket 连接
     * @param inputStream 输入流
     * @throws IOException 如果发生 I/O 错误
     */
    public void handleMessage(Device device, Socket socket, CustomDataInputStream inputStream) throws IOException {
        // 是否写入剪切板
        boolean isClip = inputStream.readBoolean();
        String messageEnc = inputStream.readString();
        logger.debug("tcp message: {}", messageEnc);
        try {
            String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
            MessageContent content = new MessageContent();
            content.setId(StringUtils.getUUID());
            content.setStatus(MessageContent.SUCCESS);
            content.setUserName(device.getDevName());
            content.setContent(message);
            content.setLeft(true);
            if (isClip) {
                // 写入剪贴板
                ClipboardManager cb = (ClipboardManager) lanService.getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("text", message));
                T.s((R.string.copy_text_to_clipboard_successful));
            }
            lanService.addMessage(content);
        } catch (Exception e) {
            logger.error("message decrypt error:", e);
            T.s((R.string.message_decryption_failed));
        } finally {
            IOUtil.closeIO(socket);
        }
    }

    /**
     * 处理媒体同步请求
     *
     * @param device       发送方设备
     * @param socket       Socket 连接
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public void handleMediaSync(Device device, Socket socket, CustomDataInputStream inputStream, CustomDataOutputStream outputStream) throws IOException {
        String mediaSyncJson = inputStream.readString();
        JSONArray mediaIdArray = JSON.parseArray(mediaSyncJson);
        JSONArray resultArray = new JSONArray();
        for (int i = 0; i < mediaIdArray.size(); i++) {
            long mediaId = mediaIdArray.getLong(i);
            if (!lanService.getMediaIdPathDBUtil().isIdExists(mediaId)) {
                resultArray.add(mediaId);
            }
        }
        outputStream.writeString(resultArray.toJSONString());
        outputStream.flush();
        IOUtil.closeIO(socket);
    }

    /**
     * 处理文件传输请求
     *
     * @param fromDevice   发送方设备
     * @param socket       Socket 连接
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public void handleFileTransfer(Device fromDevice, Socket socket, CustomDataInputStream inputStream, CustomDataOutputStream outputStream) throws IOException {
        boolean encData = inputStream.readBoolean();
        String json = inputStream.readString();
        JSONObject data = JSONObject.parseObject(json);
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
            fileContent.setId(StringUtils.getUUID());
            fileContent.setLeft(true);
            fileContent.setUserName(userName);
            fileContent.setStatus(MessageContent.IN);
            fileContent.setDevMode(fromDevice.getDevMode());
            fileContent.setFileType(type);
            fileContent.setName(jsonObject.getString("name"));
            fileContent.setLength(jsonObject.getLong("length"));
            fileContent.setFileId(jsonObject.getString("fileId"));
            files.add(fileContent);
        }
        data.remove("files");
        FileTransfer fileTransfer = new FileTransfer();
        fileTransfer.setFromDevice(fromDevice);
        fileTransfer.setFiles(files);
        RecvFileCallback recvFileCallback = new RecvFileCallback(fileTransfer, files, socket, inputStream, outputStream, encData) {
            @Override
            public void receviceFile(boolean isAgree) {
                ThreadUtils.runThread(() -> {
                    try {
                        // 处理是否接收逻辑,默认接收
                        outputStream.writeInt(isAgree ? LCmd.FS_AGREE : LCmd.FS_NOT_AGREE);
                        outputStream.flush();
                        if (!isAgree) {
                            return;
                        }
                        startReceiveFile(fromDevice, fileTransfer, files, socket, inputStream, outputStream, encData);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                    }
                });
            }
        };
        // 是否弹出确认接收dialog
        boolean isNotRecvDialog = App.getPrefUtil().getBoolean(PreConfig.NOT_RECV_DIALOG, true);
        if (isNotRecvDialog) {
            recvFileCallback.receviceFile(true);
        } else {
            lanService.sendIfReceiveFilesMsg(recvFileCallback, files.size());
        }
    }

    /**
     * 开始接收文件
     *
     * @param fromDevice   发送方设备
     * @param fileTransfer 文件传输对象
     * @param files        文件列表
     * @param client       Socket 连接
     * @param input        输入流
     * @param output       输出流
     * @param encData      是否加密数据
     * @throws IOException 如果发生 I/O 错误
     */
    public void startReceiveFile(Device fromDevice, FileTransfer fileTransfer, List<MessageFileContent> files, Socket client, CustomDataInputStream input, CustomDataOutputStream output, boolean encData) throws IOException {
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

    /**
     * 处理文件夹接收
     *
     * @param inputStream  输入流
     * @param fileTransfer 文件传输对象
     * @param fileItem     文件夹内容
     * @param encData      是否加密数据
     * @throws IOException 如果发生 I/O 错误
     */
    private void handleFolder(CustomDataInputStream inputStream, FileTransfer fileTransfer, MessageFolderContent fileItem, boolean encData) throws IOException {
        long fileSize = fileItem.getLength();
        List<MessageFileContent> children = fileItem.getChildren();
        long total = 0;
        // 文件分类
        String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER);
        File folderFile = new File(path, fileItem.getName());
        // 避免重名
        folderFile = FileUtil.avoidDuplication(folderFile);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        for (MessageFileContent child : children) {
            File filePath = new File(folderFile, child.getName());
            filePath = FileUtil.avoidDuplication(filePath);
            File parentFile = filePath.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            // 创建空文件
            if (child.getLength() <= 0) {
                filePath.createNewFile();
                continue;
            }
            long ten;
            if (encData) {
                ten = recvStreamToFileDec(fileTransfer, inputStream, total, fileSize, fileItem, child, filePath, progressCallback);
            } else {
                ten = recvStreamToFile(fileTransfer, inputStream, total, fileSize, fileItem, child, filePath, progressCallback);
            }
            if (ten < 0) {
                total = 0;
                break;
            }
            total += ten;
        }
        progressCallback.onItemFinish(fileTransfer, fileItem, total == fileSize);
    }

    /**
     * 处理文件接收
     *
     * @param inputStream  输入流
     * @param fileTransfer 文件传输对象
     * @param fileItem     文件内容
     * @param encData      是否加密数据
     * @throws IOException 如果发生 I/O 错误
     */
    private void handleFile(CustomDataInputStream inputStream, FileTransfer fileTransfer, MessageFileContent fileItem, boolean encData) throws IOException {
        String fileName = fileItem.getName();
        long fileSize = fileItem.getLength();
        // 文件分类
        String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileItem.getName()));
        File folderFile = new File(path);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        File filePath = new File(folderFile, fileName);
        filePath = FileUtil.avoidDuplication(filePath);
        logger.debug("fileName: {}, fileSize: {}", fileName, fileSize);
        try {
            long total = 0;
            if (fileSize > 0) {
                if (encData) {
                    total = recvStreamToFileDec(fileTransfer, inputStream, 0, fileSize, fileItem, fileItem, filePath, progressCallback);
                } else {
                    total = recvStreamToFile(fileTransfer, inputStream, 0, fileSize, fileItem, fileItem, filePath, progressCallback);
                }
                fileItem.setPath(filePath.getPath());
            } else {
                filePath.createNewFile();
            }
            progressCallback.onItemFinish(fileTransfer, fileItem, fileItem.getLength() == total);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
    }

    /**
     * 接收流到文件(未加密)
     *
     * @param fileTransfer 文件传输对象
     * @param inputStream  输入流
     * @param total        已接收总大小
     * @param finalSize    最终总大小
     * @param baseFileItem 基础文件项
     * @param fileItem     当前文件项
     * @param filePath     目标文件路径
     * @param callback     进度回调
     * @return 实际接收的字节数,失败返回 -1
     * @throws IOException 如果发生 I/O 错误
     */
    public long recvStreamToFile(FileTransfer fileTransfer, CustomDataInputStream inputStream, long total, long finalSize, MessageFileContent baseFileItem, MessageFileContent fileItem, File filePath, ProgressCallback callback) throws IOException {
        int ten;
        long subTotal = 0;
        long targetSize = fileItem.getLength();
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024];
        OutputStream outFileStream = new FileOutputStream(filePath);
        try {
            while (true) {
                int length = inputStream.readInt();
                if (length <= 0) {
                    logger.debug("cmd error: {}", length);
//                    if(length == LCmd.FS_NEXT){
//                        return -1;
//                    }
                    return -1;
                }
                ten = inputStream.readFully(buffer, 0, length);
                if (ten <= 0) {
                    break;
                }
                outFileStream.write(buffer, 0, ten);
                subTotal += ten;
                total += ten;
                targetSize -= ten;
                progress = (int) (total * 100 / finalSize);
                if (progress != lastProgress) {
                    lastProgress = progress;
                    if (callback != null) {
                        baseFileItem.setProgress(progress);
                        callback.onProgress(fileTransfer, baseFileItem);
                    }
                }
                if (targetSize <= 0) {
                    break;
                }
            }
        } finally {
            outFileStream.flush();
            IOUtil.closeIO(outFileStream);
        }
        if (subTotal != fileItem.getLength()) {
            subTotal = -1;
        }
        return subTotal;
    }

    /**
     * 接收流到文件(加密)
     *
     * @param fileTransfer 文件传输对象
     * @param inputStream  输入流
     * @param total        已接收总大小
     * @param finalSize    最终总大小
     * @param baseFileItem 基础文件项
     * @param fileItem     当前文件项
     * @param filePath     目标文件路径
     * @param callback     进度回调
     * @return 实际接收的字节数,失败返回 -1
     * @throws IOException 如果发生 I/O 错误
     */
    public long recvStreamToFileDec(FileTransfer fileTransfer, CustomDataInputStream inputStream, long total, long finalSize, MessageFileContent baseFileItem, MessageFileContent fileItem, File filePath, ProgressCallback callback) throws IOException {
        int ten;
        long subTotal = 0;
        long targetSize = fileItem.getLength();
        int progress;
        int lastProgress = 0;
        byte[] buffer = new byte[1024 * 1024];
        OutputStream outFileStream = new FileOutputStream(filePath);
        try {
            while (true) {
                int length = inputStream.readInt();
                if (length <= 0) {
                    logger.debug("cmd error: {}", length);
//                    if(length == LCmd.FS_NEXT){
//                        return -1;
//                    }
                    return -1;
                }
                ten = inputStream.readFully(buffer, 0, length);
                if (ten <= 0) {
                    break;
                }
                // 解密
                mUtil.decData(buffer, ten, 0, subTotal);
                outFileStream.write(buffer, 0, ten);
                subTotal += ten;
                total += ten;
                targetSize -= ten;
                progress = (int) (total * 100 / finalSize);
                if (progress != lastProgress) {
                    lastProgress = progress;
                    if (callback != null) {
                        baseFileItem.setProgress(progress);
                        callback.onProgress(fileTransfer, baseFileItem);
                    }
                }
                if (targetSize <= 0) {
                    break;
                }
            }
        } finally {
            outFileStream.flush();
            IOUtil.closeIO(outFileStream);
        }
        if (subTotal != fileItem.getLength()) {
            subTotal = -1;
        }
        return subTotal;
    }


    /** 进度回调 */
    public ProgressCallback progressCallback = new ProgressCallback() {
        @Override
        public void onStart(FileTransfer fileTransfer) {
            lanService.sendShowProgressMeg(fileTransfer.getFiles());
        }

        @Override
        public void onProgress(FileTransfer fileTransfer, MessageFileContent fileItem) {
//            logger.debug("progress: {}" + progress);
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


}























