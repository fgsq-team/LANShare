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
import com.fgsqw.lanshare.pojo.file.PhotoFolder;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.pojo.network.MediaResult;
import com.fgsqw.lanshare.toast.T;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;

import static android.graphics.BitmapFactory.decodeResource;

/**
 * 设备数据扫描器
 * <p>负责扫描设备上的媒体文件、已安装应用和文件目录数据</p>
 * <p>主要功能:</p>
 * <ul>
 *   <li>扫描设备图片和视频,按文件夹分组</li>
 *   <li>扫描设备音频文件</li>
 *   <li>扫描已安装应用列表(含图标缓存)</li>
 *   <li>浏览文件目录(支持 SAF 框架)</li>
 * </ul>
 *
 * @author fgsq
 * @version 2.0
 */
@SuppressLint("Range")
public class DeviceDataScanner {

    /** 日志标签 */
    private static final String TAG = "DeviceDataScanner";

    /** 不允许实例化 */
    private DeviceDataScanner() {
    }

    // ==================== 媒体扫描 ====================

    /**
     * 扫描设备图片
     * <p>从 MediaStore 查询所有图片,并与视频合并后按文件夹分组</p>
     *
     * @param context 上下文
     * @param refresh 是否强制刷新(为 false 时直接返回)
     */
    @SuppressLint("Range")
    public static void scanImages(final Context context, boolean refresh) {
        Lock stringLock = StringLockManager.getStringLock("scanImages");
        stringLock.lock();
        try {
            if (!refresh) {
                return;
            }
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Images.Media._ID,
                            MediaStore.Images.Media.DATA,
                            MediaStore.Images.Media.DISPLAY_NAME,
                            MediaStore.Images.Media.DATE_ADDED,
                            MediaStore.Images.Media.MIME_TYPE,
                            MediaStore.Images.Media.SIZE
                    },
                    MediaStore.Images.Media.SIZE + " > 0",
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            );

            List<MessageMediaContent> imageList = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int index = 0;
                    long mediaId = cursor.getLong(index++);
                    String path = cursor.getString(index++);
                    String name = cursor.getString(index++);
                    long time = cursor.getLong(index++);
                    String mimeType = cursor.getString(index++);
                    long size = cursor.getLong(index++);

                    if (size <= 0) {
                        continue;
                    }

                    MessageMediaContent mediaInfo = new MessageMediaContent();
                    mediaInfo.setName(name);
                    mediaInfo.setPath(path);
                    mediaInfo.setTime(time);
                    mediaInfo.setLength(size);
                    mediaInfo.setGif("image/gif".equals(mimeType));
                    mediaInfo.setMediaId(mediaId);
                    imageList.add(mediaInfo);
                }
                cursor.close();
            }

            List<MessageMediaContent> videoList = scanVideos(context);
            AnyData.mediaResult = groupMediaByFolder(context, imageList, videoList);
        } finally {
            stringLock.unlock();
        }
    }

    /**
     * 扫描设备视频
     *
     * @param context 上下文
     * @return 视频列表
     */
    public static List<MessageMediaContent> scanVideos(final Context context) {
        long startTime = System.currentTimeMillis();
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Images.Media.SIZE
                },
                MediaStore.Images.Media.SIZE + " > 0",
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        List<MessageMediaContent> videoList = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int index = 0;
                long mediaId = cursor.getLong(index++);
                String path = cursor.getString(index++);
                String name = cursor.getString(index++);
                long time = cursor.getLong(index++);
                long duration = cursor.getLong(index++);

                if (duration <= 0) {
                    continue;
                }

                long size = cursor.getLong(index++);
                MessageMediaContent mediaInfo = new MessageMediaContent();
                mediaInfo.setName(name);
                mediaInfo.setMediaId(mediaId);
                mediaInfo.setPath(path);
                mediaInfo.setTime(time);
                mediaInfo.setLength(size);
                mediaInfo.setGif(false);
                mediaInfo.setVideo(true);
                mediaInfo.setVideoTime(formatDuration(duration));
                videoList.add(mediaInfo);
            }
            cursor.close();
        }

        double elapsed = (System.currentTimeMillis() - startTime) / 1000D;
        Log.d(TAG, "scanVideos 耗时: " + elapsed + "s");
        return videoList;
    }

    /**
     * 扫描设备音频
     *
     * @param context 上下文
     * @param refresh 是否强制刷新
     */
    public static void scanAudioFiles(final Context context, boolean refresh) {
        Lock lock = StringLockManager.getStringLock("scanAudioFiles");
        lock.lock();
        try {
            long startTime = System.currentTimeMillis();
            if (!refresh) {
                return;
            }

            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DATE_ADDED,
                            MediaStore.Audio.Media.DURATION
                    },
                    MediaStore.Images.Media.SIZE + " > 0",
                    null,
                    null
            );

            List<MessageAudioContent> audioList = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int index = 0;
                    long mediaId = cursor.getLong(index++);
                    String path = cursor.getString(index++);
                    String name = cursor.getString(index++);
                    long time = cursor.getLong(index++);
                    long duration = cursor.getLong(index++);

                    if (duration <= 0) {
                        continue;
                    }

                    if (!"downloading".equals(getFileExtension(path)) && isFileExists(path)) {
                        long length = new File(path).length();
                        if (length <= 0) {
                            continue;
                        }
                        MessageAudioContent audioInfo = new MessageAudioContent();
                        audioInfo.setName(name);
                        audioInfo.setMediaId(mediaId);
                        audioInfo.setPath(path);
                        audioInfo.setTime(time);
                        audioInfo.setLength(length);
                        audioInfo.setAudioTime(formatDuration(duration));
                        audioList.add(audioInfo);
                    }
                }
                cursor.close();
            }

            double elapsed = (System.currentTimeMillis() - startTime) / 1000D;
            Log.d(TAG, "scanAudioFiles 耗时: " + elapsed + "s");
            AnyData.musicInfoList = audioList;
        } catch (Exception e) {
            Log.e(TAG, "scanAudioFiles error", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将媒体按文件夹分组
     * <p>创建"所有图片"和"所有视频"两个默认分组,再按实际路径拆分文件夹</p>
     *
     * @param context   上下文
     * @param imageList 图片列表
     * @param videoList 视频列表
     * @return 媒体分组结果
     */
    private static MediaResult groupMediaByFolder(Context context,
                                                  List<MessageMediaContent> imageList,
                                                  List<MessageMediaContent> videoList) {
        MediaResult mediaResult = new MediaResult();
        List<PhotoFolder> folders = new ArrayList<>();
        List<MessageMediaContent> allMedia = new ArrayList<>();
        Map<Long, MessageMediaContent> mediaInfoMap = new HashMap<>();
        Map<Integer, MessageMediaContent> allMediaMap = new HashMap<>();

        allMedia.addAll(imageList);
        allMedia.addAll(videoList);

        PhotoFolder allImagesFolder = new PhotoFolder(context.getString(R.string.all_images), imageList);
        allImagesFolder.setFolderPath("all_images");
        PhotoFolder allVideosFolder = new PhotoFolder(context.getString(R.string.all_videos), videoList);
        allVideosFolder.setFolderPath("all_videos");

        folders.add(allImagesFolder);
        folders.add(allVideosFolder);

        int index = 0;
        for (MessageMediaContent mediaInfo : allMedia) {
            mediaInfoMap.put(mediaInfo.getMediaId(), mediaInfo);
            mediaInfo.setIndex(index);
            allMediaMap.put(index++, mediaInfo);

            String parentPath = new File(mediaInfo.getPath()).getParent();
            if (parentPath != null && !parentPath.isEmpty()) {
                PhotoFolder folder = findOrCreateFolder(parentPath, folders);
                folder.addImage(mediaInfo);
            }
        }

        for (MessageMediaContent mediaInfo : videoList) {
            mediaInfoMap.put(mediaInfo.getMediaId(), mediaInfo);
            mediaInfo.setIndex(index);
            allMediaMap.put(index++, mediaInfo);
        }

        mediaResult.setMediaInfoMap(mediaInfoMap);
        mediaResult.setAllMedia(allMedia);
        mediaResult.setAllMediaMap(allMediaMap);
        mediaResult.setmFolders(folders);
        return mediaResult;
    }

    /**
     * 查找或创建文件夹
     *
     * @param folderPath 文件夹路径
     * @param folders    已有文件夹列表
     * @return 匹配或新创建的文件夹
     */
    private static PhotoFolder findOrCreateFolder(String folderPath, List<PhotoFolder> folders) {
        for (PhotoFolder folder : folders) {
            if (folderPath.equals(folder.getFolderPath())) {
                return folder;
            }
        }
        PhotoFolder newFolder = new PhotoFolder(folderPath, getParentFolderName(folderPath));
        folders.add(newFolder);
        return newFolder;
    }

    // ==================== 应用扫描 ====================

    /**
     * 扫描已安装应用
     * <p>遍历已安装应用包,收集应用信息并缓存图标到数据库</p>
     *
     * @param context 上下文
     * @param refresh 是否强制刷新
     */
    public static void scanInstalledApps(Context context, boolean refresh) {
        Lock lock = StringLockManager.getStringLock("scanInstalledApps");
        lock.lock();
        try {
            if (!refresh) {
                return;
            }

            long startTime = System.currentTimeMillis();
            ApkIconDBUtil iconDB = null;
            List<MessageApkContent> apkInfoList = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();

            try {
                List<PackageInfo> packages = getInstalledPackages(packageManager);
                if (packages == null || packages.isEmpty()) {
                    LLog.debug("No installed packages found");
                    AnyData.apkFileList = apkInfoList;
                    return;
                }

                PrefUtil prefUtil = App.getPrefUtil();
                boolean displaySystemApp = prefUtil.getBoolean(PreConfig.DISPLAY_SYSTEM_APP, false);
                iconDB = new ApkIconDBUtil(context);

                // 预加载缓存图标,避免循环中频繁查询数据库
                Map<String, byte[]> cachedIcons = preloadCachedIcons(iconDB, packages);
                LLog.debug("Start loading " + packages.size() + " packages, cached icons: " + cachedIcons.size());

                int successCount = 0;
                int skipCount = 0;
                int errorCount = 0;

                for (PackageInfo packageInfo : packages) {
                    try {
                        if (!displaySystemApp && isSystemApp(packageInfo)) {
                            skipCount++;
                            continue;
                        }

                        MessageApkContent apkInfo = buildApkInfo(packageInfo, packageManager, cachedIcons, iconDB);
                        apkInfoList.add(apkInfo);
                        successCount++;
                    } catch (Exception e) {
                        errorCount++;
                        LLog.error("Error processing package: " +
                                (packageInfo != null ? packageInfo.packageName : "unknown"), e);
                    }
                }

                LLog.debug(String.format("Load app completed: success=%d, skipped=%d, errors=%d, time=%dms",
                        successCount, skipCount, errorCount, System.currentTimeMillis() - startTime));

                // 按名称忽略大小写排序
                Collections.sort(apkInfoList, (a, b) ->
                        String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName())
                );
                AnyData.apkFileList = apkInfoList;

            } catch (Exception e) {
                LLog.error("Critical error while loading apps", e);
                AnyData.apkFileList = apkInfoList;
            } finally {
                if (iconDB != null) {
                    try {
                        iconDB.close();
                    } catch (Exception e) {
                        LLog.error("Error closing database", e);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取已安装应用包列表(兼容新旧版本)
     */
    private static List<PackageInfo> getInstalledPackages(PackageManager packageManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0));
        } else {
            return packageManager.getInstalledPackages(0);
        }
    }

    /**
     * 判断是否为系统应用
     */
    private static boolean isSystemApp(PackageInfo packageInfo) {
        return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    /**
     * 构建单个应用信息
     */
    private static MessageApkContent buildApkInfo(PackageInfo packageInfo,
                                                   PackageManager packageManager,
                                                   Map<String, byte[]> cachedIcons,
                                                   ApkIconDBUtil iconDB) {
        MessageApkContent apkInfo = new MessageApkContent();
        String packageName = packageInfo.packageName;

        apkInfo.setPackageName(packageName);
        apkInfo.setVersionCode(packageInfo.versionCode);
        apkInfo.setVersionName(packageInfo.versionName);

        // 获取应用名称
        try {
            CharSequence label = packageInfo.applicationInfo.loadLabel(packageManager);
            apkInfo.setName((label != null ? label.toString() : packageName) + ".apk");
        } catch (Exception e) {
            LLog.debug("Failed to load label for package: " + packageName);
            apkInfo.setName(packageName + ".apk");
        }

        // 获取应用路径和大小
        try {
            String sourceDir = packageInfo.applicationInfo.sourceDir;
            if (sourceDir != null) {
                apkInfo.setPath(sourceDir);
                apkInfo.setLength(new File(sourceDir).length());
            } else {
                apkInfo.setPath("");
                apkInfo.setLength(0);
            }
        } catch (Exception e) {
            LLog.debug("Failed to get file info for package: " + packageName);
            apkInfo.setPath("");
            apkInfo.setLength(0);
        }

        // 获取图标(优先缓存)
        byte[] iconBytes = cachedIcons.get(packageName);
        if (iconBytes == null) {
            iconBytes = loadAndCacheIcon(packageInfo, packageManager, iconDB, packageName, apkInfo.getPath());
        }
        apkInfo.setIcon(iconBytes);

        return apkInfo;
    }

    /**
     * 加载并缓存应用图标
     */
    @Nullable
    private static byte[] loadAndCacheIcon(PackageInfo packageInfo,
                                            PackageManager packageManager,
                                            ApkIconDBUtil iconDB,
                                            String packageName,
                                            String path) {
        try {
            Drawable drawable = packageInfo.applicationInfo.loadIcon(packageManager);
            if (drawable == null) {
                return null;
            }
            Bitmap bitmap = ImageUtils.drawableToBitmap(drawable);
            if (bitmap == null) {
                return null;
            }
            byte[] iconBytes = ImageUtils.bitmap2PngBytes(bitmap);
            // 异步保存到数据库,不阻塞主流程
            saveIconToDatabaseAsync(iconDB, packageName, path, iconBytes);
            return iconBytes;
        } catch (OutOfMemoryError e) {
            LLog.error("Out of memory while loading icon for: " + packageName, e);
            return null;
        } catch (Exception e) {
            LLog.debug("Failed to load icon for package: " + packageName);
            return null;
        }
    }

    /**
     * 预加载所有已缓存的图标到内存 Map 中
     *
     * @param iconDB   图标数据库工具
     * @param packages 已安装应用包列表
     * @return 缓存图标映射表(包名 → 图标字节数据)
     */
    private static Map<String, byte[]> preloadCachedIcons(ApkIconDBUtil iconDB, List<PackageInfo> packages) {
        Map<String, byte[]> cachedIcons = new HashMap<>(packages.size());
        try {
            for (PackageInfo packageInfo : packages) {
                String packageName = packageInfo.packageName;
                try {
                    byte[] icon = iconDB.queryIconByPackageName(packageName);
                    if (icon != null) {
                        cachedIcons.put(packageName, icon);
                    }
                } catch (Exception e) {
                    LLog.debug("Failed to query cached icon for: " + packageName);
                }
            }
        } catch (Exception e) {
            LLog.error("Error preloading cached icons", e);
        }
        return cachedIcons;
    }

    /**
     * 异步保存图标到数据库
     */
    private static void saveIconToDatabaseAsync(final ApkIconDBUtil iconDB,
                                                 final String packageName,
                                                 final String path,
                                                 final byte[] iconBytes) {
        new Thread(() -> {
            try {
                iconDB.addIcon(packageName, path, iconBytes);
            } catch (Exception e) {
                LLog.debug("Failed to save icon for package: " + packageName);
            }
        }, "IconSave-" + packageName).start();
    }

    /**
     * 获取所有启动器应用的包名列表
     *
     * @param context 上下文
     * @return 包名列表
     */
    public static List<String> getLauncherPackageNames(Context context) {
        PackageManager pm = context.getPackageManager();
        List<String> packageNameList = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : apps) {
            packageNameList.add(resolveInfo.activityInfo.packageName);
        }
        return packageNameList;
    }

    // ==================== 文件浏览 ====================

    /**
     * 递归扫描文件夹并计算总大小
     *
     * @param directory    目标文件夹
     * @param fileInfoList 扫描结果列表(输出参数)
     * @return 总文件大小(字节)
     */
    public static long calculateDirectorySize(File directory, List<MessageFileContent> fileInfoList) {
        long totalSize = 0;
        if (directory.exists() && directory.canRead()) {
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        totalSize += calculateDirectorySize(file, fileInfoList);
                    }
                }
            } else if (directory.isFile() && directory.length() > 0) {
                MessageFileContent content = new MessageFileContent();
                content.setName(directory.getName());
                content.setPath(directory.getPath());
                content.setLength(directory.length());
                fileInfoList.add(content);
                totalSize += content.getLength();
            }
        }
        return totalSize;
    }

    /**
     * 递归构建文件项树
     *
     * @param file         文件或文件夹
     * @param relativePath 相对路径
     * @param folderItem   父文件夹项
     * @param fileItems    文件项列表(输出参数)
     */
    public static void buildFileItemTree(File file, String relativePath,
                                          MessageFolderContent folderItem,
                                          List<MessageFileContent> fileItems) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    String childRelativePath = relativePath.isEmpty()
                            ? child.getName()
                            : relativePath + File.separator + child.getName();
                    buildFileItemTree(child, childRelativePath, folderItem, fileItems);
                }
            }
        } else {
            MessageFileContent item = new MessageFileContent();
            item.setName(relativePath.isEmpty() ? file.getName() : relativePath);
            item.setPath(file.getAbsolutePath());
            item.setLength(file.length());
            fileItems.add(item);
            folderItem.setLength(folderItem.getLength() + item.getLength());
        }
    }

    /**
     * 递归扫描 DocumentFile 目录并计算总大小
     *
     * @param documentFile DocumentFile 对象
     * @param parentPath   父路径
     * @param fileInfoList 扫描结果列表(输出参数)
     * @return 总文件大小(字节)
     */
    public static long calculateDocumentTreeSize(DocumentFile documentFile, String parentPath,
                                                  List<MessageFileContent> fileInfoList) {
        long totalSize = 0;
        if (documentFile.exists() && documentFile.canRead()) {
            if (documentFile.isDirectory()) {
                DocumentFile[] children = documentFile.listFiles();
                for (DocumentFile child : children) {
                    totalSize += calculateDocumentTreeSize(child,
                            parentPath + "/" + documentFile.getName(), fileInfoList);
                }
            } else if (documentFile.isFile() && documentFile.length() > 0) {
                MessageUriContent fileInfo = new MessageUriContent(documentFile.getUri());
                fileInfo.setName(documentFile.getName());
                fileInfo.setLength(documentFile.length());
                fileInfo.setPath(parentPath + "/" + documentFile.getName());
                fileInfoList.add(fileInfo);
                totalSize += fileInfo.getLength();
            }
        }
        return totalSize;
    }

    /**
     * 获取文件列表(支持普通目录和 SAF 目录)
     *
     * @param currentDir     当前目录
     * @param showHiddenFiles 是否显示隐藏文件
     * @param sortMethod      排序方式(0=名称升序, 1=大小升序, 2=时间升序, 3=名称降序, 4=大小降序, 5=时间降序)
     * @param context         上下文
     * @return 文件列表
     */
    public static List<MessageFileContent> listDirectoryContents(MessageFileContent currentDir,
                                                                  boolean showHiddenFiles,
                                                                  int sortMethod,
                                                                  Context context) {
        if (currentDir == null) {
            currentDir = new MessageFolderContent();
            currentDir.setPath(PermissionsUtils.ROOT_PATH);
        }

        Comparator<MessageFileContent> comparator = buildComparator(sortMethod);
        Resources res = context.getResources();
        List<MessageFileContent> fileList = new ArrayList<>();
        List<MessageFileContent> dirList = new ArrayList<>();

        if (VersionUtils.isAfterAndroid13() && PermissionsUtils.isAndroidData(currentDir.getPath())) {
            scanAndroidDataDir(currentDir, showHiddenFiles, sortMethod, context, fileList, dirList, comparator);
        } else if (PermissionsUtils.isSubAndroidData(currentDir.getPath())) {
            scanSubAndroidDataDir(currentDir, showHiddenFiles, context, fileList, dirList, res);
        } else if (currentDir instanceof MessageFolderContent) {
            scanNormalDirectory(currentDir, showHiddenFiles, context, fileList, dirList, res);
        }

        Collections.sort(fileList, comparator);
        Collections.sort(dirList, comparator);
        dirList.addAll(fileList);

        // 添加"返回上一级"项
        MessageFileContent backItem = new MessageFileContent();
        backItem.setName("...");
        backItem.setPreviewBitmap(decodeResource(res, R.drawable.ic_folder_upload));
        backItem.setIsPreView(false);
        backItem.setPath(currentDir.getPath());
        dirList.add(0, backItem);

        return dirList;
    }

    /**
     * 构建排序比较器
     */
    private static Comparator<MessageFileContent> buildComparator(int sortMethod) {
        switch (sortMethod) {
            case 0:
                return MessageFileContent::compareTo;
            case 1:
                return (o1, o2) -> Long.compare(o1.getLength(), o2.getLength());
            case 2:
                return (o1, o2) -> Double.compare(Math.signum(o1.getTime() - o2.getTime()), 0);
            case 3:
                return (o1, o2) -> o2.compareTo(o1);
            case 4:
                return (o1, o2) -> Long.compare(o2.getLength(), o1.getLength());
            case 5:
                return (o1, o2) -> Double.compare(Math.signum(o2.getTime() - o1.getTime()), 0);
            default:
                return MessageFileContent::compareTo;
        }
    }

    /**
     * 扫描 Android/data 目录(需要 MANAGE_EXTERNAL_STORAGE 权限)
     */
    private static void scanAndroidDataDir(MessageFileContent currentDir, boolean showHiddenFiles,
                                            int sortMethod, Context context,
                                            List<MessageFileContent> fileList,
                                            List<MessageFileContent> dirList,
                                            Comparator<MessageFileContent> comparator) {
        boolean isGranted = XXPermissions.isGranted(context, Permission.MANAGE_EXTERNAL_STORAGE);
        if (!isGranted) {
            if (!(context instanceof Activity)) {
                throw new RuntimeException(context.getString(
                        R.string.please_manually_authorize_this_folder_in_the_software));
            }
            XXPermissions.with(context)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .unchecked()
                    .request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            T.s((R.string.please_authorize_file_access_permission_or_else_software));
                        }
                    });
            return;
        }

        List<String> packageNames = getLauncherPackageNames(context);
        Resources res = context.getResources();
        for (String packageName : packageNames) {
            File file = new File(PermissionsUtils.ANDROID_DATA + "/" + packageName);
            if (file.exists()) {
                String name = file.getName();
                Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                MessageFolderContent folderItem = new MessageFolderContent();
                folderItem.setName(mUtil.stringSize(name, 20));
                folderItem.setPath(file.getPath());
                folderItem.setPreviewBitmap(bmp);
                folderItem.setIsPreView(false);
                folderItem.setTime(file.lastModified());
                dirList.add(folderItem);
            }
        }
    }

    /**
     * 扫描 Android/data 子目录(需要 SAF URI 权限)
     */
    private static void scanSubAndroidDataDir(MessageFileContent currentDir, boolean showHiddenFiles,
                                               Context context,
                                               List<MessageFileContent> fileList,
                                               List<MessageFileContent> dirList,
                                               Resources res) {
        Uri uri = PermissionsUtils.path2Uri(currentDir.getPath());
        String existsPermission = PermissionsUtils.existsGrantedUriPermission(uri, context);
        if (existsPermission == null) {
            if (!(context instanceof Activity)) {
                throw new RuntimeException("请手动在软件中授权此文件夹");
            }
            PermissionsUtils.goApplyUriPermissionPage(uri, (Activity) context);
            return;
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
                    if (!showHiddenFiles && name.startsWith(".")) {
                        continue;
                    }
                    Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                    MessageUriContent uriFileInfo = new MessageUriContent(documentFile.getUri());
                    uriFileInfo.setName(mUtil.stringSize(name, 20));
                    uriFileInfo.setPath(currentDir.getPath() + "/" + name);
                    uriFileInfo.setPreviewBitmap(bmp);
                    uriFileInfo.setIsPreView(false);
                    uriFileInfo.setTime(documentFile.lastModified());
                    uriFileInfo.setFile(false);
                    dirList.add(uriFileInfo);
                } else if (documentFile.isFile()) {
                    MessageUriContent uriFileInfo = new MessageUriContent(documentFile.getUri());
                    uriFileInfo.setName(name);
                    uriFileInfo.setPath(currentDir.getPath() + "/" + name);
                    uriFileInfo.setPreviewBitmap(decodeResource(res, R.drawable.ic_file_file));
                    uriFileInfo.setIsPreView(false);
                    uriFileInfo.setTime(documentFile.lastModified());
                    uriFileInfo.setLength(documentFile.length());
                    uriFileInfo.setFile(true);
                    fileList.add(uriFileInfo);
                }
            }
        }
    }

    /**
     * 扫描普通目录
     */
    private static void scanNormalDirectory(MessageFileContent currentDir, boolean showHiddenFiles,
                                             Context context,
                                             List<MessageFileContent> fileList,
                                             List<MessageFileContent> dirList,
                                             Resources res) {
        File directory = new File(currentDir.getPath());
        if (!directory.canRead()) {
            throw new RuntimeException(context.getString(R.string.this_folder_cannot_be_read));
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = file.getName();
            if (!showHiddenFiles && name.startsWith(".")) {
                continue;
            }

            if (file.isDirectory()) {
                Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                MessageFolderContent folderItem = new MessageFolderContent();
                folderItem.setName(mUtil.stringSize(name, 20));
                folderItem.setPath(file.getPath());
                folderItem.setPreviewBitmap(bmp);
                folderItem.setIsPreView(false);
                folderItem.setTime(file.lastModified());
                dirList.add(folderItem);
            } else if (file.isFile()) {
                MessageFileContent fileItem = new MessageFileContent();
                fileItem.setName(name);
                fileItem.setPath(file.getPath());
                fileItem.setTime(file.lastModified());
                fileItem.setLength(file.length());

                String extension = getFileExtension(name);
                boolean isPreView = false;

                if ("txt".equalsIgnoreCase(extension)) {
                    fileItem.setPreviewBitmap(decodeResource(res, R.drawable.ic_txts_file));
                } else if (isPreviewableMedia(extension)) {
                    isPreView = true;
                } else if ("zip".equalsIgnoreCase(extension)) {
                    fileItem.setPreviewBitmap(decodeResource(res, R.drawable.ic_zip_file));
                } else if ("apk".equalsIgnoreCase(extension)) {
                    fileItem.setPreviewBitmap(decodeResource(res, R.drawable.ic_null_android_file));
                } else if ("java".equalsIgnoreCase(extension)) {
                    fileItem.setPreviewBitmap(decodeResource(res, R.drawable.ic_coder_file));
                } else {
                    fileItem.setPreviewBitmap(decodeResource(res, R.drawable.ic_file_file));
                }

                fileItem.setIsPreView(isPreView);
                fileList.add(fileItem);
            }
        }
    }

    /**
     * 判断文件扩展名是否为可预览的媒体格式
     */
    private static boolean isPreviewableMedia(String extension) {
        return "jpg".equalsIgnoreCase(extension)
                || "png".equalsIgnoreCase(extension)
                || "jpeg".equalsIgnoreCase(extension)
                || "gif".equalsIgnoreCase(extension)
                || "mp4".equalsIgnoreCase(extension);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名(不含点号),如果没有扩展名则返回空字符串
     */
    public static String getFileExtension(String filename) {
        if (filename != null && !filename.isEmpty()) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    private static boolean isFileExists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 获取路径中最后一级文件夹名称
     *
     * @param path 文件路径
     * @return 父文件夹名称
     */
    public static String getParentFolderName(String path) {
        if (path != null && !path.isEmpty()) {
            String[] segments = path.split(File.separator);
            if (segments.length >= 2) {
                return segments[segments.length - 2];
            }
        }
        return "";
    }

    /**
     * 从 Context 中查找 Activity
     *
     * @param context 上下文
     * @return Activity 实例,如果未找到则返回 null
     */
    @Nullable
    static Activity findActivity(@NonNull Context context) {
        do {
            if (context instanceof Activity) {
                return (Activity) context;
            } else if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        } while (context != null);
        return null;
    }

    /**
     * 将毫秒时长格式化为 mm:ss 格式
     *
     * @param duration 毫秒时长
     * @return 格式化后的时长字符串
     */
    public static String formatDuration(long duration) {
        long minute = duration / 60000;
        long seconds = duration % 60000;
        long second = Math.round((float) seconds / 1000);

        StringBuilder sb = new StringBuilder();
        if (minute < 10) {
            sb.append("0");
        }
        sb.append(minute).append(":");
        if (second < 10) {
            sb.append("0");
        }
        sb.append(second);
        return sb.toString();
    }
}
