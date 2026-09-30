package com.fgsqw.lanshare.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.ApkIconDBUtil;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.file.*;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.pojo.message.MessageAudioContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.pojo.message.MessageUriContent;
import com.fgsqw.lanshare.pojo.network.MediaResult;
import com.fgsqw.lanshare.toast.T;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;

import static android.graphics.BitmapFactory.decodeResource;

@SuppressLint("Range")
public class FileSearchUtils {

    /**
     * 从SDCard加载图片
     **/
    @SuppressLint("Range")
    public static void loadImageForSDCard(final Context context, boolean refresh) {
        Lock stringLock = StringLockManager.getStringLock("loadImageForSDCard");
        stringLock.lock();
        try {
            if (!refresh) {
                return;
            }
            //由于扫描图片是耗时的操作，所以要在子线程处理。
            //扫描图片
            Uri mImageUri;
            mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver mContentResolver = context.getContentResolver();
            Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                            MediaStore.Images.Media._ID,
                            MediaStore.Images.Media.DATA,
                            MediaStore.Images.Media.DISPLAY_NAME,
                            MediaStore.Images.Media.DATE_ADDED,
                            MediaStore.Images.Media.MIME_TYPE,
                            MediaStore.Images.Media.SIZE
                    },
                    MediaStore.Images.Media.SIZE + " > 0",
                    null,
                    MediaStore.Images.Media.DATE_ADDED);
            List<MessageMediaContent> mediaInfos = new ArrayList<>();
            //读取扫描到的图片
            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    int index = 0;
                    // 图片唯一id
                    long mediaId = mCursor.getLong(index++);
                    // 获取图片的路径
                    String path = mCursor.getString(index++);
                    //获取图片名称
                    String name = mCursor.getString(index++);
                    //获取图片时间
                    long time = mCursor.getLong(index++);
                    //获取图片类型
                    String mimeType = mCursor.getString(index++);  //获取图片类型
                    long size = mCursor.getLong(index++);
                    //过滤未下载完成或者不存在的文件
                    if (size <= 0) continue;
                    MessageMediaContent mediaInfo = new MessageMediaContent();
                    mediaInfo.setName(name);
                    mediaInfo.setPath(path);
                    mediaInfo.setTime(time);
                    mediaInfo.setLength(size);
                    mediaInfo.setGif("image/gif".equals(mimeType));
                    mediaInfos.add(mediaInfo);
                    mediaInfo.setMediaId(mediaId);

                }
                mCursor.close();
            }
            Collections.reverse(mediaInfos);
            AnyData.mediaResult = splitFolder(context, mediaInfos, loadVideoForSDCard(context));
        } finally {
            stringLock.unlock();
        }
    }


    /**
     * 获取视频
     */
    public static List<MessageMediaContent> loadVideoForSDCard(final Context context) {
        long start = System.currentTimeMillis();
        //扫描图片
        Uri mImageUri;
        mImageUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = context.getContentResolver();
        Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Images.Media.SIZE,

                },
                MediaStore.Images.Media.SIZE + " > 0",
                null,
                MediaStore.Images.Media.DATE_ADDED);
        List<MessageMediaContent> mediaInfos = new ArrayList<>();
        //读取扫描到的视频
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                int index = 0;
                // 图片唯一id
                long mediaId = mCursor.getLong(index++);
                // 获取视频的路径
                String path = mCursor.getString(index++);
                //获取视频名称
                String name = mCursor.getString(index++);
                //获取视频时间
                long time = mCursor.getLong(index++);
                //获取视频类型
                long duration = mCursor.getLong(index++);
                if (duration <= 0) {
                    continue;
                }
                long size = mCursor.getLong(index++);
                MessageMediaContent mediaInfo = new MessageMediaContent();
                mediaInfo.setName(name);
                mediaInfo.setMediaId(mediaId);
                mediaInfo.setPath(path);
                mediaInfo.setTime(time);
                mediaInfo.setLength(size);
                mediaInfo.setGif(false);
                mediaInfo.setVideo(true);
                mediaInfo.setVideoTime(/*getVideoDuration(path)*/timeParse(duration));
                mediaInfos.add(mediaInfo);
            }
            mCursor.close();
        }
        double time = (System.currentTimeMillis() - start) / 1000D;
        Log.d("loadMusicForSDCard", "耗时: " + time + "s");
        return mediaInfos;
    }


    public static void loadMusicForSDCard(final Context context, boolean refresh) {
        Lock lock = StringLockManager.getStringLock("loadApp");
        lock.lock();
        try {
            long start = System.currentTimeMillis();
            if (!refresh) {
                return;
            }
            //扫描图片
            Uri mImageUri;
            mImageUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            ContentResolver mContentResolver = context.getContentResolver();
            Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DATE_ADDED,
                            MediaStore.Audio.Media.DURATION
                    },
                    MediaStore.Images.Media.SIZE + " > 0",
                    null,
                    null);
            List<MessageAudioContent> mediaInfos = new ArrayList<>();
            //读取扫描到的视频
            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    int index = 0;
                    // 图片唯一id
                    long mediaId = mCursor.getLong(index++);
                    // 获取视频的路径
                    String path = mCursor.getString(index++);
                    //获取视频名称
                    String name = mCursor.getString(index++);
                    //获取视频时间
                    long time = mCursor.getLong(index++);
                    long duration = mCursor.getLong(index++);
                    if (duration <= 0) {
                        continue;
                    }
                    //过滤未下载完成或者不存在的文件
                    if (!"downloading".equals(getExtensionName(path)) && checkImgExists(path)) {
                        long length = new File(path).length();
                        if (length <= 0) continue;
                        MessageAudioContent mediaInfo = new MessageAudioContent();
                        mediaInfo.setName(name);
                        mediaInfo.setMediaId(mediaId);
                        mediaInfo.setPath(path);
                        mediaInfo.setTime(time);
                        mediaInfo.setLength(length);
                        mediaInfo.setAudioTime(timeParse(duration));
//                        mediaInfo.setMusicTime(getAudioPlayTime(path));
                        mediaInfos.add(mediaInfo);
                    }
                }
                mCursor.close();
            }
            double time = (System.currentTimeMillis() - start) / 1000D;
            Log.d("loadMusicForSDCard", "耗时: " + time + "s");
            AnyData.musicInfoList = mediaInfos;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


    /**
     * 加载apk列表
     */
    public static void loadApp(Context context, boolean refresh) {
        Lock lock = StringLockManager.getStringLock("loadApp");
        lock.lock();
        try {
            if (!refresh) {
                return;
            }
            ApkIconDBUtil apkIconDBUtil = new ApkIconDBUtil(context);
            List<PackageInfo> packages;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packages = context.getPackageManager().getInstalledPackages(PackageManager.PackageInfoFlags.of(0));
            } else {
                packages = context.getPackageManager().getInstalledPackages(0);
            }
            List<MessageApkContent> apkInfoList = new ArrayList<>();
            PrefUtil prefUtil = App.getPrefUtil();
            boolean flag = prefUtil.getBoolean(PreConfig.DISPLAY_SYSTEM_APP, false);
            for (PackageInfo packageInfo : packages) {
                if (flag || (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    MessageApkContent apkInfo = new MessageApkContent();
                    String packageName = packageInfo.packageName;
                    long length = new File(packageInfo.applicationInfo.sourceDir).length();
                    apkInfo.setName(packageInfo.applicationInfo.loadLabel(context.getPackageManager()) + ".apk");
                    apkInfo.setLength(length);
                    apkInfo.setPath(packageInfo.applicationInfo.sourceDir);
                    apkInfo.setPackageName(packageName);
                    apkInfo.setVersionCode(packageInfo.versionCode);
                    apkInfo.setVersionName(packageInfo.versionName);
                    byte[] png = apkIconDBUtil.queryIconByPackageName(packageName);
                    if (png == null) {
                        Drawable drawable = packageInfo.applicationInfo.loadIcon(context.getPackageManager());
                        Bitmap bitmap = ImageUtils.drawableToBitmap(drawable);
                        png = ImageUtils.bitmap2PngBytes(bitmap);
                        apkIconDBUtil.addIcon(packageName, apkInfo.getPath(), png);
                    }
                    apkInfo.setIcon(png);
                    apkInfoList.add(apkInfo);
                }
            }
            // 忽略大小写排序软件
            Collections.sort(apkInfoList, (a, b) ->
                    String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName())
            );
            AnyData.apkFileList = apkInfoList;
        } finally {
            lock.unlock();
        }
    }


    public static List<String> getPackageNames(Context context) {
        PackageManager pm = context.getPackageManager();
        List<String> packageNameList = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN); // 动作匹配
        intent.addCategory(Intent.CATEGORY_LAUNCHER); // 类别匹配
        List<ResolveInfo> mApps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : mApps) {
            packageNameList.add(resolveInfo.activityInfo.packageName);
        }
        return packageNameList;
    }

    /**
     * 扫描文件夹下的文件并返回总文件大小
     *
     * @param path         文件夹路径
     * @param fileInfolist 扫描储存list
     * @return 总文件大小
     */
    public static long scanPathFileSize(File path, List<MessageFileContent> fileInfolist) {
        long totalSize = 0;
        if (path.exists() && path.canRead()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                if (files != null) {
                    for (File file : files) {
                        totalSize += scanPathFileSize(file, fileInfolist);
                    }
                }
            } else if (path.isFile() && path.length() > 0) {
                MessageFileContent content = new MessageFileContent();
                content.setName(path.getName());
                content.setPath(path.getPath());
                content.setLength(path.length());
                fileInfolist.add(content);
                totalSize += content.getLength();
            }
        }
        return totalSize;
    }

    /**
     * 递归创建 FileItem 对象
     * @param file 文件或文件夹
     * @param relativePath 相对路径
     */
    public static void createFileItem(File file, String relativePath, MessageFolderContent fileItem, List<MessageFileContent> fileItems) {
        if (file.isDirectory()) {
            MessageFolderContent item = new MessageFolderContent();
            // 文件名使用相对路径
            item.setName(relativePath.isEmpty() ? file.getName() : relativePath);
            item.setPath(file.getAbsolutePath());
            // 文件夹类型为 0
            // 递归处理子文件和文件夹
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    // 构建子项的相对路径
                    String childRelativePath = relativePath.isEmpty() ?
                            child.getName() : relativePath + File.separator + child.getName();
                    createFileItem(child, childRelativePath, fileItem, fileItems);
                }
            }
        } else {
            MessageFileContent item = new MessageFileContent();
            // 文件名使用相对路径
            item.setName(relativePath.isEmpty() ? file.getName() : relativePath);
            item.setPath(file.getAbsolutePath());
            // 文件类型为 1
            item.setLength(file.length());
            fileItems.add(item);
            fileItem.setLength(fileItem.getLength() + item.getLength());
        }
    }

    public static long scanUriPathFileSize(DocumentFile documentFile, String path, List<MessageFileContent> fileInfolist) {
        long totalSize = 0;
        if (documentFile.exists() && documentFile.canRead()) {
            if (documentFile.isDirectory()) {
                DocumentFile[] documentFiles = documentFile.listFiles();
                for (DocumentFile file : documentFiles) {
                    totalSize += scanUriPathFileSize(file, path + "/" + documentFile.getName(), fileInfolist);
                }
            } else if (documentFile.isFile() && documentFile.length() > 0) {
                MessageUriContent fileInfo = new MessageUriContent(documentFile.getUri());
                fileInfo.setName(documentFile.getName());
                fileInfo.setLength(documentFile.length());
                fileInfo.setPath(path + "/" + documentFile.getName());
                fileInfolist.add(fileInfo);
                totalSize += fileInfo.getLength();
            }
        }
        return totalSize;
    }


    /**
     * Java文件操作 获取文件扩展名
     */
    public static String getExtensionName(String filename) {
        if (filename != null && !filename.isEmpty()) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }


    /**
     * 检查图片是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     */
    private static boolean checkImgExists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 把图片按文件夹拆分，第一个文件夹保存所有的图片
     */
    private static MediaResult splitFolder(Context context, List<MessageMediaContent> photoList, List<MessageMediaContent> videoList) {
        MediaResult mediaResult = new MediaResult();
        List<PhotoFolder> folders = new ArrayList<>();
        List<MessageMediaContent> allMedia = new ArrayList<>();
        Map<Long, MessageMediaContent> mediaInfoMap = new HashMap<>();
        Map<Integer, MessageMediaContent> allMediaMap = new HashMap<>();
        allMedia.addAll(photoList);
        allMedia.addAll(videoList);

        PhotoFolder allImages = new PhotoFolder(context.getString(R.string.all_images), photoList);
        allImages.setFolderPath("all_images");
        PhotoFolder allVideos = new PhotoFolder(context.getString(R.string.all_videos), videoList);
        allVideos.setFolderPath("all_videos");

        folders.add(allImages);
        folders.add(allVideos);
        int index = 0;
        if (!allMedia.isEmpty()) {
            for (MessageMediaContent mediaInfo : allMedia) {
                mediaInfoMap.put(mediaInfo.getMediaId(), mediaInfo);
                mediaInfo.setIndex(index);
                allMediaMap.put(index++, mediaInfo);
                String path = new File(mediaInfo.getPath()).getParent();
//                String name = getFolderName(path);
                if (path != null && !path.isEmpty()) {
                    PhotoFolder folder = getFolder(path, folders);
                    folder.addImage(mediaInfo);
                }
            }
        }
        if (!videoList.isEmpty()) {
            for (MessageMediaContent mediaInfo : videoList) {
                mediaInfoMap.put(mediaInfo.getMediaId(), mediaInfo);
                mediaInfo.setIndex(index);
                allMediaMap.put(index++, mediaInfo);
            }
        }
        mediaResult.setMediaInfoMap(mediaInfoMap);
        mediaResult.setAllMedia(allMedia);
        mediaResult.setAllMediaMap(allMediaMap);
        mediaResult.setmFolders(folders);
        return mediaResult;
    }


    @Nullable
    static Activity findActivity(@NonNull Context context) {
        do {
            if (context instanceof Activity) {
                return (Activity) context;
            } else if (context instanceof ContextWrapper) {
                // android.content.ContextWrapper
                // android.content.MutableContextWrapper
                // android.support.v7.view.ContextThemeWrapper
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        } while (context != null);
        return null;
    }

    public static List<MessageFileContent> getFileList(MessageFileContent f, boolean showHiddenFiles, int sortMethod, Context context) {
        // 如果File为null则默认为跟目录
        if (f == null) {
            f = new MessageFolderContent();
            f.setPath(PermissionsUtils.ROOT_PATH);
        }
        Comparator<MessageFileContent> comparator;
        if (sortMethod == 0) {
            comparator = MessageFileContent::compareTo;
        } else if (sortMethod == 1) {
            comparator = (o1, o2) -> Long.compare(o1.getLength(), o2.getLength());
        } else if (sortMethod == 2) {
            comparator = (o1, o2) -> Double.compare(Math.signum(o1.getTime() - o2.getTime()), 0);
        } else if (sortMethod == 3) {
            comparator = (o1, o2) -> o2.compareTo(o1);
        } else if (sortMethod == 4) {
            comparator = (o1, o2) -> Long.compare(o2.getLength(), o1.getLength());
        } else if (sortMethod == 5) {
            comparator = (o1, o2) -> Double.compare(Math.signum(o2.getTime() - o1.getTime()), 0);
        } else {
            comparator = MessageFileContent::compareTo;
        }
        Resources res = context.getResources();
        List<MessageFileContent> fileList = new ArrayList<>();
        List<MessageFileContent> dirList = new ArrayList<>();
        if (VersionUtils.isAfterAndroid13() && PermissionsUtils.isAndroidData(f.getPath())) {
            boolean isGet = XXPermissions.isGranted(context, Permission.MANAGE_EXTERNAL_STORAGE);
            //已有权限则返回
            if (!isGet) {
                if (!(context instanceof Activity)) {
                    throw new RuntimeException(context.getString(R.string.please_manually_authorize_this_folder_in_the_software));
                }
                XXPermissions.with(context)
                        // 申请单个权限
                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                        // 设置不触发错误检测机制（局部设置）
                        .unchecked()
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
//                                T.s("成功");
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                T.s((R.string.please_authorize_file_access_permission_or_else_software));
                            }
                        });
                return fileList;
            }
            List<String> packageNames = FileSearchUtils.getPackageNames(context);
            for (String packageName : packageNames) {
                File file = new File(PermissionsUtils.ANDROID_DATA + "/" + packageName);
                if (file.exists()) {
                    String name = file.getName();
                    Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                    MessageFolderContent fileSource = new MessageFolderContent();
                    fileSource.setName(mUtil.stringSize(name, 20));
                    fileSource.setPath(file.getPath());
                    fileSource.setPreviewBitmap(bmp);
                    fileSource.setIsPreView(false);
                    fileSource.setTime(file.lastModified());
//                    fileSource.setFile(false);
                    dirList.add(fileSource);
                }
            }
        } else if (PermissionsUtils.isSubAndroidData(f.getPath())) {
            Uri uri = PermissionsUtils.path2Uri(f.getPath());
            //获取权限,没有权限返回null有权限返回授权uri字符串
            String existsPermission = PermissionsUtils.existsGrantedUriPermission(uri, context);
            if (existsPermission == null) {
                if (!(context instanceof Activity)) {
                    throw new RuntimeException("请手动在软件中授权此文件夹");
                }
                PermissionsUtils.goApplyUriPermissionPage(uri, (Activity) context);
                return fileList;
            }
            Uri targetUri = Uri.parse(existsPermission
                    + uri.toString().replaceFirst(PermissionsUtils.URI_PERMISSION_REQUEST_COMPLETE_PREFIX, ""));
            DocumentFile doc = DocumentFile.fromTreeUri(context, targetUri);
            Objects.requireNonNull(doc, "doc is null");

            if (doc.isDirectory()) {
                DocumentFile[] documentFiles = doc.listFiles();
                for (DocumentFile documentFile : documentFiles) {
                    String name = documentFile.getName();
                    if (StringUtils.isEmpty(name)) {
                        continue;
                    }
                    if (documentFile.isDirectory()) {
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                        MessageUriContent uriFileInfo = new MessageUriContent(documentFile.getUri());
                        uriFileInfo.setName(mUtil.stringSize(name, 20));
                        uriFileInfo.setPath(f.getPath() + "/" + name);
                        uriFileInfo.setPreviewBitmap(bmp);
                        uriFileInfo.setIsPreView(false);
                        uriFileInfo.setTime(documentFile.lastModified());
                        uriFileInfo.setFile(false);
                        dirList.add(uriFileInfo);
                    } else if (documentFile.isFile()) {
                        MessageUriContent uriFileInfo = new MessageUriContent(documentFile.getUri());
                        uriFileInfo.setName(name);
                        uriFileInfo.setPath(f.getPath() + "/" + name);
                        uriFileInfo.setPreviewBitmap(decodeResource(res, R.drawable.ic_file_file));
                        uriFileInfo.setIsPreView(false);
                        uriFileInfo.setTime(documentFile.lastModified());
                        uriFileInfo.setLength(documentFile.length());
                        uriFileInfo.setFile(true);
                        fileList.add(uriFileInfo);
                    }
                }
            }

        } else if (f instanceof MessageFolderContent) {  // 如果是文件夹
            File fe = new File(f.getPath());
            if (fe.canRead()) {  // 如果能读取
                // 保存当前路径
                File[] files = fe.listFiles();
                // 排序
                for (File file : files) {
                    // 如果是文件夹
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                        MessageFolderContent fileSource = new MessageFolderContent();
                        fileSource.setName(mUtil.stringSize(name, 20));
                        fileSource.setPath(file.getPath());
                        fileSource.setPreviewBitmap(bmp);
                        fileSource.setIsPreView(false);
                        fileSource.setTime(file.lastModified());
//                        fileSource.setFile(false);
                        dirList.add(fileSource);
                        // 如果是文件
                    } else if (file.isFile()) {
                        String name = file.getName();
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Bitmap bmp = null;
                        String suffixName = name.substring(name.lastIndexOf(".") + 1);
                        boolean isPreView = false;
                        if (suffixName.equalsIgnoreCase("txt")) {
                            bmp = decodeResource(res, R.drawable.ic_txts_file);
                        } else if (suffixName.equalsIgnoreCase("jpg")//图片及视频文件
                                || suffixName.equalsIgnoreCase("png")
                                || suffixName.equalsIgnoreCase("jpeg")
                                || suffixName.equalsIgnoreCase("gif")
                                || suffixName.equalsIgnoreCase("mp4")
                        ) {
                            isPreView = true;
                        } else if (suffixName.equalsIgnoreCase("zip")) {//压缩包文件
                            bmp = decodeResource(res, R.drawable.ic_zip_file);
                        } else if (suffixName.equalsIgnoreCase("apk")) {//apk文件
                           /* Drawable drawable = FileUtil.getApkIcon(getContext(), file.getPath());
                            if (drawable != null) {
                                bmp = ImageUtils.drawableToBitmap(drawable);
                            } else {
                                bmp = decodeResource(res, R.drawable.ic_null_android_file);
                            }*/
                            bmp = decodeResource(res, R.drawable.ic_null_android_file);
                        } else if (suffixName.equalsIgnoreCase("java")) {//java文件
                            bmp = decodeResource(res, R.drawable.ic_coder_file);
                        } else {
                            bmp = decodeResource(res, R.drawable.ic_file_file);
                        }
                        MessageFileContent fileSource = new MessageFileContent();
                        fileSource.setName(name);
                        fileSource.setPath(file.getPath());
                        fileSource.setPreviewBitmap(bmp);
                        fileSource.setIsPreView(isPreView);
                        fileSource.setTime(file.lastModified());
                        fileSource.setLength(file.length());
//                        fileSource.setFile(true);
                        fileList.add(fileSource);
                    }
                }
            } else {
                // 不能读取文件夹
                throw new RuntimeException(context.getString(R.string.this_folder_cannot_be_read));
            }
        }
        // 排序文件
        Collections.sort(fileList, comparator);
        Collections.sort(dirList, comparator);
        dirList.addAll(fileList);
        // 添加返回上一级在顶部
        MessageFileContent fileSource = new MessageFileContent();
        fileSource.setName("...");
        fileSource.setPreviewBitmap(decodeResource(res, R.drawable.ic_folder_upload));
        fileSource.setIsPreView(false);
        fileSource.setPath(f.getPath());
        dirList.add(0, fileSource);
        return dirList;
    }

    /**
     * 根据图片路径，获取图片文件夹名称
     *
     * @param path
     * @return
     */
    public static String getFolderName(String path) {
        if (path != null && !path.isEmpty()) {//判断字符是否为空
            String[] strings = path.split(File.separator);
            if (strings.length >= 2) {
                return strings[strings.length - 2];
            }
        }
        return "";
    }


    private static PhotoFolder getFolder(String folderPath, List<PhotoFolder> folders) {
        if (!folders.isEmpty()) {
            for (PhotoFolder folder : folders) {
                if (folderPath.equals(folder.getFolderPath())) {
                    return folder;
                }
            }
        }
        PhotoFolder newFolder = new PhotoFolder(folderPath, getFolderName(folderPath));
        folders.add(newFolder);
        return newFolder;
    }

    /**
     * 获取音频时长
     *
     * @return 返回处理好的音频时长
     */
  /*  static public String getAudioPlayTime(String source) {
        MediaPlayer mediapalyer = new MediaPlayer();
        long time = 0;
        try {
            mediapalyer.setDataSource(source);
            mediapalyer.prepare();
            time = mediapalyer.getDuration();
            mediapalyer.release();
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error("error", e);
        }
        return timeParse(time);
    }*/

    /**
     * long时间转换为正常时间
     *
     * @return
     */
    public static String timeParse(long duration) {
        String time = "";
        long minute = duration / 60000;
        long seconds = duration % 60000;
        long second = Math.round((float) seconds / 1000);
        if (minute < 10) {
            time += "0";
        }
        time += minute + ":";
        if (second < 10) {
            time += "0";
        }
        time += second;
        return time;
    }

    /**
     * 读取视频
     *
     * @param videoPath
     * @return
     */
   /* public static String getVideoDuration(String videoPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoPath);
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try {
            retriever.release();
            if (duration != null) {
                return timeParse(Long.parseLong(duration));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "0";
    }*/

}
