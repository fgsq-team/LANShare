package com.fgsqw.lanshare.utils;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import java.io.File;
import java.util.List;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

public class PermissionsUtils {

    public static final int REQUEST_VISIT = 20;

    // 根目录
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    // 安卓data目录
    public static final String ANDROID_DATA = ROOT_PATH + "/Android/data";
    // 安卓obb目录
    public static final String ANDROID_OBB = ROOT_PATH + "/Android/obb";
    public static final String URI_SEPARATOR = "%2F";


    public static boolean isAndroidData(String path) {
        return path.equals(ANDROID_DATA);
    }

    public static boolean isAndroidObb(String path) {
        return path.equals(ANDROID_OBB);
    }


    public static boolean isSubAndroidData(String path) {
        return path.startsWith(ANDROID_DATA);
    }


    public static String existsGrantedUriPermission(Uri uri, Context context) {

        String reqUri = uri.toString().replace("documents/document/primary", "documents/tree/primary");
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

    public static void goApplyUriPermissionPage(Uri uri, Activity activity) {

//
//        //获取所有已授权并存储的Uri列表，遍历并判断需要申请的uri是否在其中,在则说明已经授权了
//        boolean isGet = isGrantedUriPermission(uri, activity, fragment);//这里会对activity重新赋值
//        if (isGet) {
//            return;
//        }

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



}
