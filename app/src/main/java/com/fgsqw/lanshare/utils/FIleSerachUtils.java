package com.fgsqw.lanshare.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;

import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.PhotoFolder;
import com.fgsqw.lanshare.pojo.MediaInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FIleSerachUtils {


    /**
     * 从SDCard加载图片
     *
     * @param context
     */
    public static List<PhotoFolder> loadImageForSDCard(final Context context) {
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

        return splitFolder(mediaInfos, loadVideoForSDCard(context));
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

    public static List<ApkInfo> loadApkForSDCard(Context context) {
        //由于扫描应用是耗时的操作，所以要在子线程处理。
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN); // 动作匹配
        intent.addCategory(Intent.CATEGORY_LAUNCHER); // 类别匹配
        List<ResolveInfo> mApps = pm.queryIntentActivities(intent, 0);
        Collections.sort(mApps, (a, b) -> {
            // 排序规则
            PackageManager pm1 = context.getPackageManager();
            return String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(pm1).toString(), b.loadLabel(pm1).toString()); // 忽略大小写排序软件
        });
        List<ApkInfo> apkInfoList = new ArrayList<>();
        for (int i = 0; i < mApps.size(); i++) {
            ResolveInfo app = mApps.get(i);
            String apkName = app.loadLabel(pm).toString();
            String apkPath = null;
            try {
                apkPath = context.getPackageManager().getApplicationInfo(app.activityInfo.packageName, 0).sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            long length = new File(apkPath).length();
            if (length <= 0) continue;
//            Log.d("hhhhhhhhhh", apkName + ".apk");
            ApkInfo apkInfo = new ApkInfo();
            apkInfo.setName(apkName + ".apk");
            apkInfo.setIcon(app.loadIcon(pm));
            apkInfo.setLength(length);
            apkInfo.setPath(apkPath);
            apkInfo.setPackageName(app.activityInfo.packageName);
            apkInfoList.add(apkInfo);
        }
        return apkInfoList;

    }

    /**
     * 扫描文件夹下的文件并返回总文件大小
     * @param path 文件夹路径
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

    private static List<PhotoFolder> splitFolder(List<MediaInfo> photoList, List<MediaInfo> videoList) {
        List<PhotoFolder> folders = new ArrayList<>();
        List<MediaInfo> allMedia = new ArrayList<>();

        allMedia.addAll(photoList);
        allMedia.addAll(videoList);

        folders.add(new PhotoFolder("所有图片", photoList));
        folders.add(new PhotoFolder("所有视频", videoList));

        if (!allMedia.isEmpty()) {           //判断对象不为空
            int size = allMedia.size();
            for (int i = 0; i < size; i++) {                     // 遍历所有图片
                String path = allMedia.get(i).getPath();       // 获取所有图片目录
                String name = getFolderName(path);               // 获取所有图片所在目录
                if (name != null && name.length() > 0) {         // 判断字符串不为空
                    PhotoFolder folder = getFolder(name, folders); // 传入对象
                    folder.addImage(allMedia.get(i));
                }
            }
        }
        return folders;
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
