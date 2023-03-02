package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.EditTextDialog;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.PrefUtil;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static final int SETTING_REQUEST_CODE = 0x0000f01e; // Only use bottom 16 bits


    LinearLayout setting_dev_name;
    LinearLayout setting_save_path;
    LinearLayout setting_not_recv_dialog;
    LinearLayout setting_open_media_internal_player;
    LinearLayout setting_media_select_model;
    LinearLayout setting_check_show_hidden_files;
    LinearLayout setting_tcp_port;
    LinearLayout setting_udp_port;

    Switch setting_not_recv_dialog_switch;
    Switch setting_open_media_switch;
    Switch setting_media_select_model_switch;
    Switch setting_save_message_switch;
    Switch setting_save_to_gallery_switch;
    Switch setting_show_hidden_files_switch;

    TextView tv_dev_name;
    TextView tv_recv_file_path;
    TextView tv_tcp_port;
    TextView tv_udp_port;

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
        setting_dev_name = findViewById(R.id.setting_dev_name);
        setting_save_path = findViewById(R.id.setting_save_path);
        setting_not_recv_dialog = findViewById(R.id.setting_not_recv_dialog);
        setting_open_media_internal_player = findViewById(R.id.setting_open_media_internal_player);
        setting_media_select_model = findViewById(R.id.setting_media_select_model);
        setting_media_select_model = findViewById(R.id.setting_media_select_model);
        setting_check_show_hidden_files = findViewById(R.id.setting_check_show_hidden_files);
        setting_tcp_port = findViewById(R.id.setting_tcp_port);
        setting_udp_port = findViewById(R.id.setting_udp_port);

        setting_not_recv_dialog_switch = findViewById(R.id.setting_not_recv_dialog_switch);
        setting_open_media_switch = findViewById(R.id.setting_open_media_switch);
        setting_media_select_model_switch = findViewById(R.id.setting_media_select_model_switch);
        setting_save_message_switch = findViewById(R.id.setting_save_message_switch);
        setting_save_to_gallery_switch = findViewById(R.id.setting_save_to_gallery_switch);
        setting_show_hidden_files_switch = findViewById(R.id.setting_show_hidden_files_switch);

        tv_dev_name = findViewById(R.id.tv_dev_name);
        tv_recv_file_path = findViewById(R.id.tv_recv_file_path);
        tv_tcp_port = findViewById(R.id.tv_tcp_port);
        tv_udp_port = findViewById(R.id.tv_udp_port);

        setting_dev_name.setOnClickListener(this);
        setting_save_path.setOnClickListener(this);
        setting_not_recv_dialog.setOnClickListener(this);
        setting_open_media_internal_player.setOnClickListener(this);
        setting_media_select_model.setOnClickListener(this);
        setting_check_show_hidden_files.setOnClickListener(this);
        setting_tcp_port.setOnClickListener(this);
        setting_udp_port.setOnClickListener(this);

        setting_not_recv_dialog_switch.setOnCheckedChangeListener(this);
        setting_open_media_switch.setOnCheckedChangeListener(this);
        setting_media_select_model_switch.setOnCheckedChangeListener(this);
        setting_save_message_switch.setOnCheckedChangeListener(this);
        setting_save_to_gallery_switch.setOnCheckedChangeListener(this);
        setting_show_hidden_files_switch.setOnCheckedChangeListener(this);

    }

    @SuppressLint("SetTextI18n")
    public void initData() {
        tv_dev_name.setText(prefUtil.getString(PreConfig.USER_NAME));
        tv_recv_file_path.setText(prefUtil.getString(PreConfig.FILE_PATH, Config.FILE_SAVE_PATH));
        setting_not_recv_dialog_switch.setChecked(prefUtil.getBoolean(PreConfig.NOT_RECV_DIALOG, false));
        setting_open_media_switch.setChecked(prefUtil.getBoolean(PreConfig.POEN_MEDIA_PLAYER, true));
        setting_media_select_model_switch.setChecked(prefUtil.getBoolean(PreConfig.MEDIA_SELECT_MODEL, false));
        setting_save_message_switch.setChecked(prefUtil.getBoolean(PreConfig.SAVE_MESSAGE, true));
        setting_save_to_gallery_switch.setChecked(prefUtil.getBoolean(PreConfig.SAVE_TO_GALLERY, true));
        setting_show_hidden_files_switch.setChecked(prefUtil.getBoolean(PreConfig.SHOW_HIDDEN_FILES, false));
        tv_tcp_port.setText(prefUtil.getInt(PreConfig.TCP_PORT, Config.DEFAULT_FILE_SERVER_PORT) + "");
        tv_udp_port.setText(prefUtil.getInt(PreConfig.UDP_PORT, Config.DEFAULT_UDP_PORT) + "");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting_dev_name: {
                EditTextDialog editTextDialog = new EditTextDialog(this, false, getString(R.string.set_dev_name), prefUtil.getString(PreConfig.USER_NAME));
                editTextDialog.setOnClickListener((ok, str) -> {
                    String replace = str.replaceAll("\n", "");
                    prefUtil.saveString(PreConfig.USER_NAME, replace);
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
            case R.id.setting_save_message_switch: {
                setting_media_select_model_switch.setChecked(!setting_save_message_switch.isChecked());
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
                        "请输入1000-65535之间的数字"
                );
                editTextDialog.setAccepted(EditTextDialog.ACCEPTED_NUM);
                editTextDialog.setOnClickCheck(str -> {
                    int port = Integer.parseInt(str);
                    if (port < 1000 || port > 65535) {
                        T.s("请输入1000-65535之间的数字");
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
                        "请输入1000-65535之间的数字"
                );
                editTextDialog.setAccepted(EditTextDialog.ACCEPTED_NUM);
                editTextDialog.setOnClickCheck(str -> {
                    int port = Integer.parseInt(str);
                    if (port < 1000 || port > 65535) {
                        T.s("请输入1000-65535之间的数字");
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
}
