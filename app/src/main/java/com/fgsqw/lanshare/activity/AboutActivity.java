package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.utils.mUtil;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {
    RelativeLayout githubLayout;
    RelativeLayout aboutLayout;
    RelativeLayout privacyLayout;
    ImageView exitImg;
    TextView tvVersionName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initView();
    }

    @SuppressLint("SetTextI18n")
    public void initView() {
        githubLayout = findViewById(R.id.about_github);
        aboutLayout = findViewById(R.id.about_help);
        privacyLayout = findViewById(R.id.about_privacy);
        tvVersionName = findViewById(R.id.about_version_name);

        exitImg = findViewById(R.id.about_exit_img);
        TextView copyright = findViewById(R.id.about_copyright);
        copyright.setText(mUtil.addString("Copyright © 2021-2022 By FG", "\n", "        All Rights Reserved"));

        tvVersionName.setText("V" + mUtil.getAppVersionName(this));
        privacyLayout.setOnClickListener(this);
        githubLayout.setOnClickListener(this);
        aboutLayout.setOnClickListener(this);
        exitImg.setOnClickListener(this);

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_github: {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fgsqme/LANShare/"));
                startActivity(intent);
                break;
            }
            case R.id.about_help: {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("使用帮助");
                dialog.setMessage("两部手机只需要在同一局域网并打开软件就可以相互传输文件\n\n1.选择文件\n2.选择设备\n3.接收端选择接收文件");
                dialog.setPositiveButton("确定", null);
                final AlertDialog alertdialog1 = dialog.create();
                alertdialog1.show();

                break;
            }
            case R.id.about_privacy: {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("隐私声明");
                dialog.setMessage("  本应用并不会收集用户任何信息并且已开源\n  当软件需要使用到权限并申请时，您有权拒绝授予权限");
                dialog.setPositiveButton("确定", null);
                final AlertDialog alertdialog1 = dialog.create();
                alertdialog1.show();
                break;
            }
            case R.id.about_exit_img: {
                finish();
                break;
            }
        }
    }
}
