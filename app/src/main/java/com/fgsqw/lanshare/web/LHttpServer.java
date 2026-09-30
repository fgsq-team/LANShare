package com.fgsqw.lanshare.web;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.*;

import com.fgsqw.exception.L302Exception;
import com.fgsqw.exception.L404Exception;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.constants.WSCmd;
import com.fgsqw.lanshare.db.ApkIconDBUtil;
import com.fgsqw.lanshare.db.FileShareDBUtil;
import com.fgsqw.lanshare.db.TokenDBUtil;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.Token;
import com.fgsqw.lanshare.pojo.file.*;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageDownloadInfoContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.pojo.message.MessageStreamContent;
import com.fgsqw.lanshare.pojo.network.MediaResult;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.stream.SingleUploadInputStream;
import com.fgsqw.websocket.WebSocketServer;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * LANShare HTTP服务
 */
public class LHttpServer {

    private final HttpServer httpServer;
    private ApkIconDBUtil apkIconDBUtil;
    private TokenDBUtil tokenDBUtil;
    private FileShareDBUtil fileShareDBUtil;
    private LANService lanService;

    String[] paths = {
            "/apps",
            "/media",
            "/files",
            "/compressMedias",
            "/apkfile/*",
            "/file/*",
            "/wss",
            "/imageload/*",
            "/uploadFile",
            "/chatUploadFile",
            "/updateWebName",
    };

    public static List<WebSocketServer> webSocketServers = new Vector<>();

    public void compressFiles(int beginIndex, File file, ZipOutputStream zipOutputStream) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    compressFiles(beginIndex, f, zipOutputStream);
                    continue;
                }
                String subPath = f.getPath().substring(beginIndex);
                ZipUtils.compressFiles(f, subPath, zipOutputStream);
            }
        } else {
            if (!file.canRead()) {
                return;
            }
            String subPath = file.getPath().substring(beginIndex);
            ZipUtils.compressFiles(file, subPath, zipOutputStream);
        }

    }

    /**
     * 发送消息
     *
     * @param message   消息内容
     * @param toDevName 设备名
     * @param devType   设备类型
     * @param isLeft    是否在左边
     */
    public static void sendMessage(String message, String toDevName, String filePath, int messageType, int devType, String fileSize, boolean isLeft, boolean isClip) {
        Iterator<WebSocketServer> iterator = webSocketServers.iterator();
        while (iterator.hasNext()) {
            WebSocketServer webSocketServer = iterator.next();
            // 判断webSocket是否已经关闭，已经关闭的顺便从列表移除
            if (!webSocketServer.isClosed()) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cmd", WSCmd.SEND_MSSAGE);
                    jsonObject.put("isLeft", isLeft);
                    jsonObject.put("message", message);
                    jsonObject.put("devName", toDevName);
                    jsonObject.put("devType", devType);
                    jsonObject.put("messageType", messageType);
                    jsonObject.put("isClip", isClip);
                    jsonObject.put("filePath", filePath);
                    jsonObject.put("fileSize", fileSize);
                    webSocketServer.sendString(jsonObject.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                IOUtil.closeIO(webSocketServer);
                iterator.remove();
            }
        }
    }

    public static void sendMessage(WebSocketServer webSocketServer, String message, String toDevName, String filePath, int messageType, int devType, String fileSize, boolean isLeft, boolean isClip) {
        // 判断webSocket是否已经关闭，已经关闭的顺便从列表移除
        if (!webSocketServer.isClosed()) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("cmd", WSCmd.SEND_MSSAGE);
                jsonObject.put("isLeft", isLeft);
                jsonObject.put("message", message);
                jsonObject.put("devName", toDevName);
                jsonObject.put("devType", devType);
                jsonObject.put("messageType", messageType);
                jsonObject.put("isClip", isClip);
                jsonObject.put("filePath", filePath);
                jsonObject.put("fileSize", fileSize);
                webSocketServer.sendString(jsonObject.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendDeviceList() {
        Iterator<WebSocketServer> iterator = webSocketServers.iterator();
        while (iterator.hasNext()) {
            WebSocketServer webSocketServer = iterator.next();
            // 判断webSocket是否已经关闭，已经关闭的顺便从列表移除
            if (!webSocketServer.isClosed()) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cmd", WSCmd.SYNC_DEVICE_LIST);
                    JSONArray array = new JSONArray();
                    for (Device value : LANService.getInstance().getOnLineDevices().values()) {
                        JSONObject json = new JSONObject();
                        json.put("devName", value.getDevName());
                        json.put("devIP", value.getDevIP());
                        json.put("devPort", value.getDevPort());
                        json.put("devMode", value.getDevMode());
                        json.put("dataVersion", value.getDataVersion());
                        json.put("address", value.getDevIP() + ":" + value.getDevPort());
                        array.add(json);
                    }
                    jsonObject.put("data", array);
                    String jsonString = jsonObject.toJSONString();
                    webSocketServer.sendString(jsonString);
                } catch (Exception e) {
                    e.printStackTrace();
                    IOUtil.closeIO(webSocketServer);
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }


    public LHttpServer(LANService lanService) {
        this.lanService = lanService;
        apkIconDBUtil = new ApkIconDBUtil(lanService);
        tokenDBUtil = new TokenDBUtil(lanService);
        fileShareDBUtil = new FileShareDBUtil(lanService);
        httpServer = new HttpServer(ThreadUtils.EXECUTOR_SERVICE);
        // 过滤器
        httpServer.setRequestFilter((request, response, httpHandler) -> {
            if (Config.WEB_OPEN) {
                httpHandler.handle(request, response);
                return;
            }
            for (String path : paths) {
                if (HttpServer.pathMatches(path, request.getRequestPath())) {
                    String token;
                    if (request.getRequestMethod().equalsIgnoreCase(HttpConstant.METHOD_POST)) {
                        token = request.getHeaderValue("token");
                    } else {
                        token = request.getQueryParam("token");
                    }
                    if (token == null) {
                        throw new L302Exception(App.getResString(R.string.web_access_denied), "/");
                    }
                    Token s = tokenDBUtil.queryByToken(token);
                    if (s == null || s.getPass() != 1) {
                        throw new L302Exception(App.getResString(R.string.web_access_denied), "/");
                    }
                }
            }
            httpHandler.handle(request, response);
        });

        // 检测是否通行
        httpServer.addPath("/checkPass", (request, response) -> {
            String token = request.getHeaderValue("token");
            Token t = tokenDBUtil.queryByToken(token);
            JSONObject object = new JSONObject();
            object.put("pass", t != null && t.getPass() == 1);
            response.writeString(object.toJSONString());
        });

        // 文件分享
        httpServer.addPath("/sharefile/*", (request, response) -> {
            String uuid = request.getQueryParam("uuid");
            MessageDownloadInfoContent fileInfo = fileShareDBUtil.queryShare(uuid);
            if (fileInfo == null) {
                throw new L404Exception();
            }
            File file = new File(fileInfo.getPath());
            if (!file.exists()) {
                throw new L404Exception();
            }
            if (fileInfo.getDays() == -1) {
                if (fileInfo.isDownloaded()) {
                    response.writeString(App.getResString(R.string.file_expired), "text/plain; charset=UTF-8");
                    return;
                }
            } else {
                if (!DateUtils.isFutureDate(fileInfo.getDays(), fileInfo.getTime())) {
                    response.writeString(App.getResString(R.string.file_expired));
                    return;
                }
            }
            response.writeFile(file);
            fileShareDBUtil.updateDownloaded(uuid, true);
        });

        // 文件上传
        httpServer.addPath("/chatUploadFile", (request, response) -> {
            String address = request.getQueryParam("address");
            Device device = lanService.getOnLineDevices().get(address);
            if (device == null) {
                response.write500();
                return;
            }
            // 初始许可为0
            Semaphore semaphore = new Semaphore(0);
            SingleUploadInputStream uploadInputStream = request.getSingleUploadInputStream();
            MessageStreamContent streamInfo = new MessageStreamContent(uploadInputStream);
            streamInfo.setName(uploadInputStream.getFileName());
            streamInfo.setLength(uploadInputStream.getFileSize());
            streamInfo.setSemaphore(semaphore);
            Device from = new Device();
            from.setDevName(request.getClientIP());
            lanService.fileSendSync(from, device, Collections.singletonList(streamInfo));
            semaphore.acquire();
            response.writeString("文件上传成功，大小: " + FileUtil.computeSize(uploadInputStream.getFileSize()));
        });

        // 文件上传
        httpServer.addPath("/uploadFile", (request, response) -> {
            File file = new File(Config.FILE_SAVE_PATH + "网页收到的文件/");
            if (!file.exists()) {
                file.mkdirs();
            }
            Request.UploadResult uploadResult = request.transferUploadFile(file.getPath());
            String fileName = uploadResult.getFileName();
            Long fileSize = uploadResult.getFileSize();
            String filePath = uploadResult.getFilePath();
            MessageFileContent fileContent = new MessageFileContent();
            fileContent.setId(StringUtils.getUUID());
            fileContent.setStatus(MessageContent.IN);
            fileContent.setContent(fileName);
            fileContent.setLength(fileSize);
            fileContent.setUserName("(网页设备)");
            fileContent.setIndex(0);
            fileContent.setLeft(true);
            fileContent.setDevMode(Device.WINDOWS);
            fileContent.setProgress(100);
            fileContent.setStatus(MessageContent.SUCCESS);
            fileContent.setStateMessage("接收成功");
            fileContent.setPath(filePath);
            Message mMessage;
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = Collections.singletonList(fileContent);
            lanService.messageSend(mMessage);
            response.writeString("文件上传成功，大小: " + FileUtil.computeSize(fileSize));
        });

        httpServer.addPath("/updateWebName", (request, response) -> {
            JSONObject object = JSON.parseObject(request.getRequestBody());
            String webName = object.getString("webName");
            String token = request.getHeaderValue("token");
            tokenDBUtil.updateName(token, webName);
            response.writeEmpty();
        });

        // 初始化配置
        httpServer.addPath("/initConfig", (request, response) -> {
            JSONObject object = new JSONObject();
            object.put("rootPath", Environment.getExternalStorageDirectory().getPath());
            String token = request.getHeaderValue("token");
            String name = "网页设备" + mUtil.generateRandomString(6);
            boolean pass = false;
            Token t = tokenDBUtil.queryCustonIp(request.getClientIP());
            if (t != null) {
                pass = t.getPass() == 1;
                if (StringUtils.isEmpty(t.getName())) {
                    tokenDBUtil.updateName(t.getToken(), name);
                } else {
                    name = t.getName();
                }
                token = t.getToken();
            } else if (StringUtils.isEmpty(token)) {
                token = StringUtils.getUUID();
                tokenDBUtil.addToken(token, false, 0, request.getClientIP(), name);
                if (Config.WEB_OPEN) {
                    pass = true;
                } else {
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_HTTP_NEW_CLIENT;
                    mMessage.obj = new String[]{token, request.getClientIP(), name};
                    lanService.messageSend(mMessage);
                }
            } else {
                t = tokenDBUtil.queryByToken(token);
                if (t == null) {
                    tokenDBUtil.addToken(token, false, 0, request.getClientIP(), name);
                    if (Config.WEB_OPEN) {
                        pass = true;
                    } else {
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_HTTP_NEW_CLIENT;
                        mMessage.obj = new String[]{token, request.getClientIP(), name};
                        lanService.messageSend(mMessage);
                    }
                } else {
                    if (t.getPass() != 1) {
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_HTTP_NEW_CLIENT;
                        mMessage.obj = new String[]{token, request.getClientIP(), name};
                        lanService.messageSend(mMessage);
                    }
                    pass = t.getPass() == 1;
                    name = t.getName();
                }
            }
            object.put("token", token);
            object.put("name", name);
            object.put("pass", pass);
            response.writeString(object.toJSONString());
        });

        // app列表
        httpServer.addPath("/apps", (request, response) -> {
            List<MessageApkContent> apkFileList = AnyData.apkFileList;
            if (apkFileList != null) {
                JSONObject resault = new JSONObject();
                JSONArray array = new JSONArray();
                for (MessageApkContent apkInfo : apkFileList) {
                    JSONObject apk = new JSONObject();
                    apk.put("name", apkInfo.getName());
                    apk.put("packageName", apkInfo.getPackageName());
                    apk.put("length", FileUtil.computeSize(apkInfo.getLength()));
                    array.add(apk);
                }
                resault.put("list", array);
                response.writeString(resault.toJSONString());
            }
        });

        // app图标
        httpServer.addPath("/appicon", (request, response) -> {
            String packageName = request.getQueryParam("packageName");
            byte[] bytes = apkIconDBUtil.queryIconByPackageName(packageName);
            response.writeBytes(bytes, HttpConstant.STREAM_CONTEXT_IMAGE);
        });

        // 相册图片
        httpServer.addPath("/imageload/*", (request, response) -> {
            String index = request.getQueryParam("index");
            MediaResult mediaResult = AnyData.mediaResult;
            Map<Integer, MessageMediaContent> allMediaMap = mediaResult.getAllMediaMap();
            MessageMediaContent mediaInfo = allMediaMap.get(Integer.valueOf(index));
            if (mediaInfo != null) {
                Bitmap imageThumbnail;
                if (mediaInfo.isVideo()) {
                    imageThumbnail = ImageUtils.getVideoThumbnail(mediaInfo.getPath(), 200, 200);
                } else {
                    imageThumbnail = ImageUtils.getImageThumbnail(mediaInfo.getPath(), 200, 200);
                }
                imageThumbnail.compress(Bitmap.CompressFormat.PNG, 100, response.getBodyOutputStream(HttpConstant.STREAM_CONTEXT_IMAGE));
            } else {
                response.write404();
            }
        });

        // 获取图片
        httpServer.addPath("/images/*", (request, response) -> {
            String path = request.getRequestPath();
            String filePath = "web";
            filePath += path;
            InputStream open = lanService.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            if (i > 0) {
                String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
                response.writeBytes(bytes, myMIMEType);
            }
        });

        // 文件列表
        httpServer.addPath("/files", (request, response) -> {
            String str = request.getRequestBody();
            JSONObject jsonObject = JSON.parseObject(str);
            Boolean isBack = jsonObject.getBoolean("isBack");
            String path = jsonObject.getString("path");
            path = URLDecoder.decode(path, FileUtil.UTF_8.toString());

            File file = null;
            if (!StringUtils.isEmpty(path)) {
                file = new File(path);
                if (isBack) {
                    file = file.getParentFile();
                }
            }
            if (file == null) {
                file = Environment.getExternalStorageDirectory();
            }
            boolean showHiddenFiles = App.getPrefUtil().getBoolean(PreConfig.SHOW_HIDDEN_FILES, false);
            try {
                MessageFileContent fs = new MessageFileContent();
                fs.setPath(file.getPath());
                int fileSortMethod = App.getPrefUtil().getInt(PreConfig.FILE_SORT_METHOD, 0);
                List<MessageFileContent> fileList = FileSearchUtils.getFileList(fs, showHiddenFiles, fileSortMethod, lanService);
                JSONObject object = new JSONObject();
                object.put("path", file.getAbsolutePath());
                JSONArray jsonArray = new JSONArray();
                if (!fileList.isEmpty()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    for (MessageFileContent fileSource : fileList) {
                        String name = fileSource.getName();
                        if (showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        boolean isDirectory = fileSource instanceof MessageFolderContent;
                        JSONObject item = new JSONObject();
                        item.put("name", name);
                        item.put("length", fileSource.getLength());
                        item.put("path", fileSource.getPath());
                        item.put("isFile", !isDirectory);
                        item.put("time", dateFormat.format(fileSource.getTime()));
                        item.put("isDirectory", isDirectory);
                        jsonArray.add(item);
                    }
                }
                object.put("list", jsonArray);
                response.writeString(object.toJSONString());
            } catch (RuntimeException e) {
                T.s(e.getMessage());
                LLog.error("error", e);
            }
        });

        // 下载压缩后的文件
        httpServer.addPath("/downloadZipFile", (request, response) -> {
            String tempFile = request.getQueryParam("tempFile");
            File file = new File(lanService.getExternalCacheDir().getPath() + "/" + tempFile);
            if (file.exists()) {
                response.writeFile(file);
            } else {
                response.write404();
            }
            file.delete();
        });

        // 压缩文件
        httpServer.addPath("/compressFiles", (request, response) -> {
            String str = request.getRequestBody();
            JSONObject jsonObject = JSON.parseObject(str);
            JSONArray fileList = jsonObject.getJSONArray("list");
            File tempFile = new File(lanService.getExternalCacheDir().getPath() + "/" + System.currentTimeMillis() + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempFile), new CRC32()));
            for (int i = 0; i < fileList.size(); i++) {
                String path = fileList.getString(i);
                File file = new File(path);
                compressFiles(file.getParentFile().getPath().length() + 1, file, zipOutputStream);
            }
            zipOutputStream.finish();
            zipOutputStream.close();
            JSONObject result = new JSONObject();
            result.put("tempFile", tempFile.getName());
            response.writeString(result.toJSONString());
        });

        // 压缩打包媒体
        httpServer.addPath("/compressMedias", (request, response) -> {
            String str = request.getRequestBody();
            JSONObject jsonObject = JSON.parseObject(str);
            JSONArray list = jsonObject.getJSONArray("list");
            File tempFile = new File(lanService.getExternalCacheDir().getPath() + "/" + System.currentTimeMillis() + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempFile), new CRC32()));
            List<PhotoFolder> photoFolders = AnyData.mediaResult.getmFolders();
            for (int i = 0; i < list.size(); i++) {
                JSONObject data = list.getJSONObject(i);
                Boolean isDirectory = data.getBoolean("isDirectory");
                if (isDirectory) {
                    int index = data.getIntValue("index");
                    PhotoFolder photoFolder = photoFolders.get(index);
                    List<MessageMediaContent> images = photoFolder.getImages();
                    for (MessageMediaContent image : images) {
                        File file = new File(image.getPath());
                        ZipUtils.compressFiles(file, photoFolder.getName() + "/" + file.getName(), zipOutputStream);
                    }
                } else {
                    int index = data.getIntValue("index");
                    int subIndex = data.getIntValue("subIndex");
                    PhotoFolder photoFolder = photoFolders.get(index);
                    List<MessageMediaContent> images = photoFolder.getImages();
                    MessageMediaContent image = images.get(subIndex);
                    File file = new File(image.getPath());
                    ZipUtils.compressFiles(file, photoFolder.getName() + "/" + file.getName(), zipOutputStream);
                }
            }
            zipOutputStream.finish();
            zipOutputStream.close();
            JSONObject result = new JSONObject();
            result.put("tempFile", tempFile.getName());
            response.writeString(result.toJSONString());

        });

        // 媒体列表
        httpServer.addPath("/media", (request, response) -> {
            String str = request.getRequestBody();
            JSONObject jsonObject = JSON.parseObject(str);
            MediaResult mediaResult = AnyData.mediaResult;
            if (mediaResult != null) {
                int folderIndex = jsonObject.getIntValue("folderIndex");
                JSONArray jsonArray = new JSONArray();
                if (folderIndex == -1) {
                    for (int i = 0; i < mediaResult.getmFolders().size(); i++) {
                        PhotoFolder photoFolder = mediaResult.getmFolders().get(i);
                        List<MessageMediaContent> images = photoFolder.getImages();
                        if (images != null && !images.isEmpty()) {
                            MessageMediaContent mediaInfo = photoFolder.getImages().get(0);
                            if (mediaInfo != null) {
                                JSONObject folder = new JSONObject();
                                folder.put("name", photoFolder.getName() + "(" + photoFolder.getImages().size() + ")");
                                folder.put("path", mediaInfo.getPath());
                                folder.put("isDirectory", true);
                                folder.put("count", photoFolder.getImages().size());
                                folder.put("index", i);
                                folder.put("imgIndex", mediaInfo.getIndex());
                                folder.put("isVideo", false);
                                jsonArray.add(folder);
                            }
                        }
                    }
                } else {
                    PhotoFolder photoFolder = mediaResult.getmFolders().get(folderIndex);
                    List<MessageMediaContent> images = photoFolder.getImages();
                    for (int i = 0; i < images.size(); i++) {
                        MessageMediaContent mediaInfo = images.get(i);
                        JSONObject folder = new JSONObject();
                        folder.put("name", mediaInfo.getName());
                        folder.put("path", mediaInfo.getPath());
                        folder.put("length", mediaInfo.getLength());
                        folder.put("isDirectory", false);
                        folder.put("index", folderIndex);
                        folder.put("isVideo", mediaInfo.isVideo());
                        folder.put("imgIndex", mediaInfo.getIndex());
                        folder.put("subIndex", i);
                        if (mediaInfo.isVideo()) {
                            folder.put("videoTime", mediaInfo.getVideoTime());
                        }
                        jsonArray.add(folder);
                    }
                }
                response.writeString(jsonArray.toJSONString());
            }
        });

        // apk文件下载
        httpServer.addPath("/apkfile/*", (request, response) -> {
            String packageName = request.getQueryParam("packageName");
            String path = apkIconDBUtil.queryPathByPackageName(packageName);
            if (path == null) {
                response.write404();
                return;
            }
            File file = new File(path);
            if (!file.exists()) {
                response.write404();
                return;
            }
            response.writeFile(file);
        });

        // 文件下载
        httpServer.addPath("/file/*", (request, response) -> {
            String path = request.getQueryParam("path");
            File file = new File(path);
            if (file.exists()) {
                response.writeFile(file);
            } else {
                response.write404();
            }
        });

        // drawable下载图片映射
        httpServer.addPath("/drawable", (request, response) -> {
            String name = request.getQueryParam("name");
            Resources r = lanService.getResources();
            int resource = ImageUtils.getResource(name);
            String resourceName = ImageUtils.getResourceName(resource);
            String contentTypeByName = Response.getContentTypeByName(resourceName);
            InputStream is = r.openRawResource(resource);
            response.writeStream(is, contentTypeByName);
        });

        // LANShare webSocket 通讯服务
        httpServer.addPath("/wss", (request, response) -> {
            String headerValue = request.getHeaderValue("Sec-WebSocket-Key");
            WebSocketServer webSocketServer = new WebSocketServer(headerValue, request.getSocket());
            webSocketServers.add(webSocketServer);
            String token = request.getQueryParam("token");
            Token t = tokenDBUtil.queryByToken(token);
            Device webDevice = new Device();
            String ip = request.getClientIP();
            int port = request.getSocket().getPort();
            webDevice.setDevIP(ip);
            webDevice.setDevPort(port);
            webDevice.setDevName(t.getName());
            webDevice.setDevMode(Device.WEB);
            webDevice.setCanRemove(false);
            webDevice.setWebSocketServer(webSocketServer);
            String address = webDevice.getDevIP() + ":" + webDevice.getDevPort();

            lanService.onLineWebDevices.put(address, webDevice);
//            lanService.addDevice(
//                    webDevice
//            );

//            LHttpServer.sendDeviceList();
            try {
                while (!webSocketServer.isClosed()) {
                    String text = webSocketServer.readString();
                    JSONObject jsonObject = JSON.parseObject(text);
                    int cmd = jsonObject.getIntValue("cmd");
                    if (cmd == WSCmd.SEND_MSSAGE) {
                        String message = jsonObject.getString("message");
                        String selectedDevice = jsonObject.getString("selectedDevice");
                        boolean isClip = jsonObject.getBoolean("isClip");
                        LANService instance = LANService.getInstance();
                        // 保存消息到消息列表
                        MessageContent messageContent = new MessageContent();
                        messageContent.setId(StringUtils.getUUID());
                        messageContent.setLeft(true);
                        messageContent.setContent(message);
                        messageContent.setUserName(t.getName());
                        messageContent.setToUser(App.getResString(R.string.all_devices));
                        if (StringUtils.isEmpty(selectedDevice)) {
                            instance.broadcastMessage(null, message, isClip, "", false);
                        } else {
                            Device device = instance.getOnLineDevices().get(selectedDevice);
                            if (device != null) {
                                messageContent.setToUser(device.getDevName());
                                instance.broadcastMessage(device, message, isClip, "", false);
                            }
                        }
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
                        mMessage.obj = messageContent;
                        instance.messageSend(mMessage);
                        Log.d("TAG", "ws msg:" + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtil.closeIO(webSocketServer);
                lanService.removeDevice(ip + ":" + port);
                lanService.onLineWebDevices.remove(address);
                webSocketServers.remove(webSocketServer);
            }

        });

        // 主页
        httpServer.addPath("/css/*", (request, response) -> {
            String path = request.getRequestPath();
            String filePath = "web";
            filePath += path;
            InputStream open = lanService.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            if (i > 0) {
                String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
                response.writeBytes(bytes, myMIMEType);
            }
        });

        httpServer.addPath("/js/*", (request, response) -> {
            String path = request.getRequestPath();
            String filePath = "web";
            filePath += path;
            InputStream open = lanService.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            if (i > 0) {
                String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
                response.writeBytes(bytes, myMIMEType);
            }
        });

        httpServer.addPath("/favicon.ico", (request, response) -> {
            String filePath = "web/images/lanshare.png";
            InputStream open = lanService.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
            response.writeBytes(bytes, myMIMEType);
        });

        // 主页
        httpServer.addPath("/", (request, response) -> {
            String filePath = "web";
            filePath += "/lanshare.html";
            InputStream open = lanService.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
            response.writeBytes(bytes, myMIMEType);
        });

    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

    public void startHttpServer() throws Exception {
        httpServer.start();
    }

    public void startBlendingModeHttpServer() throws IOException {
        httpServer.startBlendingMode();
    }


}
