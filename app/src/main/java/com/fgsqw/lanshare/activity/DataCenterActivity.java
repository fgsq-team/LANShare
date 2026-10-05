package com.fgsqw.lanshare.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.*;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.IpConfiguration;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.dialog.ApkUpdateDialog;
import com.fgsqw.lanshare.dialog.DeviceSelectDialog;
import com.fgsqw.lanshare.dialog.FileSendDialog;
import com.fgsqw.lanshare.fragment.FragmentChat;
import com.fgsqw.lanshare.fragment.FragmentFiles;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageUriContent;
import com.fgsqw.lanshare.pojo.network.NetInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.service.MusicService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.fgsqw.lanshare.utils.PermissionsUtils.REQUEST_VISIT;
import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;


public class DataCenterActivity extends BaseActivity implements View.OnClickListener,
        Toolbar.OnMenuItemClickListener,
        View.OnLongClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "DataCenterActivity";
    // 扫码按钮
    private LinearLayout layScanCode;
    private Toolbar mainToobar;
    private TextView mainIp;
    private TextView mainName;
    private Switch webSwitch;
    // 记录
    private ImageView imgRecord;
    private TextView tvRecord;
    private LinearLayout bottomRecord;
    // 文件
    private ImageView imgFiles;
    private TextView tvFiles;
    private LinearLayout bottomFiles;
    private boolean isMenuVisible = false;
    // 发送
    private LinearLayout bottomSend;
    private ImageView imgSend;
    // 底部导航
    private LinearLayout bottomLayout;
    private LinearLayout bottomBottomMenu;
    private LinearLayout bottomDeleteMessage;
    private ImageView bottomDeleteMessageImage;
    private final List<BaseFragment> fragmentList = new ArrayList<>();
    private static BaseFragment currentFragment;
    // 文件
    private FragmentFiles fragmentFiles;
    // 消息
    private FragmentChat fragmentChat;
    public List<MessageFileContent> fileSelects = new LinkedList<>();

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            sendHandleMessage(msg);
        }
    };

    private String ip;
    private int checkCount = 0;
    private int whichFragmentIndex = -1;
    private boolean showSortFileMenu = false;
    private boolean showSearchFileTypes = false;
    private boolean showUpdateApps = false;
    private boolean showSortFileMenu_backup = false;
    private boolean showSearchFileTypes_backup = false;
    private boolean showUpdateApps_backup = false;

    /**
     * @author fgsq
     * @comments 应用内部全局消息处理
     * @date 2024/6/28 11:57
     */
    public void sendHandleMessage(Message message) {
        if (message.what == LCmd.SERVICE_UPDATE_APPS) {
            Object[] objs = (Object[]) message.obj;
            Device device = (Device) objs[0];
            JSONArray jsonArray = (JSONArray) objs[1];
            List<MessageApkContent> apkInfos = new ArrayList<>();
            List<MessageApkContent> apkFileList = AnyData.apkFileList;
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String packageName = jsonObject.getString("packageName");
                for (MessageApkContent apkInfo : apkFileList) {
                    if (packageName.equals(apkInfo.getPackageName())) {
                        MessageApkContent clone = new MessageApkContent();
                        clone.setVersionCode(apkInfo.getVersionCode());
                        clone.setVersionName(apkInfo.getVersionName());
                        clone.setName(apkInfo.getName());
                        clone.setPackageName(apkInfo.getPackageName());
                        clone.setLength(apkInfo.getLength());
                        clone.setPath(apkInfo.getPath());
                        clone.setIcon(apkInfo.getIcon());
                        clone.setVersionName(jsonObject.getString("versionName"));
                        apkInfos.add(clone);
                        break;
                    }
                }
            }
            ApkUpdateDialog dialog = new ApkUpdateDialog(this, apkInfos);
            dialog.setOnUpdate(apkInfos1 -> LANService.getInstance().updateApp(device, (JSONArray) JSONArray.toJSON(apkInfos1)));
            dialog.show();
        } else if (message.what == LCmd.SERVICE_NETWORK_CHANGES) {    // 网络变化
            String ipAddress = (String) message.obj;
            if (Config.WEB_SERVICE && !StringUtils.isEmpty(ipAddress)) {
                updateIP(ipAddress + ":" + Config.FILE_SERVER_PORT);
            } else {
                updateIP(ipAddress);
            }
        } else {
            for (BaseFragment fragment : fragmentList) {
                fragment.handleMessage(message);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    @SuppressLint("InlinedApi")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //设置底部导航颜色
        setBottomNavigationBarColor();
        initView();
        // 初始化碎片
        initFragment();
        // 启动LANShare主服务
        Intent lanService = new Intent();
        lanService.setClass(this, LANService.class);
        lanService.putExtra("messenger", new Messenger(handler));
        startService(lanService);
        // 外部数据共享监听
        processExtraData();
        // 预加载耗时数据
        preloadData();
        // 检测是否有通知权限，没有则申请
        boolean b = PermissionsUtils.checkPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS});
        if (!b) {
            PermissionsUtils.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PermissionsUtils.REQUEST_POST_NOTIFICATIONS);
        }
        // 判断软件是否是开机启动，是开机启动直接进入后台运行
        Intent intent = getIntent();
        boolean autoStart = intent.getBooleanExtra(PreConfig.AUTO_START, false);
        if (autoStart) {
            moveTaskToBack(true);
        }
        mUtil.checkUpdate(false, false, this);
    }

    public void updateIP(String ip) {
        mainIp.setText(ip);
        this.ip = ip;
    }

    @Override
    protected void onStart() {
        super.onStart();
        getConfig();
        // 刷新IP显示，防止Activity重建后IP为空
        refreshIpDisplay();
    }

    /**
     * 刷新IP地址显示
     * <p>从LANService获取当前网络信息并更新IP显示，解决切换界面回来IP为空的问题</p>
     */
    private void refreshIpDisplay() {
        LANService service = LANService.getInstance();
        if (service == null) return;
        Set<Device> localDevices = service.getDeviceManager().localDevices;
        List<NetInfo> ipv6NetInfoList = service.getDeviceManager().ipv6NetInfoList;
        if (!localDevices.isEmpty()) {
            String devIp = localDevices.iterator().next().getDevIP();
            if (Config.WEB_SERVICE) {
                updateIP("http://" + devIp + ":" + Config.FILE_SERVER_PORT);
            } else {
                updateIP(devIp);
            }
        } else if (ipv6NetInfoList != null && !ipv6NetInfoList.isEmpty()) {
            String ipv6 = ipv6NetInfoList.get(0).getIp();
            if (Config.WEB_SERVICE) {
                updateIP("http://[" + ipv6 + "]:" + Config.FILE_SERVER_PORT);
            } else {
                updateIP(ipv6);
            }
        }
    }

    private void processExtraData() {
        Intent intent = getIntent();
        List<MessageFileContent> externalShareFiles = getExternalShareFiles(intent);
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

    public void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher_round)
                .setCancelable(false)
                .setTitle(getString(R.string.port_changes_notice))
                .setMessage(R.string.port_changes_require_a_software_restart)
                .setPositiveButton(getString(R.string.exit), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    stopService(new Intent(this, LANService.class));
                    finish();
                    finish();
                }).setNegativeButton(R.string.not_exit, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        builder.create().show();
    }

    /**
     * @author fgsq
     * @comments 初始化配置文件
     * @date 2024/6/28 11:57
     */
    public void getConfig() {
        Config.initConfig(prefUtil);
        webSwitch.setOnCheckedChangeListener(null);
        webSwitch.setChecked(Config.WEB_SERVICE);
        webSwitch.setOnCheckedChangeListener(this);
       /* if (tcpPort != Config.FILE_SERVER_PORT || udpPort != Config.UDP_PORT) {
            showExitDialog();
        }*/
    }

    // 设置底部导航颜色
    public void setBottomNavigationBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.bottomBackgroundColor));
        }
    }

    public void initView() {
        bottomRecord = bind(R.id.bottom_record);
        layScanCode = bind(R.id.main_scan_code);
        imgRecord = bind(R.id.img_record);
        tvRecord = bind(R.id.bottom_record_tv);
        mainName = bind(R.id.main_name);
        mainIp = bind(R.id.main_ip);
        webSwitch = bind(R.id.main_web_switch);

        bottomSend = bind(R.id.bottom_send);
        imgSend = bind(R.id.img_send);

        bottomFiles = bind(R.id.bottom_files);
        imgFiles = bind(R.id.img_files);
        tvFiles = bind(R.id.bottom_files_tv);

        bottomLayout = bind(R.id.bottom_container);
        bottomBottomMenu = bind(R.id.bottom_bottom_menu);
        bottomDeleteMessage = bind(R.id.bottom_delete_message);
        bottomDeleteMessageImage = bind(R.id.bottom_delete_message_image);

        bottomRecord.requestFocus();
        mainToobar = bind(R.id.main_toolbar);
        setSupportActionBar(mainToobar);

//        mainToobar.inflateMenu(R.menu.toolbar_menu);
        mainToobar.setOnMenuItemClickListener(this);
        bottomRecord.setOnClickListener(this);
        bottomSend.setOnClickListener(this);
        bottomSend.setOnLongClickListener(this);
        bottomFiles.setOnClickListener(this);
        bottomFiles.setOnLongClickListener(this);
        layScanCode.setOnClickListener(this);
        layScanCode.setOnLongClickListener(this);
        mainIp.setOnClickListener(this);
        mainIp.setOnLongClickListener(this);
        webSwitch.setOnCheckedChangeListener(this);
        bottomDeleteMessageImage.setOnClickListener(this);
    }


    private void initFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // 通过FragmentManager查找已存在的Fragment实例
        FragmentChat existingFragmentChat = (FragmentChat) fragmentManager.findFragmentByTag("FragmentChat");
        if (existingFragmentChat != null) {
            fragmentChat = existingFragmentChat;
        } else {
            fragmentChat = new FragmentChat();
        }
        // 检查fragmentList是否已包含fragmentChat，避免重复添加
        if (!fragmentList.contains(fragmentChat)) {
            fragmentList.add(fragmentChat);
        }
        FragmentFiles existingFragmentFiles = (FragmentFiles) fragmentManager.findFragmentByTag("FragmentFiles");
        int whichFragment = 0;
        if (existingFragmentFiles != null) {
            fragmentFiles = existingFragmentFiles;
            whichFragment = fragmentFiles.isHidden() ? 0 : 1;
        } else {
            fragmentFiles = new FragmentFiles();
        }
        // 检查fragmentList是否已包含fragmentFiles，避免重复添加
        if (!fragmentList.contains(fragmentFiles)) {
            fragmentList.add(fragmentFiles);
        }
        switchFragment(whichFragment);
    }


    private void switchFragment(int whichFragment) {
        if (whichFragmentIndex == whichFragment) {
            return;
        }
        whichFragmentIndex = whichFragment;
        if (whichFragment == 0) {
            tvRecord.setTextColor(getResources().getColor(R.color.text_select));
            tvFiles.setTextColor(getResources().getColor(R.color.textNotSelectColor));
            imgRecord.setColorFilter(getResources().getColor(R.color.text_select));
            imgFiles.setColorFilter(getResources().getColor(R.color.textNotSelectColor));
        } else {
            tvRecord.setTextColor(getResources().getColor(R.color.textNotSelectColor));
            tvFiles.setTextColor(getResources().getColor(R.color.text_select));
            imgRecord.setColorFilter(getResources().getColor(R.color.textNotSelectColor));
            imgFiles.setColorFilter(getResources().getColor(R.color.text_select));
        }
        Fragment fragment = fragmentList.get(whichFragment);
        setFragment(fragment);
    }


    public void setFragment(Fragment fragment) {
        int frameLayoutId = R.id.fl_container;

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // 检查是否已经添加了该fragment
            if (fragment.isAdded()) {
                // 如果当前fragment不为空且不同于要显示的fragment，则隐藏当前fragment
                if (currentFragment != null && currentFragment != fragment) {
                    if (currentFragment.isAdded() && !currentFragment.isHidden()) {
                        transaction.hide(currentFragment);
                    }
                }
                // 显示目标fragment
                if (fragment.isHidden()) {
                    transaction.show(fragment);
                }
            } else {
                // 如果fragment未添加到FragmentManager中
                // 隐藏当前fragment（如果存在）
                if (currentFragment != null && currentFragment.isAdded()) {
                    transaction.hide(currentFragment);
                }
                // 添加新fragment，并指定tag便于查找
                String tag = null;
                if (fragment instanceof FragmentChat) {
                    tag = "FragmentChat";
                } else if (fragment instanceof FragmentFiles) {
                    tag = "FragmentFiles";
                }
                if (tag != null) {
                    transaction.add(frameLayoutId, fragment, tag);
                } else {
                    transaction.add(frameLayoutId, fragment);
                }
            }
            currentFragment = (BaseFragment) fragment;
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.main_scan_code: {
                startActivity(new Intent(this, DeviceQrCodeActivity.class));
                break;
            }
            case R.id.main_ip:
            case R.id.qr_code: {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("text", ip));
                T.s((R.string.copied_to_clipboard));
                break;
            }
            case R.id.bottom_files: {
                fragmentFiles.clearSelect();
                fileSelects.clear();
                setSelectedCount(fileSelects.size());
                T.s(R.string.selected_data_has_been_cleared);
                break;
            }
            case R.id.bottom_send: {
                if (fileSelects.isEmpty()) {
                    T.s(R.string.please_select_file);
                    return false;
                }
                showSelectDeviceDialog(fileSelects);
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
                checkCount++;
                if (checkCount == 3) {
                    T.s("再点击两次唤出菜单");
                } else if (checkCount == 5) {
                    checkCount = 0;
                    PopupMenu popupMenu = new PopupMenu(this, v);
                    popupMenu.getMenuInflater().inflate(R.menu.toolbar_menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(this::onMenuItemClick);
                    popupMenu.show();
                }
                switchFragment(0);
                showSortFileMenu_backup = showSearchFileTypes;
                showSearchFileTypes_backup = showSortFileMenu;
                showUpdateApps_backup = showUpdateApps;
                showSearchFileTypes = false;
                showSortFileMenu = false;
                showUpdateApps = false;
                break;
            }
            case R.id.bottom_send: {
                checkCount = 0;
                if (fileSelects.isEmpty()) {
                    T.s(R.string.please_select_file);
                    return;
                }
                sendFiles(fileSelects);
                break;
            }
            case R.id.bottom_files: {
                checkCount = 0;
                switchFragment(1);
                showSearchFileTypes = showSortFileMenu_backup;
                showSortFileMenu = showSearchFileTypes_backup;
                showUpdateApps = showUpdateApps_backup;
                break;
            }
            case R.id.main_scan_code: {
                //打开扫描界面
                IntentIntegrator intentIntegrator = new IntentIntegrator(this);
                intentIntegrator.setOrientationLocked(false);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                intentIntegrator.setCaptureActivity(ZxingActivity.class); // 设置自定义的activity是QRActivity
                intentIntegrator.setRequestCode(REQUEST_CODE);
                intentIntegrator.initiateScan();
                break;
            }
            case R.id.main_ip: {
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.add_device_qrcode, null);
                ImageView qrCode = view.findViewById(R.id.qr_code);
                CheckBox qrIpv6 = view.findViewById(R.id.qr_ipv6);
                TextView tvIpAddress = view.findViewById(R.id.qr_tv_ip_address);
                LinearLayout qrLayout = view.findViewById(R.id.qr_layout);
                String ipAddress = mainIp.getText().toString();
                tvIpAddress.setText(ipAddress);

                Bitmap qrcode = QrCodeUtils.qrcode(ipAddress, 800, 800);
                qrCode.setImageBitmap(qrcode);
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setView(view)
                        .create();
//                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                alertDialog.show();
                qrCode.setOnLongClickListener(this);
                qrIpv6.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        List<NetInfo> ipv6NetInfoList = LANService.getInstance().getDeviceManager().ipv6NetInfoList;
                        if (ipv6NetInfoList.isEmpty()) {
                            T.s((R.string.failed_to_get_device_ipv6));
                            return;
                        }
                        ip = "http://[" + ipv6NetInfoList.get(0).getIp() + "]:" + Config.FILE_SERVER_PORT;
                        Bitmap qrcode1 = QrCodeUtils.qrcode(ip, 800, 800);
                        qrCode.setImageBitmap(qrcode1);
                        tvIpAddress.setText(ip);
                    } else {
                        ip = mainIp.getText().toString();
                        Bitmap qrcode1 = QrCodeUtils.qrcode(ip, 800, 800);
                        qrCode.setImageBitmap(qrcode1);
                    }
                    tvIpAddress.setText(ip);
                });
                break;
            }
            case R.id.bottom_delete_message_image:
                fragmentChat.deleteSelectedMessages();
                break;
            default:
                break;
        }
    }

    /**
     * @author fgsq
     * @comments 设置多选删除模式
     * @date 2024/5/18 11:42
     */
    @SuppressLint("WrongConstant")
    public void setDeleteMode(boolean deleteMode) {
        fragmentChat.setChatEditVisibility(deleteMode);
        if (deleteMode) {
            bottomDeleteMessage.setVisibility(View.VISIBLE);
            bottomBottomMenu.setVisibility(View.GONE);
        } else {
            bottomDeleteMessage.setVisibility(View.GONE);
            bottomBottomMenu.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @author fgsq
     * @comments 发送单个文件
     * @date 2024/5/18 11:40
     */
    public void sendSingleFile(MessageFileContent fileInfo) {
        sendFiles(mUtil.singletonArrayList(fileInfo));
    }

    public void showSelectDeviceDialog(List<MessageFileContent> fileSelects) {
        fileSelects = mUtil.deepCopyList(fileSelects);
        FileSendDialog dialog = new FileSendDialog(this, fileSelects.size());
        List<MessageFileContent> finalFileSelects = fileSelects;
        dialog.setOnDeviceSelect(device -> {
            LANService.getInstance().fileSend(LANService.getInstance().getDeviceManager().getDevice(device), device, new LinkedList<>(finalFileSelects));
            fragmentFiles.clearSelect();
            finalFileSelects.clear();
            DataCenterActivity.this.fileSelects.clear();
            setSelectedCount(finalFileSelects.size());
        });
        dialog.show();
    }

    /**
     * @author fgsq
     * @comments 发送多个文件
     * @date 2024/5/18 11:40
     */
    public void sendFiles(List<MessageFileContent> fileSelects) {
        fileSelects = mUtil.deepCopyList(fileSelects);
        if (Config.DEFAULT_SELECT_ONLY_ONE_DEVICE) {
            // 只有一个设备时默认选择这个=设备发送文件
            Map<String, Device> onLineDevices = LANService.getInstance().getOnLineDevices();
            int size = onLineDevices.size();
            if (size == 1) {
                Device device = onLineDevices.values().iterator().next();
                T.s("已默认发送数据到：" + device.getDevName());
                LANService.getInstance().fileSend(LANService.getInstance().getDeviceManager().getDevice(device), device, new LinkedList<>(fileSelects));
                fragmentFiles.clearSelect();
                fileSelects.clear();
                DataCenterActivity.this.fileSelects.clear();
                setSelectedCount(fileSelects.size());
                return;
            }
        }
        FileSendDialog dialog = new FileSendDialog(this, fileSelects.size());
        List<MessageFileContent> finalFileSelects = fileSelects;
        dialog.setOnDeviceSelect(device -> {
            LANService.getInstance().fileSend(LANService.getInstance().getDeviceManager().getDevice(device), device, new LinkedList<>(finalFileSelects));
            fragmentFiles.clearSelect();
            finalFileSelects.clear();
            DataCenterActivity.this.fileSelects.clear();
            setSelectedCount(finalFileSelects.size());
        });
        dialog.show();
    }

    public void setSelectedCount(int count) {
        String str;
        if (count <= 0) {
            str = getString(R.string.file);
        } else if (count > 999) {
            str = getString(R.string.selected) + "(999+)";
        } else {
            str = getString(R.string.selected) + "(" + count + ")";
        }
        tvFiles.setText(str);
    }

    @SuppressLint("SetTextI18n")
    public boolean addASendFile(MessageFileContent fileInfo) {
        if (fileSelects.size() >= 1000) return false;
        fileSelects.add(fileInfo);
        setSelectedCount(fileSelects.size());
        return true;
    }

    public void removeSendFile(MessageFileContent fileInfo) {
        fileSelects.remove(fileInfo);
        setSelectedCount(fileSelects.size());
    }

    public void removeSendALL(List infos) {
        fileSelects.removeAll(infos);
        setSelectedCount(fileSelects.size());
    }

    public void removeSendALL() {
        fileSelects.clear();
    }

    @Override
    public void openOptionsMenu() {
        final View toolbar = getWindow().getDecorView().findViewById(R.id.main_toolbar);
        if (toolbar instanceof Toolbar) {
            ((Toolbar) toolbar).showOverflowMenu();
        } else {
            super.openOptionsMenu();
        }
    }

/*
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
     */
/*   if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_DOWN) {
            mainToobar.requestFocus();
            mainToobar.showOverflowMenu();
            // 禁止焦点转移
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
            // 禁止焦点转移
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            return true;
        }*//*

        return true;
    }
*/

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        return true;
    }

    /**
     * 获取外部分享的文件
     *
     * @return
     */
    public List<MessageFileContent> getExternalShareFiles(Intent intent) {
        List<MessageFileContent> uris = new ArrayList<>();
        if (intent == null || intent.getAction() == null) {
            return uris;
        }
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = uri = intent.getData();
            if (uri == null) {
                return uris;
            }
            MessageUriContent uriFileInfo = new MessageUriContent(uri);
            uris.add(uriFileInfo);
        } else if (intent.getAction().equals(Intent.ACTION_SEND)) { // 单选文件发送
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri == null) {
                uri = intent.getData();
            }
            if (uri != null) {
                MessageUriContent uriFileInfo = new MessageUriContent(uri);
                uris.add(uriFileInfo);
            } else {
               /* String aPackage = intent.getPackage();
                System.out.println("aPackage = " + aPackage);
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    // 获取 android.intent.extra.TEXT 数据
                    String text = bundle.getString(Intent.EXTRA_TEXT);
                    if (text != null) {
                        // 分离地址和链接
                        String[] parts = text.split(" ");
                        String locationName = "";
                        String mapLink = "";
                        if (parts.length == 1) {
                            locationName = "";
                            mapLink = parts[0];      // 高德地图链接
                        } else if (parts.length == 2) {
                            locationName = parts[0]; // 位置名称
                            mapLink = parts[1];      // 高德地图链接
                        }
                        MapInfo mapInfo = new MapInfo();
                        mapInfo.setName(locationName);
                        mapInfo.setPath(mapLink);
                        // 打印或使用解析后的数据
                        Log.d("MapData", "位置名称: " + locationName);
                        Log.d("MapData", "地图链接: " + mapLink);
                        uris.add(mapInfo);
                    }
                }*/
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) { // 多选文件发送
            List<Uri> files = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (files != null && !files.isEmpty()) {
                for (Uri file : files) {
                    MessageUriContent uriFileInfo = new MessageUriContent(file);
                    uris.add(uriFileInfo);
                }
            }
        }
        return uris;
    }


    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VISIT) {        // 保存目录访问权限
            if (data == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Uri uri = data.getData();
                //这个是保存权限的
                getContentResolver()
                        .takePersistableUriPermission(
                                uri,
                                data.getFlags()
                                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        );//关键是这里，这个就是保存这个目录的访问权限
            }

        } else if (requestCode == SettingActivity.SETTING_REQUEST_CODE) {
            if (data == null) {
                return;
            }
            boolean portUpdate = data.getBooleanExtra("portUpdate", false);
            if (portUpdate) {
                showExitDialog();
            }
        } else if (requestCode == REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(resultCode, data);
            String qrContent = scanResult.getContents();
            try {
                if (StringUtils.isEmpty(qrContent)) {
                    return;
                }
                if (!qrContent.contains("-")) {
                    throw new RuntimeException();
                }
                String[] split = qrContent.split("-");
                if (split.length != 4) {
                    throw new RuntimeException();
                }
                if (!split[0].equals(Config.KEY)) {
                    throw new RuntimeException();
                }
                try {
                    ThreadUtils.runThread(() -> {
                        boolean isIPV6 = split[1].equals("1");
//                        if (isIPV6) {
//                        }
                        String ip = split[2];
                        int port = Integer.parseInt(split[3]);
                        PrefUtil prefUtil = App.getPrefUtil();
                        try {
                            List<NetInfo> ipv6NetInfoList = NetWorkUtil.getOpenIpv6();
                            if (ipv6NetInfoList.isEmpty()) {
                                T.s((R.string.ipv6_address_not_found));
                                return;
                            }
                            Device device = null;
                            InetAddress inetAddress = null;
                            if (isIPV6) {
                                inetAddress = Inet6Address.getByName(ip);
                                device = LANService.getInstance().getDeviceManager().makeIPv6Device();
                            } else {
                                String[] split1 = ip.split(",");
                                Set<Device> localDevices = LANService.getInstance().getDeviceManager().localDevices;
                                flag:
                                for (Device localDevice : localDevices) {
                                    for (String s : split1) {
                                        if (NetWorkUtil.subNet(localDevice.getDevIP(), s, localDevice.getDevNetMask())) {
                                            inetAddress = InetAddress.getByName(s);
                                            device = localDevice;
                                            break flag;
                                        }
                                    }
                                }
                            }
                            if (inetAddress == null) {
                                T.s((R.string.ip_is_not_in_local_network));
                            }
                            Socket socket = LANService.getInstance().makeSocket(inetAddress, port);
                            DataEnc dataEnc = LANService.getInstance().getDeviceManager().makeDataEnc(device, null, 1024);
                            dataEnc.setCmd(LCmd.FS_ADD_DEVICE);
                            dataEnc.putBool(isIPV6);
                            InputStream inputStream = socket.getInputStream();
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(dataEnc.getData());
                            outputStream.flush();
                            TimeUnit.MILLISECONDS.sleep(10);
                            byte[] buffer = new byte[1024];
                            DataDec dataDec = new DataDec(buffer);
                            if (IOUtil.read(inputStream, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                                return;
                            int thatLength = dataDec.getLength();
                            if (IOUtil.read(inputStream, buffer, DataEnc.getHeaderSize(), thatLength) != thatLength)
                                return;
                            // 设备端口
                            int devPort = dataDec.getInt();
                            // 设备ip
                            String devIp = dataDec.getString();
                            // 设备名
                            String devName = dataDec.getString();
                            // 设备类型
                            int devMode = dataDec.getInt();
                            // 设备唯一码
                            String uniqueUUid = dataDec.getString();
                            int dataVersion = dataDec.getInt();
                            boolean isIPv6 = dataDec.getBool();
                            Device addDevice = new Device();
                            addDevice.setDevPort(devPort);
                            addDevice.setDevIP(devIp);
                            addDevice.setIPv4(!isIPv6);
                            addDevice.setDevName(devName);
                            addDevice.setDevMode(devMode);
                            addDevice.setSetTime(System.currentTimeMillis());
                            addDevice.setDataVersion(dataVersion);
                            addDevice.setCanRemove(false);
                            addDevice.setUniqueUUid(uniqueUUid);
                            LANService.getInstance().addDevice(addDevice);
                            T.s(String.format(getString(R.string.add_device_successful), device.getDevName()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    });
                } catch (Exception e) {
                    fragmentChat.setEditContent(qrContent);
                    LLog.error("addDevice", e);
                }
            } catch (RuntimeException e) {
                fragmentChat.setEditContent(qrContent);
                return;
            }
        }
    }

    // 隐藏底部导航
    public void hideBottom() {
        bottomLayout.setVisibility(View.GONE);
    }

    // 显示底部导航
    public void showBottom() {
        bottomLayout.setVisibility(View.VISIBLE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_sort_file_type);
        item.setVisible(showSortFileMenu);
        MenuItem item1 = menu.findItem(R.id.menu_search_file_types);
        item1.setVisible(showSearchFileTypes);
        MenuItem item2 = menu.findItem(R.id.menu_update_apps);
        item2.setVisible(showUpdateApps);
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (currentFragment.onKeyDown(keyCode, keyEvent)) {
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (keyEvent.getRepeatCount() == 0)) {
            if (!currentFragment.onBack()) {
                moveTaskToBack(true);
                return true;
            }
            if (bottomLayout.getVisibility() == View.GONE) {
                bottomLayout.setVisibility(View.VISIBLE);
                return true;
            }
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    private void setSearchFileTypes() {
        // 创建构造器
        AlertDialog.Builder builder = new AlertDialog.Builder(DataCenterActivity.this);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setTitle("搜索文件分类");
        // 设置内容,
        final String[] cities = {
                "应用",
                "媒体",
                "音频",
                "文件"
        };
        Config.SEARCH_FLAG =
                JSONArray.parseObject(
                        prefUtil.getString(PreConfig.SEARCH_FILE_TYPES,
                                "[true, true, true, true]"), boolean[].class
                );
        builder.setMultiChoiceItems(cities, Config.SEARCH_FLAG, (dialog, which, isChecked) -> Config.SEARCH_FLAG[which] = isChecked);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String jsonString = JSON.toJSONString(Config.SEARCH_FLAG);
            prefUtil.saveString(PreConfig.SEARCH_FILE_TYPES, jsonString);
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
        });
        // 显示dialog
        builder.create().show();
    }

    /**
     * 文件排序方式
     */
    private void setShowSortFileMenu() {
        // 创建构造器
        AlertDialog.Builder builder = new AlertDialog.Builder(DataCenterActivity.this);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setTitle("选择排序方式");
        // 设置内容,
        final String[] cities = {
                "文件名排序",
                "文件大小排序",
                "时间排序",
                "文件名排序-倒序",
                "文件大小排序-倒序",
                "时间排序-倒序",
        };
        int fileSortMethod = prefUtil.getInt(PreConfig.FILE_SORT_METHOD, 0);
        builder.setSingleChoiceItems(cities, fileSortMethod, (dialog, which) -> prefUtil.saveInt(PreConfig.FILE_SORT_METHOD, which));
        builder.setPositiveButton("确定", (dialog, which) -> {
            Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_SYNC_SORT;
            sendHandleMessage(mMessage);
        });
        builder.setNegativeButton("取消", (dialog, which) -> prefUtil.saveInt(PreConfig.FILE_SORT_METHOD, fileSortMethod));
        // 显示dialog
        builder.create().show();
    }

    public void setSortFileTypeMenuVisible(boolean visible) {
        showSortFileMenu = visible;
    }

    public void setShowUpdateApps(boolean visible) {
        showUpdateApps = visible;
    }

    public void setSearchFileTypesVisible(boolean visible) {
        showSearchFileTypes = visible;
    }

    public void updateAppsFromOtherDevice() {
        DeviceSelectDialog deviceSelectDialog = new DeviceSelectDialog(this);
        deviceSelectDialog.setOnDeviceSelect(device -> LANService.getInstance().updateAppsFromOtherDevice(device));
        deviceSelectDialog.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_search_file_types:
                setSearchFileTypes();
                break;
            case R.id.menu_update_apps:
                updateAppsFromOtherDevice();
                break;
            case R.id.menu_sort_file_type:
                setShowSortFileMenu();
                break;
            case R.id.menu_setting:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, SettingActivity.SETTING_REQUEST_CODE);
                break;
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.menu_delete_message:
                if (fragmentChat != null) {
                    fragmentChat.messageDelete();
                }
                break;
            case R.id.menu_exit:
                stopService(new Intent(this, LANService.class));
                stopService(new Intent(this, MusicService.class));
                finish();
                break;
            case R.id.menu_web_manager:
                startActivity(new Intent(this, WebClientManagerActivity.class));
                break;
            case R.id.menu_file_sync:
                startActivity(new Intent(this, FileSyncManagerActivity.class));
                break;
//            case R.id.menu_acquire_advanced_version:
//                T.s("敬请期待。。。。");
//                break;打赏时候可以备注昵称,后期会将打赏名单放入网页

           /* case R.id.menu_camera_send:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.menu_camera_recv:
                startActivity(new Intent(this, TestActivity.class));
                break;*/

        }
        return true;
    }


    /**
     * 预加载文件
     */
    public void preloadData() {
        ThreadUtils.runThread(() -> {
            DeviceDataScanner.scanInstalledApps(DataCenterActivity.this, true);
        });
        ThreadUtils.runThread(() -> {
            DeviceDataScanner.scanImages(DataCenterActivity.this, true);
            if (Config.MEDIA_SYNC) {
                LANService.getInstance().syncMedia();
            }
        });
        ThreadUtils.runThread(() -> {
            DeviceDataScanner.scanAudioFiles(DataCenterActivity.this, true);
        });
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.main_web_switch: {
                PrefUtil prefUtil = App.getPrefUtil();
                prefUtil.saveBoolean(PreConfig.WEB_SERCICE, isChecked);
                Config.WEB_SERVICE = isChecked;
                LANService instance = LANService.getInstance();
                Set<Device> localDevices = instance.getDeviceManager().localDevices;
                List<NetInfo> ipv6NetInfoList = instance.getDeviceManager().ipv6NetInfoList;
                if (isChecked) {
                    T.s((R.string.web_service_has_been_enabled));
                    if (!localDevices.isEmpty()) {
                        updateIP("http://" + localDevices.iterator().next().getDevIP() + ":" + Config.FILE_SERVER_PORT);
                    } else if (ipv6NetInfoList != null && !ipv6NetInfoList.isEmpty()) {
                        updateIP("http://[" + ipv6NetInfoList.get(0).getIp() + "]:" + Config.FILE_SERVER_PORT);
                    }
                } else {
                    T.s((R.string.web_service_has_been_disabled));
                    if (!localDevices.isEmpty()) {
                        updateIP(localDevices.iterator().next().getDevIP());
                    } else if (ipv6NetInfoList != null && !ipv6NetInfoList.isEmpty()) {
                        updateIP(ipv6NetInfoList.get(0).getIp());
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    public void searchFile(String path, String str) {
        fragmentFiles.searchFile(path, str);
    }
}



























