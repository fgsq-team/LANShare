package com.fgsqw.lanshare.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.fgsqw.lanshare.R;


public class HelloActivity extends AppCompatActivity {
    private static String[] PERMISSIONS_CAMERA_AND_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();


    }

    public void init() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int storagePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //检测是否有权限，如果没有权限，就需要申请
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                //申请权限
                dialog();
                return;
            }

        }
        startActivity();

    }

    public void startActivity() {
        Intent intent = new Intent(HelloActivity.this, DataCenterActivity.class);
        startActivity(intent);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void dialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(HelloActivity.this);
        dialog.setTitle("提示");
        dialog.setMessage("您必须同意储存使用权限才能继续使用本软件");
        dialog.setPositiveButton("确定", (dialogInterface, i) -> requestPermissions(PERMISSIONS_CAMERA_AND_STORAGE, 0));
        dialog.setNegativeButton("取消", (dialogInterface, i) -> finish());
        dialog.show();
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(HelloActivity.this, "您必须授权权限才能使用比软件", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
