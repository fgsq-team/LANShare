package com.fgsqw.lanshare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.InfoDialog;
import com.fgsqw.lanshare.dialog.PrivacyDialog;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.PrefUtil;
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
        PrefUtil prefUtil = App.getPrefUtil();
        // 隐私政策
        boolean privacyAgree = prefUtil.getBoolean(PreConfig.PRIVACY_AGREE);
        int privacyVersion = prefUtil.getInt(PreConfig.PRIVACY_VERSION);
        if (privacyAgree && privacyVersion == Config.PRIVACY_VERSION) {
            init();
        } else {
            privacy();
        }
    }

    public void privacy() {
        PrivacyDialog privacyDialog = new PrivacyDialog(this);
        privacyDialog.setOnClickListener(agree -> {
            PrefUtil prefUtil = App.getPrefUtil();
            if (agree) {
                prefUtil.saveBoolean(PreConfig.PRIVACY_AGREE, true);
                prefUtil.saveInt(PreConfig.PRIVACY_VERSION, Config.PRIVACY_VERSION);
                privacyDialog.dismiss();
                init();
            } else {
                prefUtil.saveBoolean(PreConfig.PRIVACY_AGREE, false);
                privacyDialog.dismiss();
                finish();
            }
        });
        privacyDialog.setCancelable(false);
        privacyDialog.show();
    }

    public void init() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            int storagePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //检测是否有权限，如果没有权限，就需要申请
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                //申请权限
                dialog();
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isGet = XXPermissions.isGranted(this, Permission.MANAGE_EXTERNAL_STORAGE);
            //已有权限则返回
            if (!isGet) {
                dialog();
                return;
            }

        }
        startActivity();
    }

    public void startActivity() {
        Intent intent = new Intent(HelloActivity.this, DataCenterActivity.class);
//        Intent intent = new Intent(HelloActivity.this, SensorTrailActivity.class);
        startActivity(intent);
        finish();
    }

    public void dialog() {
        InfoDialog infoDialog = new InfoDialog(this);
        infoDialog.setText(getString(R.string.you_need_to_agree_to_storage_permissions));
        infoDialog.setCancelable(false);
        infoDialog.setOnClickListener(agree -> {
            if (agree) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
//                                        if (never) {
//                                            XXPermissions.startPermissionActivity(HelloActivity.this, permissions);
//                                        }
                                    T.s((R.string.authorize_file_access_permission));
                                    finish();
                                }
                            });
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS_CAMERA_AND_STORAGE, 0);
                }
            } else {
                finish();
            }
        });
        infoDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String sdCard = Environment.getExternalStorageState();
                if (sdCard.equals(Environment.MEDIA_MOUNTED)) {
                    startActivity();
                }
            } else {
                init();
                T.s(R.string.you_must_authorize_permissions);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
