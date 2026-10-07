package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.EditTextDialog;
import com.fgsqw.lanshare.dialog.InfoDialog;
import com.fgsqw.lanshare.dialog.PrivacyDialog;
import com.fgsqw.lanshare.dialog.RewardDialog;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.mUtil;

public class AboutActivity extends BaseActivity implements View.OnClickListener {

    RelativeLayout aboutLayout;
    RelativeLayout privacyLayout;
    RelativeLayout groupLayout;
    RelativeLayout aboutReward;
    RelativeLayout aboutUpdateVersion;
    RelativeLayout aboutOfficialWebsite;
    RelativeLayout aboutGithub;
    RelativeLayout aboutUploadLogs;
    ImageView exitImg;
    ImageView aboutLogo;
    TextView tvVersionName;
    int count = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initView();
    }

    @SuppressLint("SetTextI18n")
    public void initView() {
        aboutLogo = bind(R.id.about_logo);
        aboutLayout = bind(R.id.about_help);
        privacyLayout = bind(R.id.about_privacy);
        groupLayout = bind(R.id.about_group);
        aboutReward = bind(R.id.about_reward);
        aboutUpdateVersion = bind(R.id.about_update_version);
        aboutOfficialWebsite = bind(R.id.about_official_website);
        aboutGithub = bind(R.id.about_github);
        aboutUploadLogs = bind(R.id.about_upload_logs);
        tvVersionName = bind(R.id.about_version_name);
        exitImg = bind(R.id.about_exit_img);
        TextView copyright = bind(R.id.about_copyright);
        copyright.setText(mUtil.addString("Copyright © 2021-2026 By FGSQ", "\n", "        All Rights Reserved"));
        tvVersionName.setText("V" + mUtil.getAppVersionName(this));
        privacyLayout.setOnClickListener(this);
        aboutLayout.setOnClickListener(this);
        groupLayout.setOnClickListener(this);
        aboutReward.setOnClickListener(this);
        aboutUpdateVersion.setOnClickListener(this);
        aboutUploadLogs.setOnClickListener(this);
        aboutOfficialWebsite.setOnClickListener(this);
        aboutGithub.setOnClickListener(this);
        aboutLogo.setOnClickListener(this);
        exitImg.setOnClickListener(this);
    }

    /**
     * @author fgsq
     * @comments 隐私声明弹窗
     * @date 2024/4/28 16:09
     */
    public void privacy() {
        PrivacyDialog privacyDialog = new PrivacyDialog(this);
        privacyDialog.setOnClickListener(agree -> {
            PrefUtil prefUtil = App.getPrefUtil();
            if (agree) {
                prefUtil.saveBoolean(PreConfig.PRIVACY_AGREE, true);
                prefUtil.saveInt(PreConfig.PRIVACY_VERSION, Config.PRIVACY_VERSION);
                privacyDialog.dismiss();
            } else {
                prefUtil.saveBoolean(PreConfig.PRIVACY_AGREE, false);
                privacyDialog.dismiss();
            }
        });
        privacyDialog.setCancelable(false);
        privacyDialog.show();
    }

    /**
     * @author fgsq
     * @comments 显示手机信息
     * @date 2024/4/28 16:09
     */
    public void showBuildInfo() {
        TextView textView = findViewById(R.id.about_build_name);
        textView.setText(mUtil.getBuildInfo(this));
    }

    /**
     * @author fgsq
     * @comments 上传日志到服务器
     * @date 2024/4/28 16:25
     */
    public void uploadLogs() {
        EditTextDialog dialog = new EditTextDialog(this, true, "输入问题说明(可选)", "", "输入问题说明(可选,最大300字符)");
        dialog.setMaxLen(300);
        dialog.setOnClickListener((ok, str) -> {
            if (ok) {
                mUtil.uploadLogs(str, this);
            }
        });
        dialog.show();
        dialog.setLeftButtonText("取消");
        dialog.setRightButtonText("上传");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_help: {
                InfoDialog dialog = new InfoDialog(this);
                dialog.setTitle(R.string.use_help);
                dialog.setText(getString(R.string.use_help_detail));
                dialog.setLeftButtonText("");
                dialog.show();
            }
            break;
            case R.id.about_privacy: {
                privacy();
            }
            break;
            case R.id.about_exit_img: {
                finish();
            }
            break;
            case R.id.about_group: {
                mUtil.joinQQGroup("xJNzrothp7Dj3U3GDRxEjJaH78y4TSrF", AboutActivity.this);
            }
            break;
            case R.id.about_update_version: {
                mUtil.checkUpdate(true, true, this);
            }
            break;
            case R.id.about_upload_logs: {
                uploadLogs();
            }
            break;
            case R.id.about_official_website: {
                mUtil.openUrlInBrowser(this, Config.SERVER);
            }
            break;
            case R.id.about_github: {
                mUtil.openUrlInBrowser(this, Config.GITHUB);
            }
            break;
            case R.id.about_reward: {
                RewardDialog rewardDialog = new RewardDialog(this);
                rewardDialog.show();
            }
            break;
            case R.id.about_logo: {
                count++;
                if (count == 3) {
                    T.s((R.string.system_information_displayed));
                    showBuildInfo();
                } else if (count == 1) {
                    T.s((R.string.click_twice_to_display_system_information));
                }
            }
            break;
        }
    }
}
