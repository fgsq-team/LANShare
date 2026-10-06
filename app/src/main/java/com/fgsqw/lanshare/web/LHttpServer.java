package com.fgsqw.lanshare.web;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.*;


import com.fgsqw.httpserver.HttpConstant;
import com.fgsqw.httpserver.HttpServer;
import com.fgsqw.httpserver.Request;
import com.fgsqw.httpserver.Response;
import com.fgsqw.httpserver.exception.L302Exception;
import com.fgsqw.httpserver.exception.L404Exception;
import com.fgsqw.httpserver.stream.SingleUploadInputStream;
import com.fgsqw.httpserver.websocket.WebSocketServer;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.activity.DrawingActivity;
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



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * LANShare HTTP服务
 * <p>
 * 负责启动和管理HTTP服务器，提供网页端访问局域网设备资源的接口。
 * 主要功能包括：文件浏览与下载、APK管理、媒体文件访问、文件上传与分享、
 * WebSocket实时通讯、设备列表同步等。
 * </p>
 *
 * @author fgsq
 * @version 1.0
 */
public class LHttpServer {

    /** HTTP服务器实例 */
    private final HttpServer httpServer;
    /** APK图标数据库操作工具 */
    private ApkIconDBUtil apkIconDBUtil;
    /** Token令牌数据库操作工具 */
    private TokenDBUtil tokenDBUtil;
    /** 文件分享数据库操作工具 */
    private FileShareDBUtil fileShareDBUtil;
    /** 局域网服务实例 */
    private LANService lanService;

    /** 需要进行Token鉴权的路径列表 */
    private String[] paths = {
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

    /** 当前所有已连接的WebSocket服务器列表（线程安全） */
    public static List<WebSocketServer> webSocketServers = new Vector<>();

    /** 网页端侧边栏菜单配置 */
    private static final JSONArray WEB_MENUS = new JSONArray();
    static {
        JSONObject m1 = new JSONObject();
        m1.put("key", "apps"); m1.put("text", "软件"); m1.put("icon", "nav-item-media-apps");
        WEB_MENUS.add(m1);
        JSONObject m2 = new JSONObject();
        m2.put("key", "media"); m2.put("text", "图片"); m2.put("icon", "nav-item-media-img");
        WEB_MENUS.add(m2);
        JSONObject m3 = new JSONObject();
        m3.put("key", "files"); m3.put("text", "文件列表"); m3.put("icon", "nav-item-media-folder");
        WEB_MENUS.add(m3);
        JSONObject m4 = new JSONObject();
        m4.put("key", "chat"); m4.put("text", "消息记录"); m4.put("icon", "nav-item-media-record");
        WEB_MENUS.add(m4);
        JSONObject m5 = new JSONObject();
        m5.put("key", "draw"); m5.put("text", "远程绘图"); m5.put("icon", "nav-item-media-record");
        WEB_MENUS.add(m5);
    }

    // ==================== 文件压缩 ====================

    /**
     * 递归压缩文件/目录到ZipOutputStream
     *
     * @param beginIndex     路径截取起始索引，用于生成zip内的相对路径
     * @param file           待压缩的文件或目录
     * @param zipOutputStream 目标Zip输出流
     */
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

    // ==================== WebSocket消息推送 ====================

    /**
     * 向所有已连接的WebSocket客户端广播消息
     *
     * @param message     消息内容
     * @param toDevName   目标设备名
     * @param filePath    文件路径（如有）
     * @param messageType 消息类型
     * @param devType     设备类型
     * @param fileSize    文件大小描述
     * @param isLeft      是否显示在左侧
     * @param isClip      是否为剪贴板内容
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

    /**
     * 向指定WebSocket客户端发送消息
     *
     * @param webSocketServer 目标WebSocket服务器
     * @param message         消息内容
     * @param toDevName       目标设备名
     * @param filePath        文件路径（如有）
     * @param messageType     消息类型
     * @param devType         设备类型
     * @param fileSize        文件大小描述
     * @param isLeft          是否显示在左侧
     * @param isClip          是否为剪贴板内容
     */
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

    /**
     * 向所有已连接的WebSocket客户端推送在线设备列表
     */
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

    /**
     * 向所有已连接的WebSocket客户端推送主题变更通知
     *
     * @param theme 主题名称: "light" / "dark" / "emerald" / "follow_system"
     */
    public static void sendThemeChange(String theme) {
        Iterator<WebSocketServer> iterator = webSocketServers.iterator();
        while (iterator.hasNext()) {
            WebSocketServer webSocketServer = iterator.next();
            if (!webSocketServer.isClosed()) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cmd", WSCmd.CHANGE_THEME);
                    jsonObject.put("theme", theme);
                    webSocketServer.sendString(jsonObject.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                    IOUtil.closeIO(webSocketServer);
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }

    /**
     * 向所有已连接的WebSocket客户端广播绘图事件
     *
     * @param drawEventJson 绘图事件JSON字符串，包含cmd/action/x/y/color/strokeWidth
     */
    public static void sendDrawEvent(String drawEventJson) {
        Iterator<WebSocketServer> iterator = webSocketServers.iterator();
        while (iterator.hasNext()) {
            WebSocketServer webSocketServer = iterator.next();
            if (!webSocketServer.isClosed()) {
                try {
                    webSocketServer.sendString(drawEventJson);
                } catch (IOException e) {
                    e.printStackTrace();
                    IOUtil.closeIO(webSocketServer);
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }


    // ==================== 路由注册 ====================

    /**
     * 构造LHttpServer实例，初始化数据库工具并注册所有HTTP路由
     *
     * @param lanService 局域网服务实例
     */
    public LHttpServer(LANService lanService) {
        this.lanService = lanService;
        apkIconDBUtil = new ApkIconDBUtil(lanService);
        tokenDBUtil = new TokenDBUtil(lanService);
        fileShareDBUtil = new FileShareDBUtil(lanService);
        httpServer = new HttpServer(ThreadUtils.EXECUTOR_SERVICE);
        // ==================== 鉴权过滤器 ====================
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

        // ==================== Token与设备管理接口 ====================

        // 检测Token是否已通过授权
        httpServer.addPath("/checkPass", (request, response) -> {
            String token = request.getHeaderValue("token");
            Token t = tokenDBUtil.queryByToken(token);
            JSONObject object = new JSONObject();
            object.put("pass", t != null && t.getPass() == 1);
            response.writeString(object.toJSONString());
        });

        // ==================== 文件分享接口 ====================

        // 文件分享下载（根据UUID获取分享文件）
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

        // ==================== 文件上传接口 ====================

        // 聊天文件上传（流式转发到指定设备）
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

        // 通用文件上传（保存到本地并通知UI）
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

        // 更新网页端设备名称
        httpServer.addPath("/updateWebName", (request, response) -> {
            JSONObject object = JSON.parseObject(request.getRequestBody());
            String webName = object.getString("webName");
            String token = request.getHeaderValue("token");
            tokenDBUtil.updateName(token, webName);
            response.writeEmpty();
        });

        // ==================== 初始化配置接口 ====================

        // 初始化网页端配置（Token分配、设备命名、授权检测）
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
            // 返回当前APP的主题模式
            int themeMode = App.getPrefUtil().getInt(PreConfig.THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            String theme;
            switch (themeMode) {
                case AppCompatDelegate.MODE_NIGHT_YES:
                    theme = "dark";
                    break;
                case AppCompatDelegate.MODE_NIGHT_NO:
                    theme = "light";
                    break;
                case 3:
                    theme = "emerald";
                    break;
                default:
                    theme = "follow_system";
                    break;
            }
            object.put("theme", theme);
            object.put("menus", WEB_MENUS);
            response.writeString(object.toJSONString());
        });

        // ==================== APK管理接口 ====================

        // 获取已安装APK列表
        httpServer.addPath("/apps", (request, response) -> {
            List<MessageApkContent> apkFileList = AnyData.apkFileList;
            if (apkFileList != null) {
                JSONObject result = new JSONObject();
                JSONArray array = new JSONArray();
                for (MessageApkContent apkInfo : apkFileList) {
                    JSONObject apk = new JSONObject();
                    apk.put("name", apkInfo.getName());
                    apk.put("packageName", apkInfo.getPackageName());
                    apk.put("length", FileUtil.computeSize(apkInfo.getLength()));
                    array.add(apk);
                }
                result.put("list", array);
                response.writeString(result.toJSONString());
            }
        });

        // 获取APK图标
        httpServer.addPath("/appicon", (request, response) -> {
            String packageName = request.getQueryParam("packageName");
            byte[] bytes = apkIconDBUtil.queryIconByPackageName(packageName);
            response.writeBytes(bytes, HttpConstant.STREAM_CONTEXT_IMAGE);
        });

        // ==================== 媒体文件接口 ====================

        // 获取相册图片缩略图
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

        // ==================== 静态资源接口 ====================

        // 加载web目录下的静态图片资源
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

        // ==================== 文件浏览接口 ====================

        // 获取指定目录的文件列表
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
                    if (file == null || !file.exists() || !file.canRead()) {
                        file = Environment.getExternalStorageDirectory();
                    }
                }
            }
            if (file == null) {
                file = Environment.getExternalStorageDirectory();
            }
            boolean showHiddenFiles = App.getPrefUtil().getBoolean(PreConfig.SHOW_HIDDEN_FILES, false);
            try {
                MessageFolderContent fs = new MessageFolderContent();
                fs.setPath(file.getPath());
                int fileSortMethod = App.getPrefUtil().getInt(PreConfig.FILE_SORT_METHOD, 0);
                List<MessageFileContent> fileList = DeviceDataScanner.listDirectoryContents(fs, showHiddenFiles, fileSortMethod, lanService);
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

        // ==================== 文件压缩与下载接口 ====================

        // 下载已压缩的临时zip文件
        httpServer.addPath("/downloadZipFile", (request, response) -> {
            String tempFile = request.getQueryParam("tempFile");
            File file = new File(lanService.getExternalCacheDir().getPath() + "/" + tempFile);

            if (file.exists()) {
                String fileName = file.getName();
                String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
                response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
                response.writeFile(file);
            } else {
                response.write404();
            }
            file.delete();
        });

        // 压缩指定文件列表为zip（返回临时文件名）
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

        // 压缩打包媒体文件为zip（返回临时文件名）
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

        // ==================== 媒体浏览接口 ====================

        // 获取媒体列表（folderIndex=-1时返回文件夹列表，否则返回指定文件夹内的媒体）
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

        // ==================== 文件下载接口 ====================

        // APK文件下载（根据包名获取APK文件）
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

        // 通用文件下载（根据路径下载任意文件）
        httpServer.addPath("/file/*", (request, response) -> {
            String path = request.getQueryParam("path");
            File file = new File(path);
            if (file.exists()) {
                response.writeFile(file);
            } else {
                response.write404();
            }
        });

        // ==================== 资源映射接口 ====================

        // Drawable资源图片映射（根据资源名返回对应的图片）
        httpServer.addPath("/drawable", (request, response) -> {
            String name = request.getQueryParam("name");
            Resources r = lanService.getResources();
            int resource = ImageUtils.getResource(name);
            String resourceName = ImageUtils.getResourceName(resource);
            String contentTypeByName = Response.getContentTypeByName(resourceName);
            InputStream is = r.openRawResource(resource);
            response.writeStream(is, contentTypeByName);
        });

        // ==================== WebSocket通讯接口 ====================

        // WebSocket升级与消息通讯处理
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

            lanService.getDeviceManager().onLineWebDevices.put(address, webDevice);
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
                    } else if (cmd == WSCmd.DRAW_EVENT) {
                        // 网页端发来的绘图事件：广播给其他网页客户端 + 转发给APP本地渲染
                        String drawText = text;
                        ThreadUtils.runThread(() -> {
                            // 广播给所有网页客户端（网页端会忽略 from=web 的消息，防止回显）
                            JSONObject drawJson = JSON.parseObject(drawText);
                            drawJson.put("from", "web");
                            String broadcastJson = drawJson.toJSONString();
                            Iterator<WebSocketServer> it = webSocketServers.iterator();
                            while (it.hasNext()) {
                                WebSocketServer ws = it.next();
                                if (!ws.isClosed() && ws != webSocketServer) {
                                    try {
                                        ws.sendString(broadcastJson);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            // 转发给APP本地 DrawingActivity 渲染
                            String action = drawJson.getString("action");
                            float nx = drawJson.getFloatValue("x");
                            float ny = drawJson.getFloatValue("y");
                            int color = drawJson.getIntValue("color");
                            float sw = drawJson.getFloatValue("strokeWidth");
                            DrawingActivity.handleRemoteDraw(action, nx, ny, color, sw);
                        });
                    } else if (cmd == WSCmd.PING) {
                        // 心跳保活：回复PONG
                        try {
                            JSONObject pong = new JSONObject();
                            pong.put("cmd", WSCmd.PONG);
                            webSocketServer.sendString(pong.toJSONString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtil.closeIO(webSocketServer);
                lanService.removeDevice(ip + ":" + port);
                lanService.getDeviceManager().onLineWebDevices.remove(address);
                webSocketServers.remove(webSocketServer);
            }

        });

        // ==================== Web前端静态资源接口 ====================

        // CSS样式文件加载
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

        // JS脚本文件加载
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

        // 网站图标
        httpServer.addPath("/favicon.ico", (request, response) -> {
            String filePath = "web/images/lanshare.png";
            InputStream open = lanService.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
            response.writeBytes(bytes, myMIMEType);
        });

        // 主页（加载默认HTML页面）
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

    // ==================== 服务器控制 ====================

    /**
     * 获取HTTP服务器实例
     *
     * @return HTTP服务器实例
     */
    public HttpServer getHttpServer() {
        return httpServer;
    }

    /**
     * 启动HTTP服务器
     *
     * @throws Exception 启动异常
     */
    public void startHttpServer() throws Exception {
        httpServer.start();
    }

    /**
     * 以混合模式启动HTTP服务器（与LANService共用端口）
     *
     * @throws IOException IO异常
     */
    public void startBlendingModeHttpServer() throws IOException {
        httpServer.startBlendingMode();
    }

}
