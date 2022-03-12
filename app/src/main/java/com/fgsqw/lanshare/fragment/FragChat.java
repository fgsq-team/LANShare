package com.fgsqw.lanshare.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.dialog.DeviceSelectDialog;
import com.fgsqw.lanshare.dialog.FileSendDialog;
import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;
import com.fgsqw.lanshare.fragment.adapter.viewolder.FileMsgHolder;
import com.fgsqw.lanshare.pojo.DataObject;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.MessageContent;
import com.fgsqw.lanshare.pojo.MessageFileContent;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.pojo.mSocket;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FragChat extends BaseFragment implements View.OnClickListener, ChatAdabper.OnItemClickListener, ChatAdabper.OnItemLongClickListener {


    public DataCenterActivity dataCenterActivity;
    InputMethodManager mInputManager;

    private View view;
    private Button btnSned;
    private EditText editContent;
    private LinearLayout messageLayout;
    private TextView devSelectLTv;
    private RecyclerView recyclerView;
    private ChatAdabper chatAdabper;
    private LinearLayoutManager layoutManager;
    private List<MessageContent> messageContentList = new ArrayList<>();
    private PrefUtil prefUtil;
    private Device selectedDevice;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    // 接收LANService的消息
    @SuppressWarnings("all")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == mCmd.SERVICE_IF_RECIVE_FILES) {   // 是否接收文件弹窗
                DataObject dataObject = (DataObject) msg.obj;
                Device device = (Device) dataObject.getObj1();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                        .setIcon(R.mipmap.ic_launcher)
                        .setCancelable(false)
                        .setTitle("接收文件")
                        .setMessage("是否接收来自" + device.getDevName() + "的" + msg.arg1 + "个文件")
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            LANService.service.startRecvFile(dataObject, true);
                        }).setNegativeButton("取消", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            LANService.service.startRecvFile(dataObject, false);
                        });
                builder.create().show();
            } else if (msg.what == mCmd.SERVICE_SHOW_PROGRESS) {     // 创建一条数据
                List<MessageFileContent> messageContents = (List<MessageFileContent>) msg.obj;
                messageContentList.addAll(messageContents);
                chatAdabper.refresh();
                recyclerView.scrollToPosition(chatAdabper.getItemCount() - 1);
            } else if (msg.what == mCmd.SERVICE_PROGRESS) {          // 更新进度
                MessageFileContent fileContent = (MessageFileContent) msg.obj;
                // 获取数据在列表中的下标
                int dataPosition = chatAdabper.getDataPosition(fileContent);
                // 获取视图并更新视图数据
                FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
                if (viewHolder != null) {
                    viewHolder.progressBar.setProgress(fileContent.getProgress());
                } else {
                    chatAdabper.notifyItemChanged(dataPosition);
                }
            } else if (msg.what == mCmd.SERVICE_CLOSE_PROGRESS) {    // 完成
                MessageFileContent fileContent = (MessageFileContent) msg.obj;
                // 获取数据在列表中的下标
                int dataPosition = chatAdabper.getDataPosition(fileContent);
                // 获取视图并更新视图数据
                FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
                if (viewHolder != null) {
                    viewHolder.progressBar.setProgress(fileContent.getProgress());
                    if (fileContent.getSuccess() != null) {
                        viewHolder.progressBar.setVisibility(View.GONE);
                        viewHolder.stateTv.setVisibility(View.VISIBLE);
                        viewHolder.stateTv.setText(fileContent.getStateMessage());
                        if (!fileContent.getSuccess()) {
                            viewHolder.stateTv.setTextColor(Color.RED);
                        } else {
                            viewHolder.stateTv.setTextColor(getContext().getColor(R.color.item_text));
                        }
                    } else {
                        viewHolder.progressBar.setVisibility(View.VISIBLE);
                        viewHolder.stateTv.setVisibility(View.GONE);
                        viewHolder.stateTv.setTextColor(getContext().getColor(R.color.item_text));
                    }
                } else {
                    chatAdabper.notifyItemChanged(dataPosition);
                }
            } else if (msg.what == mCmd.SERVICE_ADD_MESSGAGE) {    //新增消息
                MessageContent messageContent = (MessageContent) msg.obj;
                addMessage(messageContent);
            }
        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        prefUtil = new PrefUtil(getContext());
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_chat, container, false);
            initView();
            initList();
        }
        mInputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void initView() {
        recyclerView = view.findViewById(R.id.chat_recy);
        btnSned = view.findViewById(R.id.chat_btn_send);
        editContent = view.findViewById(R.id.chat_et_content);
        messageLayout = view.findViewById(R.id.chat_send_message_layout);
        devSelectLTv = view.findViewById(R.id.chat_dev_select_tv);

        btnSned.setOnClickListener(this);
        devSelectLTv.setOnClickListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initList() {
        layoutManager = new LinearLayoutManager(getContext());
        chatAdabper = new ChatAdabper(this);
        chatAdabper.setOnItemClickListener(this);
        chatAdabper.setOnItemLongClickListener(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdabper);

        recyclerView.setOnTouchListener((view, motionEvent) -> {
            hideSoftInput();
            editContent.clearFocus();
            return false;
        });


        editContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 得到焦点
                dataCenterActivity.hideBottom();
            } else {
                // 失去焦点
                dataCenterActivity.showBottom();
            }
        });
    }


    @Override
    public void onItemClick(MessageContent messageContent, int position) {
        if (messageContent instanceof MessageFileContent) {
            MessageFileContent fileContent = (MessageFileContent) messageContent;
            if (fileContent.getSuccess() != null && fileContent.getSuccess()) {
                FileUtil.openFile(dataCenterActivity, new File(fileContent.getPath()));
//                FileUtil.openFile1(dataCenterActivity, fileContent.getPath());
            }
        }
    }

    @Override
    public boolean onItemLongClick(MessageContent messageContent, View view, int position) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.recy_menu, popupMenu.getMenu());
        popupMenu.setGravity(messageContent.isLeft() ? Gravity.START : Gravity.END);
        // 弹出式菜单的菜单项点击事件
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_delete) {
                chatAdabper.notifyItemRemoved(position);
                if (messageContent instanceof MessageFileContent) {
                    MessageFileContent fileContent = (MessageFileContent) messageContent;
                    mSocket socket = fileContent.getSocket();
                    if (fileContent.isLeft()) {
                        LANService.service.sendCloseCmd(fileContent, socket.getOut());
                    } else {
                        socket.mClose();
                    }
                }
                // 移除数据
                messageContentList.remove(messageContent);
                // 更新列表
                chatAdabper.refresh();
            } else if (item.getItemId() == R.id.menu_cppy) {
                mUtil.copy(messageContent.getContent(), getContext());
            }
            return false;
        });
        popupMenu.show();
        return true;
    }

    public List<MessageContent> getMessageContents() {
        return messageContentList;
    }

    public Handler getHandler() {
        return handler;
    }

    public void addMessage(MessageContent messageContent) {
        messageContentList.add(messageContent);
        chatAdabper.refresh();
        recyclerView.scrollToPosition(chatAdabper.getItemCount() - 1);
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (editContent.isFocused()) {
            editContent.clearFocus();
            return true;
        }
        return false;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_btn_send: {
                String message = editContent.getText().toString();
                if (message.isEmpty()) {
                    T.s("输入不能为空");
                    return;
                } else if (message.length() > 700) {
                    T.s("字符不长度能超出700个");
                    return;
                }
                MessageContent messageContent = new MessageContent();
                messageContent.setLeft(false);
                messageContent.setContent(message);
                messageContent.setUserName(LANService.service.getDevName());
                messageContent.setToUser("所有设备");
                LANService.service.broadcastMessage(selectedDevice, message);
                addMessage(messageContent);
                editContent.setText("");
                break;
            }
            case R.id.chat_dev_select_tv: {
                selectDevice();
                break;
            }
        }
    }

    public void selectDevice() {
        DeviceSelectDialog deviceSelectDialog = new DeviceSelectDialog(getContext());
        deviceSelectDialog.setOnDeviceSelect(device -> {
            if (device.getDevIP() == null) {
                editContent.setHint(getString(R.string.send_message_hint));
                devSelectLTv.setText(getString(R.string.all_device));
                selectedDevice = null;
            } else {
                selectedDevice = device;
                editContent.setHint("向" + device.getDevName() + "发送消息");
                devSelectLTv.setText(device.getDevName());
            }
        });
        deviceSelectDialog.show();
    }


    /**
     * 隐藏软件盘
     */
    public void hideSoftInput() {
        mInputManager.hideSoftInputFromWindow(editContent.getWindowToken(), 0);
    }

    /**
     * 显示软键盘
     */
    public void showSoftInput() {
        editContent.requestFocus();
        editContent.post(() -> mInputManager.showSoftInput(editContent, 0));
    }


}
