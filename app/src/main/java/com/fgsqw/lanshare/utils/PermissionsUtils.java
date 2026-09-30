package com.fgsqw.lanshare.utils;


import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.util.List;
import java.util.Set;

public class PermissionsUtils {

    public static final int REQUEST_VISIT = 20;
    public static final int REQUEST_POST_NOTIFICATIONS = 21;
    // 进入应用页申请系统「获取应用列表」权限的请求码
    public static final int REQUEST_APP_LIST_PERMISSION = 999;
    // MIUI/HyperOS「获取应用列表」系统权限
    public static final String MIUI_GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS";

    /**
     * MIUI 13+/HyperOS 是否支持动态申请「获取应用列表」权限
     */
    public static boolean isAppListPermissionSupported(Context context) {
        try {
            PermissionInfo permissionInfo = context.getPackageManager()
                    .getPermissionInfo(MIUI_GET_INSTALLED_APPS, 0);
            return permissionInfo != null && "com.lbe.security.miui".equals(permissionInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否可读取完整应用列表：非 MIUI（QUERY_ALL_PACKAGES 已足够）或已授予该权限
     */
    public static boolean hasAppListPermission(Context context) {
        if (context == null) {
            return true;
        }
        if (!isAppListPermissionSupported(context)) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, MIUI_GET_INSTALLED_APPS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // 根目录
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    // 安卓data目录
    public static final String ANDROID_DATA = ROOT_PATH + "/Android/data";
    public static final String URI_SEPARATOR = "%2F";

    public static boolean isAndroidData(String path) {
        if (!VersionUtils.isAfterAndroid11()) {
            return false;
        }
        return path.equals(ANDROID_DATA);
    }


    // 检查通知权限是否开启
    public static boolean isNotificationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 在Android O及以上版本中，使用NotificationManager的方法
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return notificationManager.areNotificationsEnabled();
        } else {
            // 在Android O以下版本中，使用Settings的方法
            String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return flat != null && flat.contains(context.getPackageName());
        }
    }

    // 跳转至应用通知设置页面
    public static void openNotificationSettings(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 在Android O及以上版本中，跳转至系统通知设置页面
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            // 在Android O以下版本中，跳转至应用详情页面
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        context.startActivity(intent);
    }

    public static boolean hasNotificationPermission(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }

    public static boolean isSubAndroidData(String path) {
        if (!VersionUtils.isAfterAndroid11()) {
            return false;
        }
        return path.startsWith(ANDROID_DATA);
    }

    public static void requestNotificationPermission(Context context) {
        Intent intent_s = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent_s = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            context.startActivity(intent_s);
        }
    }


    public boolean hasWritePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean isGet = XXPermissions.isGranted(context, Permission.MANAGE_EXTERNAL_STORAGE);
            int perm = context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
            return isGet || perm == PackageManager.PERMISSION_GRANTED;
        } else {
            int perm = context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
            return perm == PackageManager.PERMISSION_GRANTED;
        }
    }


    public static String existsGrantedUriPermission(Uri uri, Context context) {

        String reqUri = uri.toString().replace("content://com.android.externalstorage.documents/document",
                "content://com.android.externalstorage.documents/tree");
        //获取已授权并已存储的uri列表
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            List<UriPermission> uriPermissions = context.getContentResolver().getPersistedUriPermissions();
            String tempUri;
            //遍历并判断请求的uri字符串是否已经被授权
            for (UriPermission uriP : uriPermissions) {
                tempUri = uriP.getUri().toString();
                //如果父目录已经授权就返回已经授权
                if (reqUri.matches(tempUri + URI_SEPARATOR + ".*") || (reqUri.equals(tempUri) && (uriP.isReadPermission() || uriP.isWritePermission()))) {
                    return tempUri;
                }
            }
        }
        return null;
    }

    public static final String PRIMARY = "primary:";
    public static final String URI_PERMISSION_REQUEST_PREFIX = "com.android.externalstorage.documents";
    public static final String URI_PERMISSION_REQUEST_COMPLETE_PREFIX = "content://com.android.externalstorage.documents";

    public static Uri path2Uri(String path) {
        String uriSuf = PRIMARY + path.replaceFirst(ROOT_PATH + File.separator, "");
        Uri uri = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            uri = DocumentsContract.buildDocumentUri(URI_PERMISSION_REQUEST_PREFIX, uriSuf);
        }
        return uri;
    }

    public static Uri docPath2Uri(String path) {
        String uriSuf = path;
        Uri uri = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            uri = DocumentsContract.buildDocumentUri(URI_PERMISSION_REQUEST_PREFIX, uriSuf);
        }
        return uri;
    }


    public static void goApplyUriPermissionPage(Uri uri, Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true)
                .putExtra("android.content.extra.SHOW_ADVANCED", true)
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);

        activity.startActivityForResult(intent, REQUEST_VISIT);
    }


    public static void requestPermissions(Activity context, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(
                context,
                permissions,
                requestCode
        );
    }

    public static boolean checkPermissions(Activity context, String[] permissions) {
        for (String permission : permissions) {
            int storagePermission = ContextCompat.checkSelfPermission(
                    context,
                    permission
            );
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


}
