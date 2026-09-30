package com.fgsqw.lanshare.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.fgsqw.ContentTypes;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.toast.T;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.List;

public class FileUtil {

    private final static String TAG = FileUtil.class.getName();

    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
     */
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    /**
     * Eight-bit UCS Transformation Format
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order
     */
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order
     */
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark
     */
    public static final Charset UTF_16 = Charset.forName("UTF-16");

    public static final String DEFAULT_TYPE = "*/*";

    /**
     * @author fgsq
     * @comments 通过后缀名判断文件类型
     * @date 2024/5/21 15:33
     * @return String
     */
    public static String getNameType(String name) {
        if (name.contains(".")) {
            String suffix = name.substring(name.lastIndexOf(".") + 1);
            for (String[] strings : Config.fileTypes) {
                if (strings[0].equalsIgnoreCase(suffix)) {
                    return strings[1];
                }
            }
        }
        return "其他";
    }


    public static String createPath(String path, String cotegary) {
        File dir;
        if (Config.SAVE_FILES_CATEGORY) {
            dir = new File(path, cotegary);
        } else {
            dir = new File(path);
        }
        // 目录不存在时自动创建，避免落盘时提示失败
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir.toString();
    }

    public abstract static class Search {
        public boolean next = true;
        public String text;

        public Search(String text) {
            this.text = text.toLowerCase();
        }

        public abstract void onSearchChange(File foundFiles);
    }

    public static boolean searchFiles(File folder, Search search) {
        if (search == null) {
            throw new RuntimeException("Search is null!");
        }
        // 获取文件夹中的所有文件和子文件夹
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
//                Log.d("ffffffffff文件路径:", file.getAbsolutePath());
                if (file.getName().startsWith(".")) {
                    continue;
                }
                if (file.isDirectory()) {
                    if (file.getName().toLowerCase().contains(search.text)) {
                        if (!search.next) {
                            return false;
                        }
                        search.onSearchChange(file);
                    }
                    // 如果是文件夹，递归搜索
                    if (!search.next || !searchFiles(file, search)) {
                        return false;
                    }
                } else if (file.isFile()) {
                    // 如果是文件，检查文件名是否包含关键字
                    if (file.getName().toLowerCase().contains(search.text)) {
                        if (!search.next) {
                            return false;
                        }
                        // 将匹配的文件路径添加到列表中
                        search.onSearchChange(file);
                    }
                }
            }
        }
        return true;
    }

    /**
     * long格式转换文件大小
     *
     * @param size
     * @return
     */
    public static String computeSize(long size) {
        if (size <= 0) return "0B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 传输速度格式化（字节/秒）
     */
    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond <= 0) return "0 B/s";
        if (bytesPerSecond >= 1048576) return String.format("%.1f MB/s", bytesPerSecond / 1048576.0);
        if (bytesPerSecond >= 1024) return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        return bytesPerSecond + " B/s";
    }


    public static void createNullFile(String path) {
        try {
            File parent = new File(path).getParentFile();
            if (parent != null && !parent.isDirectory()) {
                parent.mkdirs();
            }
            OutputStream fileOut = new FileOutputStream(path);
            fileOut.write(0);
            fileOut.close();
        } catch (Exception e) {
            LLog.error("error", e);
        }

    }

    public static void copyFile(String srcPath, String outPath) throws IOException {
        File outFile = new File(outPath);
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        InputStream input = new FileInputStream(srcPath);
        OutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[2048];
        int ten = 0;
        while ((ten = input.read(buffer)) != -1) {
            out.write(buffer, 0, ten);
        }
        IOUtil.closeIO(out, input);
    }

    /**
     * 获取应用图标
     *
     * @param context 上下文对象
     * @param apkPath apk绝对路径
     * @return
     */
    public static Drawable getApkIcon(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch (OutOfMemoryError e) {
                Log.e("ApkIconLoader", e.toString());
                LLog.error("ApkIconLoader", e);
            }
        }
        return null;
    }


    /**
     * 根据路径打开文件
     *
     * @param context 上下文
     * @param path    文件路径
     */
    public static void openFile1(Context context, String path) {
        if (context == null || path == null)
            return;
        Intent intent = new Intent();
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        //文件的类型
        String type = getMyMIMEType(new File(path));

        try {
            File out = new File(path);
            Uri fileURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 由于7.0以后文件访问权限，可以通过定义xml在androidmanifest中申请，也可以直接跳过权限
                // 通过定义xml在androidmanifest中申请
                fileURI = FileProvider.getUriForFile(context, "com.fgfile.send.provider", out);
                // 直接跳过权限
               /* StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                fileURI = Uri.fromFile(out);*/
            } else {
                fileURI = Uri.fromFile(out);
            }
            //设置intent的data和Type属性
            intent.setDataAndType(fileURI, type);
            //跳转
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent);
            } else {
                T.s((R.string.no_corresponding_program_found));
            }

        } catch (Exception e) { //当系统没有携带文件打开软件，提示
            T.s((R.string.cannot_open_file_format));
            LLog.error("error", e);
        }
    }

    private static void startActivity(Activity activity, File file) {
        File apkFile = file;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //版本高于6.0，权限不一样
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            //兼容8.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean hasInstallPermission = activity.getPackageManager().canRequestPackageInstalls();
                if (!hasInstallPermission) {
                    startInstallPermissionSettingActivity(activity);
                }
            }
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        activity.startActivity(intent);
    }

    /**
     * 跳转到设置-允许安装未知来源-页面
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void startInstallPermissionSettingActivity(Activity activity) {
        //注意这个是8.0新API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public static void openFile(Activity activity, File file) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取文件file的MIME类型
            String type = getMyMIMEType(file);
            //设置intent的data和Type属性。android 7.0以上crash,改用provider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);//android 7.0以上
                intent.setDataAndType(fileUri, type);
                grantUriPermission(activity, fileUri, intent);
            } else {
                intent.setDataAndType(Uri.fromFile(file), type);
            }
            //跳转
            activity.startActivity(intent);
        } catch (Exception e) {
            LLog.error("error", e);
        }
    }


    public static void openFile(Activity activity, Uri uri, String filename) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取文件file的MIME类型
            String type = getMyMIMEType(filename);
            //设置intent的data和Type属性。android 7.0以上crash,改用provider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                Uri fileUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);//android 7.0以上
                intent.setDataAndType(uri, type);
                grantUriPermission(activity, uri, intent);
            } else {
                intent.setDataAndType(uri, type);
            }
            //跳转
            activity.startActivity(intent);
        } catch (Exception e) {
            LLog.error("error", e);
        }
    }

    private static void grantUriPermission(Context context, Uri fileUri, Intent intent) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }


    /**
     * 根据文件后缀名获得对应的MIME类型。
     **/
    public static String getMyMIMEType(String name) {
        String mimeType = ContentTypes.contentTypeMap.get(name);
        if (!StringUtils.isEmpty(mimeType)) {
            return mimeType;
        }
        return DEFAULT_TYPE;
    }

    /**
     * 根据文件后缀名获得对应的MIME类型。
     **/
    public static String getMyMIMEType(File file) {
        String name = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex < 0) {
            return DEFAULT_TYPE;
        }
        /* 获取文件的后缀名 */
        String end = name.substring(dotIndex + 1);
        if (end.isEmpty()) return DEFAULT_TYPE;
        return getMyMIMEType(end);
    }


    public static DocumentFile getDocumentFileFromSingleUri(Uri fileUri) {
        if (fileUri == null) return null;
        return DocumentFile.fromSingleUri(App.getInstance(), fileUri);
    }

    public static DocumentFile getDocumentFileFromTreeUri(Uri fileUri) {
        if (fileUri == null) return null;
        return DocumentFile.fromTreeUri(App.getInstance(), fileUri);
    }

    public static InputStream getInputStreamFromUri(Uri uri) throws FileNotFoundException {
        ContentResolver resolver = App.getInstance().getContentResolver();
        return resolver.openInputStream(uri);
    }

    public static void writeString(String str, String path) throws IOException {
        writeString(str, new File(path), true);
    }

    public static void writeString(String str, File path, boolean append) throws IOException {
        OutputStream out = new FileOutputStream(path, append);
        out.write(str.getBytes(FileUtil.UTF_8));
        out.close();
    }

    /**
     * 防止重名文件被覆盖
     */
    public static File avoidDuplication(File outFile) {
        String name = outFile.getName();
        if (outFile.exists()) {
            for (int s = 1; s < 65535; s++) {
                String str;
                if (name.contains(".")) {
                    String prefix = name.substring(0, name.lastIndexOf(".")) + "(" + s + ")";
                    String suffix = name.substring(name.lastIndexOf("."));
                    str = prefix + suffix;
                } else {
                    str = name + "(" + s + ")";
                }
                outFile = new File(outFile.getParentFile(), str);
                if (!outFile.exists()) {
                    return outFile;
                }
            }
        }
        return outFile;
    }


    public static boolean deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                return file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (!deleteFile(f)) {
                            return false;
                        }
                    }
                }
                return file.delete();
            }
        }
        return true;
    }


    public static void startApp(Context context, String packageName) {
        context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packageName));
    }


    public static void uninstallApp(Context context, String packageName) {
        Intent uninstall_intent = new Intent();
        uninstall_intent.setAction(Intent.ACTION_DELETE);
        uninstall_intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(uninstall_intent);
    }


    public static String getParentPath(String path) {
        if (!StringUtils.isEmpty(path)) {
            String s = path;
            int i = s.lastIndexOf("/");
            if (i > -1) {
                int length = s.length() - 1;
                if (i == length) {
                    s = s.substring(0, length);
                    i = s.lastIndexOf("/");
                }
                return s.substring(0, i);

            } else {
                return path;
            }
        }
        return null;
    }
}
