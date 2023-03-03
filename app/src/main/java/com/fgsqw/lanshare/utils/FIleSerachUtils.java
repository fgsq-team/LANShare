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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.ApkIconDBUtil;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.*;
import com.fgsqw.lanshare.toast.T;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.graphics.BitmapFactory.decodeResource;

public class FIleSerachUtils {


    private static final Lock loadMediaLock = new ReentrantLock();


    /**
     * 从SDCard加载图片
     *
     * @param context
     */
    public static void loadImageForSDCard(final Context context, boolean refresh) {
        loadMediaLock.lock();
        try {
            if (refresh) {
                //由于扫描图片是耗时的操作，所以要在子线程处理。
                //扫描图片
                Uri mImageUri;
                mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = context.getContentResolver();
                Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                                MediaStore.Images.Media.DATA,
                                MediaStore.Images.Media.DISPLAY_NAME,
                                MediaStore.Images.Media.DATE_ADDED,
                                MediaStore.Images.Media._ID,
                                MediaStore.Images.Media.MIME_TYPE,
                        },
                        null,
                        null,
                        MediaStore.Images.Media.DATE_ADDED);
                List<MediaInfo> mediaInfos = new ArrayList<>();
                //读取扫描到的图片
                if (mCursor != null) {
                    while (mCursor.moveToNext()) {
                        // 获取图片的路径
                        String path = mCursor.getString(
                                mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        //获取图片名称
                        String name = mCursor.getString(
                                mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                        //获取图片时间
                        long time = mCursor.getLong(
                                mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
                        //获取图片类型
                        String mimeType = mCursor.getString(
                                mCursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
                        //过滤未下载完成或者不存在的文件
                        if (!"downloading".equals(getExtensionName(path)) && checkImgExists(path)) {
                            long length = new File(path).length();
                            if (length <= 0) continue;
                            MediaInfo mediaInfo = new MediaInfo();
                            mediaInfo.setName(name);
                            mediaInfo.setPath(path);
                            mediaInfo.setTime(time);
                            mediaInfo.setLength(length);
                            mediaInfo.setGif("image/gif".equals(mimeType));
                            mediaInfos.add(mediaInfo);
                        }
                    }
                    mCursor.close();
                }
                Collections.reverse(mediaInfos);
                AnyData.mediaResult = splitFolder(mediaInfos, loadVideoForSDCard(context));
            }
        } finally {
            loadMediaLock.unlock();
        }
    }


    /**
     * 获取视频
     *
     * @param context
     */
    public static List<MediaInfo> loadVideoForSDCard(final Context context) {
        //由于扫描图片是耗时的操作，所以要在子线程处理。
        //扫描图片
        Uri mImageUri;
        mImageUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = context.getContentResolver();
        Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.MIME_TYPE},
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED);

        List<MediaInfo> mediaInfos = new ArrayList<>();

        //读取扫描到的视频
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                // 获取视频的路径
                String path = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                //获取视频名称
                String name = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                //获取视频时间
                long time = mCursor.getLong(
                        mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));

                //获取视频类型
                String mimeType = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));

                //过滤未下载完成或者不存在的文件
                if (!"downloading".equals(getExtensionName(path)) && checkImgExists(path)) {
                    long length = new File(path).length();
                    if (length <= 0) continue;
                    MediaInfo mediaInfo = new MediaInfo();
                    mediaInfo.setName(name);
                    mediaInfo.setPath(path);
                    mediaInfo.setTime(time);
                    mediaInfo.setLength(length);
                    mediaInfo.setGif(false);
                    mediaInfo.setVideo(true);
                    mediaInfo.setVideoTime(getAudioPlayTime(path));
                    mediaInfos.add(mediaInfo);
                }
            }
            mCursor.close();
        }
        return mediaInfos;
    }


    /**
     * 加载apk列表线程锁
     */
    private static final Lock loadApkLock = new ReentrantLock();


    /**
     * 加载apk列表
     */
    public static void loadApp(Context context, boolean refresh) {
        loadApkLock.lock();
        try {
            if (refresh) {
                ApkIconDBUtil apkIconDBUtil = new ApkIconDBUtil(context);
                List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
                List<ApkInfo> apkInfoList = new ArrayList<>();
                PrefUtil prefUtil = App.getPrefUtil();
                boolean flag = prefUtil.getBoolean(PreConfig.DISPLAY_SYSTEM_APP, false);
                for (int i = 0; i < packages.size(); i++) {
                    PackageInfo packageInfo = packages.get(i);
                    if (flag || (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        ApkInfo apkInfo = new ApkInfo();
                        String packageName = packageInfo.packageName;
                        long length = new File(packageInfo.applicationInfo.sourceDir).length();
                        apkInfo.setName(packageInfo.applicationInfo.loadLabel(context.getPackageManager()) + ".apk");
                        apkInfo.setLength(length);
                        apkInfo.setPath(packageInfo.applicationInfo.sourceDir);
                        apkInfo.setPackageName(packageName);
                        byte[] png = apkIconDBUtil.queryByPackageName(packageName);
                        if (png == null) {
                            Drawable drawable = packageInfo.applicationInfo.loadIcon(context.getPackageManager());
                            Bitmap bitmap = ImageUtils.drawableToBitmap(drawable);
                            png = ImageUtils.bitmap2PngBytes(bitmap);
                            apkIconDBUtil.addIcon(packageName, png);
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
            }
        } finally {
            loadApkLock.unlock();
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
    public static long scanPathFiles(File path, List<FileInfo> fileInfolist) {
        long totalSize = 0;
        if (path.exists() && path.canRead()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (File file : files) {
                    totalSize += scanPathFiles(file, fileInfolist);
                }
            } else if (path.isFile() && path.length() > 0) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setName(path.getName());
                fileInfo.setPath(path.getPath());
                fileInfo.setLength(path.length());
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
        if (filename != null && filename.length() > 0) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }


    /**
     * 检查图片是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     *
     * @param filePath
     * @return
     */
    private static boolean checkImgExists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 把图片按文件夹拆分，第一个文件夹保存所有的图片
     *
     * @return
     */

    private static MediaResult splitFolder(List<MediaInfo> photoList, List<MediaInfo> videoList) {
        MediaResult mediaResult = new MediaResult();
        List<PhotoFolder> folders = new ArrayList<>();
        List<MediaInfo> allMedia = new ArrayList<>();

        Map<Integer, MediaInfo> allMediaMap = new HashMap<>();
        allMedia.addAll(photoList);
        allMedia.addAll(videoList);

        folders.add(new PhotoFolder("所有图片", photoList));
        folders.add(new PhotoFolder("所有视频", videoList));

        int index = 0;
        if (!allMedia.isEmpty()) {
            for (MediaInfo mediaInfo : allMedia) {
                mediaInfo.setIndex(index);
                allMediaMap.put(index++, mediaInfo);
                String path = mediaInfo.getPath();
                String name = getFolderName(path);
                if (name != null && name.length() > 0) {
                    PhotoFolder folder = getFolder(name, folders);
                    folder.addImage(mediaInfo);
                }
            }
        }

        if (!videoList.isEmpty()) {
            for (MediaInfo mediaInfo : videoList) {
                mediaInfo.setIndex(index);
                allMediaMap.put(index++, mediaInfo);
            }
        }

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

    @SuppressLint("SetTextI18n")
    public static List<FileSource> getFileList(File f, boolean showHiddenFiles, Context context) {
        // 如果File为null则默认为跟目录
        if (f == null) {
            f = new File("/");
        }
        List<FileSource> fileList = new ArrayList<>();
        List<FileSource> paths = new ArrayList<>();
        if (VersionUtils.isAndroid11() && PermissionsUtils.isAndroidData(f.getPath())) {
            boolean isGet = XXPermissions.isGranted(context, Permission.MANAGE_EXTERNAL_STORAGE);
            //已有权限则返回
            if (!isGet) {
                if(!(context instanceof Activity)){
                    T.s("请手动在软件中授权此文件夹");
                    return fileList;
                }
                XXPermissions.with(context)
                        // 申请单个权限
                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                        // 设置不触发错误检测机制（局部设置）
                        .unchecked()
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                T.s("成功");
                            }
                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                if (never) {
                                    //如果是被永久拒绝就跳转到应用权限系统设置页面
                                    XXPermissions.startPermissionActivity(context, permissions);
                                } else {
                                }

                            }
                        });
                return fileList;
            }
            Resources res = context.getResources();
            FileSource fileSource = new FileSource();
            fileSource.setName("...");
            fileSource.setPreView(decodeResource(res, R.drawable.ic_folder_upload));
            fileSource.setIsPreView(false);
            fileList.add(fileSource);
            List<String> packageNames = FIleSerachUtils.getPackageNames(context);
            for (String packageName : packageNames) {
                File file = new File(PermissionsUtils.ANDROID_DATA + "/" + packageName);
                if (file.exists()) {
                    String name = file.getName();
                    Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                    fileSource = new FileSource();
                    fileSource.setName(mUtil.StringSize(name, 20));
                    fileSource.setPath(file.getPath());
                    fileSource.setPreView(bmp);
                    fileSource.setIsPreView(false);
                    fileSource.setTime(file.lastModified());
                    fileSource.setFile(false);
                    fileList.add(fileSource);
                }
            }
        } else if (PermissionsUtils.isSubAndroidData(f.getPath())) {
            Uri uri = PermissionsUtils.path2Uri(f.getPath());
            //获取权限,没有权限返回null有权限返回授权uri字符串
            String existsPermission = PermissionsUtils.existsGrantedUriPermission(uri, context);
            if (existsPermission == null) {
                if(!(context instanceof Activity)){
                    T.s("请手动在软件中授权此文件夹");
                    return fileList;
                }
                PermissionsUtils.goApplyUriPermissionPage(uri, (Activity) context);
                return fileList;
            }
            Uri targetUri = Uri.parse(existsPermission + uri.toString()
                    .replaceFirst(PermissionsUtils.URI_PERMISSION_REQUEST_COMPLETE_PREFIX, ""));
            //Mtools.log(targetUri);
            DocumentFile rootDocumentFile = DocumentFile.fromSingleUri(context, targetUri);
            Objects.requireNonNull(rootDocumentFile, "rootDocumentFile is null");
            //创建一个 DocumentFile表示以给定的 Uri根的文档树。其实就是获取子目录的权限
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, targetUri);
            Objects.requireNonNull(pickedDir, "pickedDir is null");
            DocumentFile[] documentFiles = pickedDir.listFiles();
            Resources res = context.getResources();
            FileSource fileSource = new FileSource();
            fileSource.setName("...");
            fileSource.setPreView(decodeResource(res, R.drawable.ic_folder_upload));
            fileSource.setIsPreView(false);
            fileList.add(fileSource);
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
                    fileSource = new FileSource();
                    fileSource.setName(mUtil.StringSize(name, 20));
                    fileSource.setPath(f.getPath() + "/" + name);
                    fileSource.setPreView(bmp);
                    fileSource.setIsPreView(false);
                    fileSource.setTime(documentFile.lastModified());
                    fileSource.setFile(false);
                    fileList.add(fileSource);
                } else if (documentFile.isFile()) {
                    fileSource = new FileSource();
                    fileSource.setName(name);
                    fileSource.setPath(f.getPath() + "/" + name);
                    fileSource.setPreView(decodeResource(res, R.drawable.ic_file_file));
                    fileSource.setIsPreView(false);
                    fileSource.setTime(documentFile.lastModified());
                    fileSource.setLength(documentFile.length());
                    fileSource.setFile(true);
                    paths.add(fileSource);
                }
            }
            fileList.addAll(paths);
        } else if (f.isDirectory()) {  // 如果是文件夹
            if (f.canRead()) {  // 如果能读取
                // 保存当前路径
                File[] files = f.listFiles();
                // 排序
                Arrays.sort(files);
                Resources sres = context.getResources();
                FileSource fileSource = new FileSource();
                fileSource.setName("...");
                fileSource.setPreView(decodeResource(sres, R.drawable.ic_folder_upload));
                fileSource.setIsPreView(false);
                fileList.add(fileSource);
                for (File file : files) {
                    // 如果是文件夹
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Resources res = context.getResources();
                        Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                        fileSource = new FileSource();
                        fileSource.setName(mUtil.StringSize(name, 20));
                        fileSource.setPath(file.getPath());
                        fileSource.setPreView(bmp);
                        fileSource.setIsPreView(false);
                        fileSource.setTime(file.lastModified());
                        fileSource.setFile(false);
                        fileList.add(fileSource);
                        // 如果是文件
                    } else if (file.isFile()) {
                        String name = file.getName();
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Resources res = context.getResources();
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
                        fileSource = new FileSource();
                        fileSource.setName(name);
                        fileSource.setPath(file.getPath());
                        fileSource.setPreView(bmp);
                        fileSource.setIsPreView(isPreView);
                        fileSource.setTime(file.lastModified());
                        fileSource.setLength(file.length());
                        fileSource.setFile(true);
                        paths.add(fileSource);
                    }
                }
                fileList.addAll(paths);
            } else {
                // 不能读取文件夹
                T.s("此文件夹不能被读取");
            }
        }
        return fileList;
    }

    /**
     * 根据图片路径，获取图片文件夹名称
     *
     * @param path
     * @return
     */
    public static String getFolderName(String path) {
        if (path != null && path.length() > 0) {//判断字符是否为空
            String[] strings = path.split(File.separator);
            if (strings.length >= 2) {
                return strings[strings.length - 2];
            }
        }
        return "";
    }


    private static PhotoFolder getFolder(String name, List<PhotoFolder> folders) {
        if (!folders.isEmpty()) {
            int size = folders.size();
            for (int i = 0; i < size; i++) {
                PhotoFolder folder = folders.get(i);
                if (name.equals(folder.getName())) {
                    return folder;

                }
            }
        }
        PhotoFolder newFolder = new PhotoFolder(name);
        folders.add(newFolder);
        return newFolder;
    }

    /**
     * 获取音频时长
     *
     * @param source
     * @return 返回处理好的音频时长
     */
    static public String getAudioPlayTime(String source) {
        MediaPlayer mediapalyer = new MediaPlayer();
        long time = 0;
        try {
            mediapalyer.setDataSource(source);
            mediapalyer.prepare();
            time = mediapalyer.getDuration();//获得了视频的时长（以毫秒为单位）
            mediapalyer.release();
            mediapalyer = null;
        } catch (Exception e) {

        }
        return timeParse(time);
    }

    /**
     * long时间转换为正常时间
     *
     * @param duration
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

}
