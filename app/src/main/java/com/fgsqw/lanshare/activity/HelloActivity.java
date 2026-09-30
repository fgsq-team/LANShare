package com.fgsqw.lanshare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.toast.T;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;


public class HelloActivity extends BaseActivity {
    private static final String[] PERMISSIONS_CAMERA_AND_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    public void init() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            int storagePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //检测是否有权限，如果没有权限，直接申请（不再弹隐私/提示对话框）
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_CAMERA_AND_STORAGE, 0);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isGet = XXPermissions.isGranted(this, Permission.MANAGE_EXTERNAL_STORAGE);
            //已有权限则返回
            if (!isGet) {
                requestFilePermission();
                return;
            }

        }
        startActivity();
    }

    private void requestFilePermission() {
        XXPermissions.with(HelloActivity.this)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .unchecked()
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean all) {
                        startActivity();
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean never) {
                        T.s((R.string.authorize_file_access_permission));
                        finish();
                    }
                });
    }

    public void startActivity() {
        Intent intent = new Intent(HelloActivity.this, DataCenterActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String sdCard = Environment.getExternalStorageState();
                if (sdCard.equals(Environment.MEDIA_MOUNTED)) {
                    startActivity();
                }
            } else {
                T.s(R.string.you_must_authorize_permissions);
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
