package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Messenger;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.provider.DocumentFile;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.FileSendDialog;
import com.fgsqw.lanshare.fragment.FragChat;
import com.fgsqw.lanshare.fragment.FragFiles;
import com.fgsqw.lanshare.fragment.interfaces.IChildBaseMethod;
import com.fgsqw.lanshare.pojo.AddDevice;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.UriFileInfo;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.ByteUtil;
import com.fgsqw.lanshare.utils.DataDec;
import com.fgsqw.lanshare.utils.DataEnc;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.fgsqw.lanshare.utils.PermissionsUtils.REQUEST_VISIT;
import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;


@SuppressWarnings("all")
public class DataCenterActivity extends BaseActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener, View.OnLongClickListener {


    private LinearLayout bottomRecord;
    private LinearLayout mainScan;
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

    public List<FileInfo> fileSelects = new LinkedList<>();


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
        processExtraData();
    }

    public void updateIP(String ip) {
        mainToobar.setSubtitle(ip);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getConfig();
    }

    private void processExtraData() {
        Intent intent = getIntent();
        List<FileInfo> externalShareFiles = getExternalShareFiles(intent);
        if (!externalShareFiles.isEmpty()) {
            sendFiles(externalShareFiles);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processExtraData();
    }

    public void getConfig() {
        PrefUtil prefUtil = App.getPrefUtil();
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
        mainScan = bind(R.id.main_scan);
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
        mainScan.setOnClickListener(this);
        mainScan.setOnLongClickListener(this);
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
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.main_scan: {
                startActivity(new Intent(this, DeviceQrCodeActivity.class));
                break;
            }
            default:
                break;
        }

        return true;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bottom_record: {
                switchFragment(0);
                break;
            }
            case R.id.bottom_send: {
                if (fileSelects.size() <= 0) {
                    Toast.makeText(this, "请选择文件", Toast.LENGTH_LONG).show();
                    return;
                }
                sendFiles(fileSelects);
                break;
            }
            case R.id.bottom_files: {
                switchFragment(1);
                break;
            }
            case R.id.main_scan: {
                //打开扫描界面
                IntentIntegrator intentIntegrator = new IntentIntegrator(this);
                intentIntegrator.setOrientationLocked(false);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                intentIntegrator.setCaptureActivity(ZxingActivity.class); // 设置自定义的activity是QRActivity
                intentIntegrator.setRequestCode(REQUEST_CODE);
                intentIntegrator.initiateScan();
                break;
            }

            default:
                break;
        }
    }


    public void sendOneFile(FileInfo fileInfo) {
        FileSendDialog dialog = new FileSendDialog(this, 1);
        dialog.setOnDeviceSelect(device -> {
            LANService.getInstance().fileSend(device, Arrays.asList(fileInfo));
        });
        dialog.show();
    }


    public void sendFiles(List<FileInfo> fileSelects) {
        FileSendDialog dialog = new FileSendDialog(this, fileSelects.size());
        dialog.setOnDeviceSelect(device -> {
            LANService.getInstance().fileSend(device, new LinkedList<>(fileSelects));
            ((IChildBaseMethod) fragFiles).clearSelect();
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
        if (fileSelects.size() >= 1000) return false;
        fileSelects.add(fileInfo);
        setSelectCount(fileSelects.size());
        return true;
    }

    public void removeSendFile(FileInfo fileInfo) {
        fileSelects.remove(fileInfo);
        setSelectCount(fileSelects.size());
    }

    public void removeSendALL(List infos) {
        fileSelects.removeAll(infos);
        setSelectCount(fileSelects.size());
    }

    public void removeSendALL() {
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


    /**
     * 获取外部分享的文件
     *
     * @return
     */
    public List<FileInfo> getExternalShareFiles(Intent intent) {
        List<FileInfo> uris = new ArrayList<>();
        if (intent == null) {
            return uris;
        }

        Uri uri = intent.getParcelableExtra(intent.EXTRA_STREAM);
        if (uri != null) {
            uris.add(new UriFileInfo(uri));
        } else {
            List<Uri> files = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (files != null && !files.isEmpty()) {
                for (Uri file : files) {
                    uris.add(new UriFileInfo(file));
                }
            }
        }
        return uris;
    }


    public void requestAccessAndroidData() {
        try {
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            //flag看实际业务需要可再补充
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_VISIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void startForRoot() {
        Uri uri1 = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata");
        String uri = FileUtil.changeToUri(Environment.getExternalStorageDirectory().getPath());
        uri = uri + "/document/primary%3A" + Environment.getExternalStorageDirectory().getPath().replace("/storage/emulated/0/", "").replace("/", "%2F");
        Uri parse = Uri.parse(uri);
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri1);
        Intent intent1 = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent1.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile.getUri());
        startActivityForResult(intent1, REQUEST_VISIT);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VISIT && data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Uri uri = data.getData();
                //这个是保存权限的
                getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));//关键是这里，这个就是保存这个目录的访问权限
            }

        }
        if (requestCode == REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(resultCode, data);
            String qrContent = scanResult.getContents();
            if (!StringUtils.isEmpty(qrContent)) {
                try {
                    // 先判断二维码是否是添加设备
                    AddDevice addDevice = new Gson().fromJson(qrContent, AddDevice.class);
                    if (!Config.KEY.equals(addDevice.getKey())) {
                        throw new RuntimeException();
                    }
                    ThreadUtils.runThread(() -> {
                        List<Device> devices = addDevice.getDevices();
                        if (devices != null && !devices.isEmpty()) {
                            for (Device device : devices) {
                                Socket socket = new Socket();
                                try {
                                    PrefUtil prefUtil = App.getPrefUtil();
                                    socket.connect(new InetSocketAddress(device.getDevIP(), device.getDevPort()), 2000);
                                    InputStream inputStream = socket.getInputStream();
                                    OutputStream outputStream = socket.getOutputStream();
                                    String uuid = StringUtils.getUUID();
                                    byte[] buffer = new byte[1024];
                                    String userName = prefUtil.getString(PreConfig.USER_NAME);
                                    DataEnc dataEnc = new DataEnc(buffer);
                                    dataEnc.setCmd(mCmd.FS_ADD_DEVICE);
                                    dataEnc.putString(uuid);

                                    Device mDevice = new Device();
                                    mDevice.setDevName(prefUtil.getString(PreConfig.USER_NAME));
                                    mDevice.setDevPort(Config.FILE_SERVER_PORT);
                                    mDevice.setDevMode(Device.ANDROID);

                                    byte[] objectBytes = ByteUtil.objectToByte(mDevice);
                                    dataEnc.putBytes(objectBytes);
                                    IOUtil.write(outputStream, dataEnc);
                                    DataDec dataDec = new DataDec(buffer);
                                    if (IOUtil.read(inputStream, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                                        continue;
                                    int length = dataDec.getLength();
                                    if (IOUtil.read(inputStream, buffer, DataEnc.getHeaderSize(), length) != length)
                                        continue;
                                    String sgin = dataDec.getString();
                                    if (sgin.equals(Config.sgin(uuid))) {
                                        String address = device.getDevIP() + ":" + device.getDevPort();
                                        LANService.getInstance().addDevice(device);
                                        T.s("添加设备 " + device.getDevName() + " 成功!");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    fragChat.setEditContent(qrContent);
                }
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



























