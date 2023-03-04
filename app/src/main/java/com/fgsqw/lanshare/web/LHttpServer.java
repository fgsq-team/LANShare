package com.fgsqw.lanshare.web;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.ApkIconDBUtil;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.*;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * LANShare HTTP服务
 */
public class LHttpServer {
    private final HttpServer httpServer;

    ApkIconDBUtil apkIconDBUtil;


    public LHttpServer() {
        apkIconDBUtil = new ApkIconDBUtil(LANService.getInstance());
        httpServer = new HttpServer();
        // 初始化配置
        httpServer.addPath("/initConfig", (request, response) -> {
            JSONObject object = new JSONObject();
            object.put("rootPath", Environment.getExternalStorageDirectory().getPath());
            String string = object.toJSONString();
            response.writeString(string);
        });


        httpServer.addPath("/apps", (request, response) -> {
            List<ApkInfo> apkFileList = AnyData.apkFileList;
            if (apkFileList != null) {
                JSONObject resault = new JSONObject();
                JSONArray array = new JSONArray();
                for (int i = 0; i < 10; i++) {
                    for (ApkInfo apkInfo : apkFileList) {
                        JSONObject apk = new JSONObject();
                        apk.put("name", apkInfo.getName());
//                    apk.put("path", apkInfo.getPath());
                        apk.put("packageName", apkInfo.getPackageName());
                        apk.put("length", FileUtil.computeSize(apkInfo.getLength()));
                        array.add(apk);
                    }
                }

                resault.put("list", array);
                response.writeString(resault.toJSONString());
            }
        });

        // app图标
        httpServer.addPath("/appicon", (request, response) -> {
            String packageName = request.getPathParam("packageName");
            byte[] bytes = apkIconDBUtil.queryIconByPackageName(packageName);
            response.writeBytes(bytes, Response.STREAM_CONTEXT_IMAGE);
        });


        // 相册图片
        httpServer.addPath("/imageload/", (request, response) -> {
            String index = request.getPathParam("index");
            MediaResult mediaResult = AnyData.mediaResult;
            Map<Integer, MediaInfo> allMediaMap = mediaResult.getAllMediaMap();
            MediaInfo mediaInfo = allMediaMap.get(Integer.valueOf(index));
            if (mediaInfo != null) {
                Bitmap imageThumbnail;
                if (mediaInfo.isVideo()) {
                    imageThumbnail = ImageUtils.getVideoThumbnail(mediaInfo.getPath(), 200, 200);
                } else {
                    imageThumbnail = ImageUtils.getImageThumbnail(mediaInfo.getPath(), 200, 200);
                }
                imageThumbnail.compress(Bitmap.CompressFormat.PNG, 100, response.getOutputStream(Response.STREAM_CONTEXT_IMAGE));
            }
        });

        httpServer.addPath("/images", (request, response) -> {
            String path = request.getRequestURL();
            String filePath = "web";
            filePath += path;
            App instance = App.getInstance();
            InputStream open = instance.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            if (i > 0) {
                String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
                response.writeBytes(bytes, myMIMEType);
            }
        });

        httpServer.addPath("/files", (request, response) -> {
            String str = request.getRequestBody();
            JSONObject jsonObject = JSON.parseObject(str);
            Boolean isBack = jsonObject.getBoolean("isBack");
            String path = jsonObject.getString("path");
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
                List<FileSource> fileList = FIleSerachUtils.getFileList(file, showHiddenFiles, LANService.getInstance());
                JSONObject object = new JSONObject();
                object.put("path", file.getAbsolutePath());
                JSONArray jsonArray = new JSONArray();
                if (fileList.size() > 0) {
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm");
                    for (FileSource fileSource : fileList) {
                        String name = fileSource.getName();
                        if (showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        JSONObject item = new JSONObject();
                        item.put("name", name);
                        item.put("path", fileSource.getPath());
                        item.put("isFile", fileSource.isFile());
                        item.put("time", sfd.format(fileSource.getTime()));
                        item.put("isDirectory", !fileSource.isFile());
                        jsonArray.add(item);
                    }
                }
                object.put("list", jsonArray);
                response.writeString(object.toJSONString());
            } catch (RuntimeException e) {
                T.s(e.getMessage());
            }
        });

        httpServer.addPath("/media", (request, response) -> {
            String str = request.getRequestBody();
            JSONObject jsonObject = JSON.parseObject(str);
//            jsonObject.get()
            MediaResult mediaResult = AnyData.mediaResult;
            if (mediaResult != null) {
                int folderIndex = jsonObject.getIntValue("folderIndex");
                JSONArray jsonArray = new JSONArray();
                if (folderIndex == -1) {
                    for (int i = 0; i < mediaResult.getmFolders().size(); i++) {
                        PhotoFolder photoFolder = mediaResult.getmFolders().get(i);
                        MediaInfo mediaInfo = photoFolder.getImages().get(0);
                        if (mediaInfo != null) {
                            JSONObject folder = new JSONObject();
                            folder.put("name", photoFolder.getName());
                            folder.put("path", mediaInfo.getPath());
                            folder.put("isDirectory", true);
                            folder.put("count", photoFolder.getImages().size());
                            folder.put("index", i);
                            folder.put("imgIndex", mediaInfo.getIndex());
                            folder.put("isVide", false);
                            jsonArray.add(folder);
                        }
                    }
                } else {
                    PhotoFolder photoFolder = mediaResult.getmFolders().get(folderIndex);
                    List<MediaInfo> images = photoFolder.getImages();
                    for (int i = 0; i < images.size(); i++) {
                        MediaInfo mediaInfo = images.get(i);
                        JSONObject folder = new JSONObject();
                        folder.put("name", mediaInfo.getName());
                        folder.put("path", mediaInfo.getPath());
                        folder.put("isDirectory", false);
                        folder.put("index", folderIndex);
                        folder.put("isVide", false);
                        folder.put("imgIndex", mediaInfo.getIndex());
                        jsonArray.add(folder);
                    }
                }
                response.writeString(jsonArray.toJSONString());
            }
        });

        httpServer.addPath("/apkfile/", (request, response) -> {
            String packageName = request.getPathParam("packageName");
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

        httpServer.addPath("/file/", (request, response) -> {
            String path = request.getPathParam("path");
            File file = new File(path);
            if (file.exists()) {
                response.writeFile(file);
            } else {
                response.write404();
            }
        });

        httpServer.addPath("/drawable", (request, response) -> {
            String name = request.getPathParam("name");
            Resources r = App.getInstance().getResources();
            int resource = ImageUtils.getResource(name);
            String resourceName = ImageUtils.getResourceName(resource);
            String contentTypeByName = Response.getContentTypeByName(resourceName);
            InputStream is = r.openRawResource(resource);
            response.writeStream(is, contentTypeByName);
        });


        httpServer.addPath("/", (request, response) -> {
            String path = request.getRequestURL();
            String filePath = "web";
            if (StringUtils.isEmpty(path) || path.equals("/")) {
                filePath += "/lanshare.html";
            } else {
                filePath += path;
            }
            App instance = App.getInstance();
            InputStream open = instance.getAssets().open(filePath);
            byte[] bytes = IOUtil.readBytes(open);
            int i = filePath.lastIndexOf(".");
            if (i > 0) {
                String myMIMEType = FileUtil.getMyMIMEType(filePath.substring(i + 1));
                response.writeBytes(bytes, myMIMEType);
            }
        });

    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

}
