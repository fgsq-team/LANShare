package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.FileSendDialog;
import com.fgsqw.lanshare.fragment.FragChat;
import com.fgsqw.lanshare.fragment.FragFiles;
import com.fgsqw.lanshare.fragment.minterface.ChildBaseMethod;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.StringUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;


@SuppressWarnings("all")
public class DataCenterActivity extends BaseActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener {


    private LinearLayout bottomRecord;
    private ImageView imgRecord;
    private TextView tvRecord;

    private LinearLayout bottomSend;
    private ImageView imgSend;

    private LinearLayout bottomFiles;
    private ImageView imgFiles;
    private TextView tvFiles;

    private Toolbar mainToobar;

    private LinearLayout bottomLayout;

    private final List<Fragment> fragmentList = new ArrayList<>();
    private BaseFragment currentFragment;
    private FragFiles fragFiles;
    private FragChat fragChat;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initView();
        initFragment();


        Intent lanService = new Intent();
        lanService.setClass(this, LANService.class);
        lanService.putExtra("messenger", new Messenger(fragChat.getHandler()));
        startService(lanService);
    }

    public void updateIP(String ip) {
        mainToobar.setSubtitle(ip);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getConfig();
    }

    public void getConfig() {
        PrefUtil prefUtil = new PrefUtil(this);
        String filePath = prefUtil.getString(PreConfig.FILE_PATH);
        if (filePath.isEmpty()) {
            prefUtil.saveString(PreConfig.FILE_PATH, Config.DEFAULT_FILE_SAVE_PATH);
            filePath = Config.DEFAULT_FILE_SAVE_PATH;
        }

        String userName = prefUtil.getString(PreConfig.USER_NAME);
        if (userName.isEmpty()) {
            prefUtil.saveString(PreConfig.USER_NAME, android.os.Build.MODEL);
        }
        Config.FILE_SAVE_PATH = new File(Environment.getExternalStorageDirectory(), filePath).getPath() + File.separator;

        File file = new File(Config.FILE_SAVE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        Config.SAVE_MESSAGE = prefUtil.getBoolean(PreConfig.SAVE_MESSAGE, true);
        Config.SAVE_TO_GALLERY = prefUtil.getBoolean(PreConfig.SAVE_TO_GALLERY, true);
    }

    public void initView() {
        bottomRecord = bind(R.id.bottom_record);
        imgRecord = bind(R.id.img_record);
        tvRecord = bind(R.id.bottom_record_tv);

        bottomSend = bind(R.id.bottom_send);
        imgSend = bind(R.id.img_send);

        bottomFiles = bind(R.id.bottom_files);
        imgFiles = bind(R.id.img_files);
        tvFiles = bind(R.id.bottom_files_tv);

        bottomLayout = bind(R.id.bottom_container);

        mainToobar = bind(R.id.main_toolbar);
        mainToobar.inflateMenu(R.menu.toolbar_menu);
        mainToobar.setOnMenuItemClickListener(this);

        bottomRecord.setOnClickListener(this);
        bottomSend.setOnClickListener(this);
        bottomFiles.setOnClickListener(this);
    }


    private void initFragment() {
        fragChat = new FragChat();
        fragmentList.add(fragChat);
        fragFiles = new FragFiles();
        fragmentList.add(fragFiles);
        switchFragment(0);
    }

    private void switchFragment(int whichFragment) {
        if (whichFragment == 0) {
            tvRecord.setTextColor(getResources().getColor(R.color.text_select));
            tvFiles.setTextColor(getResources().getColor(R.color.text_not_select));
            imgRecord.setImageResource(R.drawable.ic_select_record);
            imgFiles.setImageResource(R.drawable.ic_file);
        } else {
            tvRecord.setTextColor(getResources().getColor(R.color.text_not_select));
            tvFiles.setTextColor(getResources().getColor(R.color.text_select));
            imgRecord.setImageResource(R.drawable.ic_record);
            imgFiles.setImageResource(R.drawable.ic_select_file);
        }
        Fragment fragment = fragmentList.get(whichFragment);
        setFragment(fragment);
    }


    public void setFragment(Fragment fragment) {
        int frameLayoutId = R.id.fl_container;

        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (fragment.isAdded()) {
                if (currentFragment != null) {
                    transaction.hide(currentFragment).show(fragment);
                } else {
                    transaction.show(fragment);
                }
            } else {
                if (currentFragment != null) {
                    transaction.hide(currentFragment).add(frameLayoutId, fragment);
                } else {
                    transaction.add(frameLayoutId, fragment);
                }
            }
            currentFragment = (BaseFragment) fragment;
            transaction.commit();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_record: {
                switchFragment(0);
                break;
            }
            case R.id.bottom_send: {
                if (fragFiles.getFileSelects().size() <= 0) {
                    Toast.makeText(this, "请选择文件", Toast.LENGTH_LONG).show();
                    return;
                }
                sendFiles(fragFiles.getFileSelects());
                break;
            }
            case R.id.bottom_files: {
                switchFragment(1);
                break;
            }
            default:
                break;
        }
    }


    public void sendOneFile(FileInfo fileInfo) {
        FileSendDialog dialog = new FileSendDialog(this, 1);
        dialog.setOnDeviceSelect(device -> {
            LANService.service.fileSend(device, Arrays.asList(fileInfo));
        });
        dialog.show();
    }


    public void sendFiles(List<FileInfo> fileSelects) {
        FileSendDialog dialog = new FileSendDialog(this, fileSelects.size());
        dialog.setOnDeviceSelect(device -> {
            LANService.service.fileSend(device, new LinkedList<>(fileSelects));
            ((ChildBaseMethod) fragFiles).clearSelect();
            fileSelects.clear();
            setSelectCount(fileSelects.size());
        });
        dialog.show();
    }

    public void setSelectCount(int count) {
        String str;
        if (count <= 0) {
            str = "文件";
        } else if (count > 999) {
            str = "已选(999+)";
        } else {
            str = "已选(" + count + ")";
        }
        tvFiles.setText(str);
    }

    @SuppressLint("SetTextI18n")
    public boolean addASendFile(FileInfo fileInfo) {
        if (fragFiles.getFileSelects().size() >= 1000) return false;
        List fileSelects = fragFiles.getFileSelects();
        fileSelects.add(fileInfo);
        setSelectCount(fileSelects.size());
        return true;
    }

    public void removeSendFile(FileInfo fileInfo) {
        List fileSelects = fragFiles.getFileSelects();
        fileSelects.remove(fileInfo);
        setSelectCount(fileSelects.size());
    }

    public void removeSendALL(List infos) {
        List fileSelects = fragFiles.getFileSelects();
        fileSelects.removeAll(infos);
        setSelectCount(fileSelects.size());
    }

    public void removeSendALL() {
        List fileSelects = fragFiles.getFileSelects();
        fileSelects.clear();
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if ((n != KeyEvent.KEYCODE_BACK) || (keyEvent.getRepeatCount() != 0))
            return super.onKeyDown(n, keyEvent);
        if (!currentFragment.onKeyDown(n, keyEvent)) {
            moveTaskToBack(true);
        }
        return true;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(resultCode, data);
            final String qrContent = scanResult.getContents();
            if (!StringUtils.isEmpty(qrContent)) {
                fragChat.setEditContent(qrContent);
            }
        }
    }

    public void hideBottom() {
        bottomLayout.setVisibility(View.GONE);
    }

    public void showBottom() {
        bottomLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.menu_setting:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.menu_http_share:
                startActivity(new Intent(this, HttpShareActivity.class));
                break;
            case R.id.menu_delete_message:
                if (fragChat != null) {
                    fragChat.messageDelete();
                }
                break;
            case R.id.menu_scan:
                //打开扫描界面
                IntentIntegrator intentIntegrator = new IntentIntegrator(this);
                intentIntegrator.setOrientationLocked(false);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                intentIntegrator.setCaptureActivity(ZxingActivity.class); // 设置自定义的activity是QRActivity
                intentIntegrator.setRequestCode(REQUEST_CODE);
                intentIntegrator.initiateScan();
                break;
            case R.id.menu_exit:
                stopService(new Intent(this, LANService.class));
                finish();
                break;
           /* case R.id.menu_camera_send:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.menu_camera_recv:
                startActivity(new Intent(this, TestActivity.class));
                break;*/

        }
        return true;
    }
}



























