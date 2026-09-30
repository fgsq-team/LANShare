package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.EditTextDialog;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.AESUtils;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.utils.PermissionsUtils;
import com.fgsqw.lanshare.utils.PrefUtil;

public class SettingActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {

    public static final int SETTING_REQUEST_CODE = 0x0000f01e; // Only use bottom 16 bits

    ImageView setting_exit_img;
    LinearLayout setting_dev_name;
    LinearLayout setting_save_path;
    LinearLayout setting_message_key;
    LinearLayout setting_save_message;
    LinearLayout setting_not_recv_dialog;
    LinearLayout setting_open_media_internal_player;
    LinearLayout setting_media_select_model;
    LinearLayout setting_check_show_hidden_files;
    LinearLayout setting_tcp_port;
    LinearLayout setting_udp_port;
    LinearLayout setting_auto_start;
    LinearLayout setting_receive_broadcast_messages;
    LinearLayout setting_broadcast_message;
    LinearLayout setting_send_mute;
    LinearLayout setting_receivce_mute;
    LinearLayout setting_enc_data;
    LinearLayout setting_web_service;
    LinearLayout setting_sync_notification;
    LinearLayout setting_web_service_open;
    LinearLayout setting_message_notification;
    LinearLayout setting_save_files_category_path;
    LinearLayout setting_media_sync;
    LinearLayout setting_save_files_path;
    LinearLayout setting_default_select_only_one_device;
    LinearLayout setting_theme; // 添加主题设置项

    Switch setting_not_recv_dialog_switch;
    Switch setting_open_media_switch;
    Switch setting_media_select_model_switch;
    Switch setting_save_message_switch;
    Switch setting_save_to_gallery_switch;
    Switch setting_show_hidden_files_switch;
    Switch setting_auto_start_switch;
    Switch setting_receive_broadcast_messages_switch;
    Switch setting_broadcast_message_switch;
    Switch setting_send_mute_switch;
    Switch setting_receivce_mute_switch;
    Switch setting_enc_data_switch;
    Switch setting_web_service_switch;
    Switch setting_sync_notification_switch;
    Switch setting_web_service_open_switch;
    Switch setting_message_notification_switch;
    Switch setting_save_files_path_switch;
    Switch setting_save_files_category_path_switch;
    Switch setting_media_sync_switch;
    Switch setting_default_select_only_one_device_switch;
    SeekBar setting_activity_top_margin_seekbar;

    TextView tv_dev_name;
    TextView tv_recv_file_path;
    TextView tv_message_key;
    TextView tv_tcp_port;
    TextView tv_udp_port;
    TextView setting_activity_top_margin_num;
    TextView tv_theme; // 添加主题显示文本

    PrefUtil prefUtil;
    boolean portUpdate = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        prefUtil = App.getPrefUtil();
        initView();
        initData();
    }

    public void initView() {
        setting_exit_img = bind(R.id.setting_exit_img);
        setting_dev_name = bind(R.id.setting_dev_name);
        setting_save_path = bind(R.id.setting_save_path);
        setting_message_key = bind(R.id.setting_message_key);
        setting_save_message = bind(R.id.setting_save_message);
        setting_not_recv_dialog = bind(R.id.setting_not_recv_dialog);
        setting_open_media_internal_player = bind(R.id.setting_open_media_internal_player);
        setting_media_select_model = bind(R.id.setting_media_select_model);
        setting_media_select_model = bind(R.id.setting_media_select_model);
        setting_check_show_hidden_files = bind(R.id.setting_check_show_hidden_files);
        setting_tcp_port = bind(R.id.setting_tcp_port);
        setting_udp_port = bind(R.id.setting_udp_port);
        setting_auto_start = bind(R.id.setting_auto_start);
        setting_receive_broadcast_messages = bind(R.id.setting_receive_broadcast_messages);
        setting_broadcast_message = bind(R.id.setting_broadcast_message);
        setting_send_mute = bind(R.id.setting_send_mute);
        setting_receivce_mute = bind(R.id.setting_receivce_mute);
        setting_enc_data = bind(R.id.setting_enc_data);
        setting_web_service = bind(R.id.setting_web_service);
        setting_sync_notification = bind(R.id.setting_sync_notification);
        setting_web_service_open = bind(R.id.setting_web_service_open);
        setting_message_notification = bind(R.id.setting_message_notification);
        setting_save_files_path = bind(R.id.setting_save_files_path);
        setting_save_files_category_path = bind(R.id.setting_save_files_category_path);
        setting_media_sync = bind(R.id.setting_media_sync);
        setting_default_select_only_one_device = bind(R.id.setting_default_select_only_one_device);
        setting_theme = bind(R.id.setting_theme); // 绑定主题设置项

        setting_not_recv_dialog_switch = bind(R.id.setting_not_recv_dialog_switch);
        setting_open_media_switch = bind(R.id.setting_open_media_switch);
        setting_media_select_model_switch = bind(R.id.setting_media_select_model_switch);
        setting_save_message_switch = bind(R.id.setting_save_message_switch);
        setting_save_to_gallery_switch = bind(R.id.setting_save_to_gallery_switch);
        setting_show_hidden_files_switch = bind(R.id.setting_show_hidden_files_switch);
        setting_auto_start_switch = bind(R.id.setting_auto_start_switch);
        setting_receive_broadcast_messages_switch = bind(R.id.setting_receive_broadcast_messages_switch);
        setting_broadcast_message_switch = bind(R.id.setting_broadcast_message_switch);
        setting_send_mute_switch = bind(R.id.setting_send_mute_switch);
        setting_receivce_mute_switch = bind(R.id.setting_receivce_mute_switch);
        setting_enc_data_switch = bind(R.id.setting_enc_data_switch);
        setting_web_service_switch = bind(R.id.setting_web_service_switch);
        setting_sync_notification_switch = bind(R.id.setting_sync_notification_switch);
        setting_web_service_open_switch = bind(R.id.setting_web_service_open_switch);
        setting_message_notification_switch = bind(R.id.setting_message_notification_switch);
        setting_save_files_path_switch = bind(R.id.setting_save_files_path_switch);
        setting_save_files_category_path_switch = bind(R.id.setting_save_files_category_path_switch);
        setting_media_sync_switch = bind(R.id.setting_media_sync_switch);
        setting_default_select_only_one_device_switch = bind(R.id.setting_default_select_only_one_device_switch);

        setting_activity_top_margin_seekbar = bind(R.id.setting_activity_top_margin_seekbar);
        tv_dev_name = bind(R.id.tv_dev_name);
        tv_recv_file_path = bind(R.id.tv_recv_file_path);
        tv_message_key = bind(R.id.tv_message_key);
        tv_tcp_port = bind(R.id.tv_tcp_port);
        tv_udp_port = bind(R.id.tv_udp_port);
        tv_theme = bind(R.id.tv_theme); // 绑定主题显示文本

        setting_activity_top_margin_num = bind(R.id.setting_activity_top_margin_num);

        setting_exit_img.setOnClickListener(this);
        setting_dev_name.setOnClickListener(this);
        setting_save_path.setOnClickListener(this);
        setting_message_key.setOnClickListener(this);
        setting_save_message.setOnClickListener(this);
        setting_not_recv_dialog.setOnClickListener(this);
        setting_open_media_internal_player.setOnClickListener(this);
        setting_media_select_model.setOnClickListener(this);
        setting_check_show_hidden_files.setOnClickListener(this);
        setting_tcp_port.setOnClickListener(this);
        setting_udp_port.setOnClickListener(this);
        setting_auto_start.setOnClickListener(this);
        setting_receive_broadcast_messages.setOnClickListener(this);
        setting_broadcast_message.setOnClickListener(this);
        setting_send_mute.setOnClickListener(this);
        setting_receivce_mute.setOnClickListener(this);
        setting_enc_data.setOnClickListener(this);
        setting_web_service.setOnClickListener(this);
        setting_sync_notification.setOnClickListener(this);
        setting_web_service_open.setOnClickListener(this);
        setting_message_notification.setOnClickListener(this);
        setting_save_files_path.setOnClickListener(this);
        setting_save_files_category_path.setOnClickListener(this);
        setting_media_sync.setOnClickListener(this);
        setting_default_select_only_one_device.setOnClickListener(this);
        setting_theme.setOnClickListener(this); // 设置主题项点击监听

        setting_not_recv_dialog_switch.setOnCheckedChangeListener(this);
        setting_open_media_switch.setOnCheckedChangeListener(this);
        setting_media_select_model_switch.setOnCheckedChangeListener(this);
        setting_save_message_switch.setOnCheckedChangeListener(this);
        setting_save_to_gallery_switch.setOnCheckedChangeListener(this);
        setting_show_hidden_files_switch.setOnCheckedChangeListener(this);
        setting_auto_start_switch.setOnCheckedChangeListener(this);
        setting_receive_broadcast_messages_switch.setOnCheckedChangeListener(this);
        setting_broadcast_message_switch.setOnCheckedChangeListener(this);
        setting_send_mute_switch.setOnCheckedChangeListener(this);
        setting_receivce_mute_switch.setOnCheckedChangeListener(this);
        setting_enc_data_switch.setOnCheckedChangeListener(this);
        setting_web_service_switch.setOnCheckedChangeListener(this);
        setting_sync_notification_switch.setOnCheckedChangeListener(this);
        setting_web_service_open_switch.setOnCheckedChangeListener(this);
        setting_message_notification_switch.setOnCheckedChangeListener(this);
        setting_save_files_path_switch.setOnCheckedChangeListener(this);
        setting_save_files_category_path_switch.setOnCheckedChangeListener(this);
        setting_media_sync_switch.setOnCheckedChangeListener(this);
        setting_default_select_only_one_device_switch.setOnCheckedChangeListener(this);

        setting_activity_top_margin_seekbar.setOnSeekBarChangeListener(this);

    }

    @SuppressLint("SetTextI18n")
    public void initData() {
        tv_dev_name.setText(prefUtil.getString(PreConfig.USER_NAME));
        tv_recv_file_path.setText(prefUtil.getString(PreConfig.FILE_PATH, Config.FILE_SAVE_PATH));
        String s = prefUtil.getString(PreConfig.MESSAGE_KEY, Config.DEFAULT_MESSAGE_KEY);
        String decrypt = null;
        try {
            decrypt = AESUtils.decrypt(s, Config.KEY);
            tv_message_key.setText(decrypt);
        } catch (Exception e) {
            LLog.error("error:", e);
        }
        // 设置当前主题显示文本
        int themeMode = prefUtil.getInt(PreConfig.THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_YES:
                tv_theme.setText(R.string.dark_theme);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                tv_theme.setText(R.string.light_theme);
                break;
            default:
                tv_theme.setText(R.string.follow_system);
                break;
        }
        setting_not_recv_dialog_switch.setChecked(prefUtil.getBoolean(PreConfig.NOT_RECV_DIALOG, true));
        setting_open_media_switch.setChecked(prefUtil.getBoolean(PreConfig.POEN_MEDIA_PLAYER, true));
        setting_media_select_model_switch.setChecked(prefUtil.getBoolean(PreConfig.MEDIA_SELECT_MODEL, false));
        setting_save_message_switch.setChecked(prefUtil.getBoolean(PreConfig.SAVE_MESSAGE, true));
        setting_save_to_gallery_switch.setChecked(prefUtil.getBoolean(PreConfig.SAVE_TO_GALLERY, true));
        setting_show_hidden_files_switch.setChecked(prefUtil.getBoolean(PreConfig.SHOW_HIDDEN_FILES, false));
        setting_auto_start_switch.setChecked(prefUtil.getBoolean(PreConfig.AUTO_START, false));
        setting_receive_broadcast_messages_switch.setChecked(prefUtil.getBoolean(PreConfig.RECEIVE_BROADCAST_MESSAGES, false));
        setting_broadcast_message_switch.setChecked(prefUtil.getBoolean(PreConfig.BROADCAST_MESSASGE, false));
        setting_send_mute_switch.setChecked(prefUtil.getBoolean(PreConfig.SEND_MUTE, false));
        setting_receivce_mute_switch.setChecked(prefUtil.getBoolean(PreConfig.RECEIVE_MUTE, false));
        setting_enc_data_switch.setChecked(prefUtil.getBoolean(PreConfig.ENC_DATA, false));
        setting_web_service_switch.setChecked(prefUtil.getBoolean(PreConfig.WEB_SERCICE, true));
        setting_web_service_open_switch.setChecked(prefUtil.getBoolean(PreConfig.WEB_OPEN, false));
        setting_web_service_open_switch.setChecked(prefUtil.getBoolean(PreConfig.WEB_OPEN, false));
        setting_sync_notification_switch.setChecked(PermissionsUtils.hasNotificationPermission(this) && prefUtil.getBoolean(PreConfig.SYNC_NOTIFICATION, false));
        setting_message_notification_switch.setChecked(prefUtil.getBoolean(PreConfig.MESSAGE_NOTIFICAION, false));
        setting_save_files_path_switch.setChecked(prefUtil.getBoolean(PreConfig.S_LAST_FILE_PATH, false));
        setting_save_files_category_path_switch.setChecked(prefUtil.getBoolean(PreConfig.SAVE_FILES_CATEGORY, false));
        setting_media_sync_switch.setChecked(prefUtil.getBoolean(PreConfig.MEDIA_SYNC, false));
        setting_default_select_only_one_device_switch.setChecked(prefUtil.getBoolean(PreConfig.DEFAULT_SELECT_ONLY_ONE_DEVICE, true));
        tv_tcp_port.setText(String.valueOf(prefUtil.getInt(PreConfig.TCP_PORT, Config.DEFAULT_FILE_SERVER_PORT)));
        tv_udp_port.setText(String.valueOf(prefUtil.getInt(PreConfig.UDP_PORT, Config.DEFAULT_UDP_PORT)));
        setting_activity_top_margin_seekbar.setProgress(prefUtil.getInt(PreConfig.ACTIVITY_TOP_MARGIN, 0));
        setting_activity_top_margin_num.setText(String.valueOf(setting_activity_top_margin_seekbar.getProgress()));
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting_exit_img: {
                finish();
            }
            case R.id.setting_theme: {
                showThemeSelectDialog();
                break;
            }
            case R.id.setting_dev_name: {
                EditTextDialog editTextDialog = new EditTextDialog(this, false, getString(R.string.set_dev_name), prefUtil.getString(PreConfig.USER_NAME));
                editTextDialog.setOnClickListener((ok, str) -> {
                    String replace = str.replaceAll("\n", "");
                    prefUtil.saveString(PreConfig.USER_NAME, replace);
                    Config.USER_NAME = replace;
                    tv_dev_name.setText(replace);
                });
                editTextDialog.setMaxLen(15).show();
                break;
            }
            case R.id.setting_save_path: {
                EditTextDialog editTextDialog = new EditTextDialog(this, false, getString(R.string.set_recv_file_path), prefUtil.getString(PreConfig.FILE_PATH, Config.FILE_SAVE_PATH));
                editTextDialog.setOnClickListener((ok, str) -> {
                    prefUtil.saveString(PreConfig.FILE_PATH, str);
                    tv_recv_file_path.setText(str);
                });
                editTextDialog.setMaxLen(255).show();
                break;
            }
            case R.id.setting_message_key: {
                String s = prefUtil.getString(PreConfig.MESSAGE_KEY, Config.DEFAULT_MESSAGE_KEY);
                String decrypt = null;
                try {
                    decrypt = AESUtils.decrypt(s, Config.KEY);
                    tv_message_key.setText(decrypt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                EditTextDialog editTextDialog = new EditTextDialog(this, false, getString(R.string.message_key), decrypt);
                editTextDialog.setOnClickListener((ok, str) -> {
                    try {
                        String encrypt = AESUtils.encrypt(str, Config.KEY);
                        prefUtil.saveString(PreConfig.MESSAGE_KEY, encrypt);
                        tv_message_key.setText(str);
                        Config.MESSAGE_KEY = str;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                editTextDialog.setMaxLen(255).show();
                break;
            }
            case R.id.setting_not_recv_dialog: {
                setting_not_recv_dialog_switch.setChecked(!setting_not_recv_dialog_switch.isChecked());
                break;
            }
            case R.id.setting_open_media_internal_player: {
                setting_open_media_switch.setChecked(!setting_open_media_switch.isChecked());
                break;
            }
            case R.id.setting_media_select_model: {
                setting_media_select_model_switch.setChecked(!setting_media_select_model_switch.isChecked());
                break;
            }
            case R.id.setting_save_message: {
                setting_save_message_switch.setChecked(!setting_save_message_switch.isChecked());
                break;
            }
            case R.id.setting_save_to_gallery_switch: {
                setting_save_to_gallery_switch.setChecked(!setting_save_to_gallery_switch.isChecked());
                break;
            }
            case R.id.setting_check_show_hidden_files: {
                setting_show_hidden_files_switch.setChecked(!setting_show_hidden_files_switch.isChecked());
                break;
            }
            case R.id.setting_tcp_port: {
                int tcpPort = prefUtil.getInt(PreConfig.TCP_PORT, Config.DEFAULT_FILE_SERVER_PORT);
                EditTextDialog editTextDialog = new EditTextDialog(
                        this,
                        false, getString(R.string.tcp_port),
                        tcpPort + "",
                        getString(R.string.number_between_1000_and_65535)
                );
                editTextDialog.setAccepted(EditTextDialog.ACCEPTED_NUM);
                editTextDialog.setOnClickCheck(str -> {
                    int port = Integer.parseInt(str);
                    if (port < 1000 || port > 65535) {
                        T.s((R.string.number_between_1000_and_65535));
                        return false;
                    }
                    return true;
                });
                editTextDialog.setOnClickListener((ok, str) -> {
                    int port = Integer.parseInt(str);
                    prefUtil.saveInt(PreConfig.TCP_PORT, port);
                    tv_tcp_port.setText(str);
                    if (tcpPort != port) {
                        portUpdate = true;
                    }

                });
                editTextDialog.setMaxLen(5).show();
                break;
            }
            case R.id.setting_udp_port: {
                int udpPort = prefUtil.getInt(PreConfig.UDP_PORT, Config.DEFAULT_UDP_PORT);
                EditTextDialog editTextDialog = new EditTextDialog(
                        this,
                        false, getString(R.string.udp_port),
                        udpPort + "",
                        getString(R.string.number_between_1000_and_65535)
                );
                editTextDialog.setAccepted(EditTextDialog.ACCEPTED_NUM);
                editTextDialog.setOnClickCheck(str -> {
                    int port = Integer.parseInt(str);
                    if (port < 1000 || port > 65535) {
                        T.s((R.string.number_between_1000_and_65535));
                        return false;
                    }
                    return true;
                });
                editTextDialog.setOnClickListener((ok, str) -> {
                    int port = Integer.parseInt(str);
                    prefUtil.saveInt(PreConfig.UDP_PORT, port);
                    tv_udp_port.setText(str);
                    if (udpPort != port) {
                        portUpdate = true;
                    }
                });
                editTextDialog.setMaxLen(5).show();
                break;
            }
            case R.id.setting_auto_start: {
                setting_auto_start_switch.setChecked(!setting_auto_start_switch.isChecked());
                break;
            }
            case R.id.setting_receive_broadcast_messages: {
                setting_receive_broadcast_messages_switch.setChecked(!setting_receive_broadcast_messages_switch.isChecked());
                break;
            }
            case R.id.setting_broadcast_message: {
                setting_broadcast_message_switch.setChecked(!setting_broadcast_message_switch.isChecked());
                break;
            }
            case R.id.setting_send_mute: {
                setting_send_mute_switch.setChecked(!setting_send_mute_switch.isChecked());
                break;
            }
            case R.id.setting_receivce_mute: {
                setting_receivce_mute_switch.setChecked(!setting_receivce_mute_switch.isChecked());
                break;
            }
            case R.id.setting_enc_data: {
                setting_enc_data_switch.setChecked(!setting_enc_data_switch.isChecked());
                break;
            }
            case R.id.setting_web_service: {
                setting_web_service_switch.setChecked(!setting_web_service_switch.isChecked());
                break;
            }
            case R.id.setting_sync_notification: {
                setting_sync_notification_switch.setChecked(!setting_sync_notification_switch.isChecked());
                break;
            }

            case R.id.setting_web_service_open: {
                setting_web_service_open_switch.setChecked(!setting_web_service_open_switch.isChecked());
                break;
            }
            case R.id.setting_message_notification: {
                setting_message_notification_switch.setChecked(!setting_message_notification_switch.isChecked());
                break;
            }
            case R.id.setting_save_files_path: {
                setting_save_files_path_switch.setChecked(!setting_save_files_path_switch.isChecked());
                break;
            }
            case R.id.setting_save_files_category_path: {
                setting_save_files_category_path_switch.setChecked(!setting_save_files_category_path_switch.isChecked());
                break;
            }
            case R.id.setting_media_sync: {
                setting_media_sync_switch.setChecked(!setting_media_sync_switch.isChecked());
                break;
            }
            case R.id.setting_default_select_only_one_device: {
                setting_default_select_only_one_device_switch.setChecked(!setting_default_select_only_one_device_switch.isChecked());
                break;
            }

        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.setting_not_recv_dialog_switch: {
                prefUtil.saveBoolean(PreConfig.NOT_RECV_DIALOG, isChecked);
                break;
            }
            case R.id.setting_open_media_switch: {
                prefUtil.saveBoolean(PreConfig.POEN_MEDIA_PLAYER, isChecked);
                break;
            }
            case R.id.setting_media_select_model_switch: {
                prefUtil.saveBoolean(PreConfig.MEDIA_SELECT_MODEL, isChecked);
                break;
            }
            case R.id.setting_save_message_switch: {
                prefUtil.saveBoolean(PreConfig.SAVE_MESSAGE, isChecked);
                break;
            }
            case R.id.setting_save_to_gallery_switch: {
                prefUtil.saveBoolean(PreConfig.SAVE_TO_GALLERY, isChecked);
                break;
            }
            case R.id.setting_show_hidden_files_switch: {
                prefUtil.saveBoolean(PreConfig.SHOW_HIDDEN_FILES, isChecked);
                break;
            }
            case R.id.setting_auto_start_switch: {
                prefUtil.saveBoolean(PreConfig.AUTO_START, isChecked);
                // 安卓10以上需要申请悬浮窗权限才能开机自启
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!Settings.canDrawOverlays(this)) {
                            T.s((R.string.need_overlay_permission));
                            //若未授权则请求权限
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 0);
                        }
                    }
                }
                break;
            }
            case R.id.setting_receive_broadcast_messages_switch: {
                prefUtil.saveBoolean(PreConfig.RECEIVE_BROADCAST_MESSAGES, isChecked);
                break;
            }
            case R.id.setting_broadcast_message_switch: {
                prefUtil.saveBoolean(PreConfig.BROADCAST_MESSASGE, isChecked);
                break;
            }
            case R.id.setting_send_mute_switch: {
                prefUtil.saveBoolean(PreConfig.SEND_MUTE, isChecked);
                break;
            }
            case R.id.setting_receivce_mute_switch: {
                if (setting_receivce_mute_switch.isChecked()) {
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.isNotificationPolicyAccessGranted()) {
                        T.s((R.string.need_do_not_disturb_permission));
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    }
                }
                prefUtil.saveBoolean(PreConfig.RECEIVE_MUTE, isChecked);
                break;
            }
            case R.id.setting_enc_data_switch: {
                prefUtil.saveBoolean(PreConfig.ENC_DATA, isChecked);
                break;
            }
            case R.id.setting_web_service_switch: {
                prefUtil.saveBoolean(PreConfig.WEB_SERCICE, isChecked);
                Config.WEB_SERVICE = isChecked;
                break;
            }
            case R.id.setting_sync_notification_switch: {
                if (setting_sync_notification_switch.isChecked()) {
                    if (!PermissionsUtils.hasNotificationPermission(this)) {
                        T.s((R.string.need_notification_permission));
                        PermissionsUtils.requestNotificationPermission(this);
                    }
                }
                prefUtil.saveBoolean(PreConfig.SYNC_NOTIFICATION, isChecked);
                Config.SYNC_NOTIFICATION = isChecked;
                break;
            }
            case R.id.setting_web_service_open_switch: {
                prefUtil.saveBoolean(PreConfig.WEB_OPEN, isChecked);
                Config.WEB_OPEN = isChecked;
                break;
            }
            case R.id.setting_message_notification_switch: {
                prefUtil.saveBoolean(PreConfig.MESSAGE_NOTIFICAION, isChecked);
                Config.MESSAGE_NOTIFICAION = isChecked;
                break;
            }

            case R.id.setting_save_files_path_switch: {
                prefUtil.saveBoolean(PreConfig.S_LAST_FILE_PATH, isChecked);
                Config.LAST_FILE_PATH = isChecked;
                break;
            }
            case R.id.setting_save_files_category_path_switch: {
                prefUtil.saveBoolean(PreConfig.SAVE_FILES_CATEGORY, isChecked);
                Config.SAVE_FILES_CATEGORY = !isChecked;
                break;
            }
            case R.id.setting_default_select_only_one_device_switch: {
                prefUtil.saveBoolean(PreConfig.DEFAULT_SELECT_ONLY_ONE_DEVICE, isChecked);
                Config.DEFAULT_SELECT_ONLY_ONE_DEVICE = !isChecked;
                break;
            }
            case R.id.setting_media_sync_switch: {
                prefUtil.saveBoolean(PreConfig.MEDIA_SYNC, isChecked);
                Config.MEDIA_SYNC = !isChecked;
                if (isChecked) {
                    LANService.getInstance().getImageObserver().registerObserver();
                } else {
                    LANService.getInstance().getImageObserver().unregisterObserver();
                }
                break;
            }

        }
    }

    @Override
//调用onBackPressed()方法，点击返回键返回数据给上一个Activity
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("portUpdate", portUpdate);
        setResult(1, intent);
        finish();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.setting_activity_top_margin_seekbar) {
            setting_activity_top_margin_num.setText(String.valueOf(progress));
            setTopMargin(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.setting_activity_top_margin_seekbar) {
            prefUtil.saveInt(PreConfig.ACTIVITY_TOP_MARGIN, seekBar.getProgress());
        }
    }

    /**
     * 显示主题选择对话框
     */
    private void showThemeSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.theme_setting);
        String[] themes = {getString(R.string.light_theme), getString(R.string.dark_theme), getString(R.string.follow_system)};
        int selectedTheme = prefUtil.getInt(PreConfig.THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int selectedIndex;
        switch (selectedTheme) {
            case AppCompatDelegate.MODE_NIGHT_YES:
                selectedIndex = 1;
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                selectedIndex = 0;
                break;
            default:
                selectedIndex = 2;
                break;
        }
        builder.setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
            int themeMode;
            switch (which) {
                case 0: // 浅色主题
                    themeMode = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case 1: // 深色主题
                    themeMode = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                default: // 跟随系统
                    themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    break;
            }
            prefUtil.saveInt(PreConfig.THEME_MODE, themeMode);
            AppCompatDelegate.setDefaultNightMode(themeMode);
            tv_theme.setText(themes[which]);
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
