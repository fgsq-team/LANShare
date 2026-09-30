package com.fgsqw.lanshare.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.activity.preview.ReviewImages;
import com.fgsqw.lanshare.activity.video.VideoPlayer;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.TokenDBUtil;
import com.fgsqw.lanshare.dialog.DeviceSelectDialog;
import com.fgsqw.lanshare.fragment.adapter.ChatAdapter;
import com.fgsqw.lanshare.fragment.adapter.viewolder.FileMsgHolder;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.service.RecvFileCallback;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.db.MesssageDButil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author fgsq
 * @comments 消息界面
 * @date 2024/7/13 10:53
 */
public class FragmentChat extends BaseFragment implements View.OnClickListener,
        View.OnLongClickListener,
        ChatAdapter.OnItemClickListener,
        ChatAdapter.OnItemLongClickListener,
        ChatAdapter.OnCheckedChangeListener {

    public final static String TAG = "FragChat";
    public DataCenterActivity dataCenterActivity;
    InputMethodManager mInputManager;

    private View view;
    // 发下消息按钮
    private Button btnSned;
    // 消息编辑框
    private EditText editContent;
    private LinearLayout chatEditLayout;
    // 选中设备名称
    private TextView devSelectLTv;
    // 消息列表视图
    private RecyclerView recyclerView;
    // 消息列表适配器
    private ChatAdapter chatAdapter;
    // 消息列表布局管理器
    private LinearLayoutManager layoutManager;
    // 消息列表
    private final List<MessageContent> messageContentList = new ArrayList<>();
    private final List<MessageContent> checkedMessageList = new ArrayList<>();
    // 选中的设备
    private Device selectedDevice;
    // 配置文件加载工具
    private PrefUtil prefUtil;
    // 消息数据库工具
    private MesssageDButil messsageDButil;
    private TokenDBUtil tokenDBUtil;

    private boolean isFragmentVisible = false;
    int pageSize = 20;
    int pageCount = 0;

    @Override
    public void onResume() {
        super.onResume();
        isFragmentVisible = true;
        // 在这里执行在Fragment变为可见时的操作
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentVisible = false;
        // 在这里执行在Fragment变为不可见时的操作
    }

    // 在需要判断的地方，可以通过 isFragmentVisible 来判断当前 Fragment 是否在前台
    public boolean isFragmentVisible() {
        return isFragmentVisible;
    }

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        messsageDButil = new MesssageDButil(context);
        Config.lastMessageTime = messsageDButil.getLastMessageTime();
        tokenDBUtil = new TokenDBUtil(context);
        super.onAttach(context);
    }

    // 接收LANService的消息
    @Override
    public void handleMessage(Message msg) {
        if (msg.what == LCmd.SERVICE_IF_RECIVE_FILES) {
            /* 是否接收文件弹窗 */
            showIsReceiveDialog(msg);
        } else if (msg.what == LCmd.SERVICE_GET_APPS) {
            /* 新增一些数据 */
            showGetAppDialog(msg);
        } else if (msg.what == LCmd.SERVICE_SHOW_PROGRESS) {
            /* 新增一些数据 */
            addListData(msg);
        } else if (msg.what == LCmd.SERVICE_PROGRESS) {
            /* 更新文件进度 */
            updateLocalItemProgress(msg);
        } else if (msg.what == LCmd.SERVICE_COMPLETE_COUNT) {
            /* 更新文件夹传输文完成数量 */
            updateLocalItemFolderCount(msg);
        } else if (msg.what == LCmd.SERVICE_CLOSE_PROGRESS) {
            /* 完成传输 */
            updateLocalItemInfo(msg);
        } else if (msg.what == LCmd.SERVICE_ADD_MESSGAGE) {
            /* 新增消息 */
            MessageContent messageContent = (MessageContent) msg.obj;
            addMessage(messageContent, false);
        } else if (msg.what == LCmd.SERVICE_HTTP_NEW_CLIENT) {
            /* http新客户端 */
            newHttpClient(msg.obj);
        }
    }

    public void newHttpClient(Object token) {
        String[] arr = (String[]) token;
        AlertDialog.Builder normalDialog = new AlertDialog.Builder(dataCenterActivity);
        normalDialog.setTitle(R.string.new_web_client);
        normalDialog.setMessage(R.string.new_web_client_detail);
        normalDialog.setPositiveButton(getString(R.string.agree), (dialog, which) -> {
            tokenDBUtil.setPass(arr[0], 1);
            //...To-do
        });
        normalDialog.setNegativeButton(getString(R.string.disagree), (dialog, which) -> {
            //...To-do
        });
        // 显示
        normalDialog.show();
    }

    //    @RequiresApi(api = Build.VERSION_CODES.M)
    public void updateLocalItemInfo(Message message) {
        MessageContent messageContent = (MessageContent) message.obj;
        updateMessage(messageContent, true);
        /*if (messageContent instanceof MessageMediaContent) {
            MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
            MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.setPath(mediaContent.getPath());
            mediaInfo.setLength(mediaContent.getLength());
            mediaInfo.setName(mediaContent.getContent());
            VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
        }*/
    }

    public void checkAndAddChatTime(String bindId, boolean save) {
        if (DateUtils.isFiveMinutesAgo(Config.lastMessageTime)) {
            MessageTimeContent messageTimeContent = new MessageTimeContent();
            messageTimeContent.setId(StringUtils.getUUID());
            messageTimeContent.setBindId(bindId);
            messageContentList.add(messageTimeContent);
            if (Config.SAVE_MESSAGE && save) {
                messsageDButil.addMessage(messageTimeContent);
            }
            Config.lastMessageTime = messageTimeContent.getCreateTime().getTime();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addListData(Message message) {
        List<MessageContent> messageContents = (List<MessageContent>) message.obj;
        checkAndAddChatTime(messageContents.get(0).getId(), false);
        addListMessage(messageContents);
    }

    public void updateLocalItemFolderCount(Message message) {
        MessageFolderContent folderContent = (MessageFolderContent) message.obj;
        int dataPosition = chatAdapter.getDataPosition(folderContent);
        FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
        if (viewHolder != null) {
            viewHolder.content.setText(folderContent.getContent());
        } else {
            chatAdapter.notifyItemChanged(dataPosition);
        }
    }

    public void updateLocalItemProgress(Message message) {
        MessageFileContent fileContent = (MessageFileContent) message.obj;
        // 获取数据在列表中的下标
        int dataPosition = chatAdapter.getDataPosition(fileContent);
        // 获取视图并更新视图数据
        FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
        if (viewHolder != null) {
            if (viewHolder.progressBar.getVisibility() == View.GONE) {
                viewHolder.progressBar.setVisibility(View.VISIBLE);
                viewHolder.stateTv.setVisibility(View.GONE);
                viewHolder.stateTv.setTextColor(getContext().getResources().getColor(R.color.itemTextColor));
                fileContent.setStatus(MessageContent.IN);
            }
            viewHolder.progressBar.setProgress(fileContent.getProgress());
        } else {
            chatAdapter.notifyItemChanged(dataPosition);
        }
    }

    /**
     * @author fgsq
     * @comments 显示确认接收消息弹窗
     * @date 2024/5/22 11:01
     */
    public void showIsReceiveDialog(Message msg) {
        RecvFileCallback recvFileCallback = (RecvFileCallback) msg.obj;
        Device device = recvFileCallback.getFileTransfer().getFromDevice();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                .setIcon(R.mipmap.ic_launcher_round)
                .setCancelable(false)
                .setTitle(getString(R.string.accept_files))
                .setMessage(String.format(getString(R.string.accept_file_from_x), device.getDevName(), msg.arg1))
                .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    recvFileCallback.receviceFile(true);
                }).setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    recvFileCallback.receviceFile(false);
                });
        builder.create().show();
    }

    /**
     * @author fgsq
     * @comments 显示获取APP更新弹窗
     * @date 2024/5/22 11:00
     */
    public void showGetAppDialog(Message msg) {
        Object[] dataObject = (Object[]) msg.obj;
        Device device = (Device) dataObject[0];
        List<MessageFileContent> fileInfos = (List<MessageFileContent>) dataObject[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                .setIcon(R.mipmap.ic_launcher_round)
                .setCancelable(false)
                .setTitle("APP更新请求")
                .setMessage(String.format("是否同意%s的获取的%d个APP更新请求", device.getDevName(), fileInfos.size()))
                .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    LANService.getInstance().fileSend(LANService.getInstance().getDevice(device), device, fileInfos);
                }).setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        builder.create().show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        if (view == null) {
            prefUtil = App.getPrefUtil();
            view = inflater.inflate(R.layout.fragment_chat, container, false);
            initView();
            initList();
            loadData();
        }
        mInputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public class TopScrollListener extends RecyclerView.OnScrollListener {
        private boolean isTop = false;

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            boolean atTop = isAtTop(recyclerView);
            // 只有当状态从非顶部变为顶部时才触发事件
            if (!isTop && atTop) {
                isTop = true;
                Log.d(TAG, "已滚动到顶部" + (pageCount));
                loadData();
            } else if (isTop && !atTop) {
                isTop = false;
            }
        }

        private boolean isAtTop(RecyclerView recyclerView) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                if (firstVisiblePosition == 0) {
                    View firstVisibleView = layoutManager.getChildAt(0);
                    return firstVisibleView != null && firstVisibleView.getTop() == 0;
                }
            }
            return false;
        }
    }


    public void initView() {
        recyclerView = view.findViewById(R.id.chat_recy);
        btnSned = view.findViewById(R.id.chat_btn_send);
        editContent = view.findViewById(R.id.chat_et_content);
        chatEditLayout = view.findViewById(R.id.chat_edit_layout);
        devSelectLTv = view.findViewById(R.id.chat_dev_select_tv);
        recyclerView.setOnClickListener(this);
        btnSned.setOnClickListener(this);
        btnSned.setOnLongClickListener(this);
        devSelectLTv.setOnClickListener(this);

        recyclerView.addOnScrollListener(new TopScrollListener());

    }

    @SuppressLint("ClickableViewAccessibility")
    public void initList() {
        layoutManager = new LinearLayoutManager(getContext());
        chatAdapter = new ChatAdapter(this);
        chatAdapter.setOnItemClickListener(this);
        chatAdapter.setOnItemLongClickListener(this);
        chatAdapter.setOnCheckedChangeListener(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
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

    /**
     * @author fgsq
     * @comments 初始化消息
     * @date 2024/5/22 11:01
     */
    @SuppressLint("NotifyDataSetChanged")
    public void loadData() {
        int oldItemCount = chatAdapter.getItemCount();
        List<MessageContent> messageContents = messsageDButil.queryMessage(pageSize, pageCount++);
//        List<MessageContent> messageContents = messsageDButil.queryMessage();
        // 将新数据插入到列表开头
        messageContentList.addAll(0, messageContents);
        chatAdapter.notifyDataSetChanged();
        // 保持滚动位置
        if (oldItemCount > 0 && !messageContents.isEmpty()) {
            // 让RecyclerView保持在原来的滚动位置
            layoutManager.scrollToPositionWithOffset(messageContents.size(), 0);
        }
    }

    @Override
    public void onItemCheckedChanged(boolean check, MessageContent messageContent, int position) {
        if (check) {
            checkedMessageList.add(messageContent);
        } else {
            checkedMessageList.remove(messageContent);
        }
    }

    /**
     * @author fgsq
     * @comments 消息列表项目点击
     * @date 2024/5/22 11:01
     */
    @Override
    public void onItemClick(MessageContent messageContent, int position) {
        boolean POEN_MEDIA_PLAYER = prefUtil.getBoolean(PreConfig.POEN_MEDIA_PLAYER, true);
        /*if (messageContent instanceof MessageGPSContent) {
            MessageGPSContent messageGPSContent = (MessageGPSContent) messageContent;
            MapUtils.openMap(getContext(), messageGPSContent);
        } else */if (messageContent instanceof MessageStreamContent) {
            T.s((R.string.web_page_file_sharing_view_not_supported));
        } else if (messageContent instanceof MessageFileContent) {
            MessageFileContent fileContent = (MessageFileContent) messageContent;
            // 消息内容传输完成
            if (fileContent.getStatus() == MessageContent.SUCCESS) {
                if (POEN_MEDIA_PLAYER && messageContent instanceof MessageMediaContent) {
                    MessageMediaContent mediaContent = (MessageMediaContent) fileContent;
                    MessageMediaContent mediaInfo = new MessageMediaContent();
                    mediaInfo.setPath(mediaContent.getPath());
                    mediaInfo.setLength(mediaContent.getLength());
                    mediaInfo.setName(mediaContent.getContent());
                    if (mediaContent.isVideo()) {
                        VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
                    } else {
                        List<MessageMediaContent> mediaInfos = Collections.singletonList(mediaInfo);
                        ReviewImages.openActivity(getActivity(), mediaInfos, mediaInfos, false, 0, 1);
                    }
                } else {
                    if (messageContent instanceof MessageUriContent) {
                        T.s(R.string.file_sharing_view_not_supported);
                    } else if (messageContent instanceof MessageFolderContent) {
                        T.s(R.string.folder_opening_not_supported);
                    } else {
                        if (fileContent.getStatus() == MessageContent.SUCCESS) {
                            FileUtil.openFile(dataCenterActivity, new File(fileContent.getPath()));
                        }
                    }
                }
            } else {
                T.s(R.string.file_transfer_unfinished);
            }

        }
    }


    @Override
    public boolean onItemLongClick(MessageContent messageContent, View view, int position) {
        PopupMenu popupMenu = new PopupMenu(Objects.requireNonNull(getContext()), view);
        popupMenu.getMenuInflater().inflate(R.menu.recy_menu, popupMenu.getMenu());
        popupMenu.setGravity(messageContent.isLeft() ? Gravity.START : Gravity.END);

//        if (messageContent instanceof MessageFileContent) {
//            MessageFileContent fileContent = (MessageFileContent) messageContent;
//            if (fileContent.getContent().endsWith(".apk")) {
//                popupMenu.getMenu().add(android.view.Menu.NONE, 666, 666, "使用命令安装apk");
//            }
//        }

        if (messageContent instanceof MessageFileContent) {
            popupMenu.getMenu().add(android.view.Menu.NONE, 777, 777, messageContent.isTextSelection() ? "取消选择文本" : "选择文本");
        }

        // 弹出式菜单的菜单项点击事件
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_delete) {
                delMessage(messageContent, true);
            } else if (item.getItemId() == R.id.menu_copy) {
                // 复制文本
                mUtil.copyString(messageContent.getContent(), getContext());
            } else if (item.getItemId() == R.id.menu_send) {
                if (messageContent.getStatus() != MessageContent.SUCCESS && messageContent.isLeft()) {
                    T.s((R.string.file_transfer_unfinished));
                } else {
                    if (messageContent instanceof MessageMediaContent) {
                        MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
                        MessageMediaContent fileSource = new MessageMediaContent();
                        fileSource.setName(mediaContent.getName());
                        fileSource.setPath(mediaContent.getPath());
                        fileSource.setLength(mediaContent.getLength());
                        dataCenterActivity.sendSingleFile(fileSource);
                    } else if (messageContent instanceof MessageFolderContent) {
                        MessageFolderContent folderContent = (MessageFolderContent) messageContent;
                        MessageFolderContent fileSource = new MessageFolderContent();
                        fileSource.setName(folderContent.getName());
                        fileSource.setPath(folderContent.getPath());
                        fileSource.setIsPreView(false);
                        fileSource.setLength(folderContent.getLength());
                        dataCenterActivity.sendSingleFile(fileSource);
                    } else if (messageContent instanceof MessageFileContent) {
                        MessageFileContent fileContent = (MessageFileContent) messageContent;
                        if (StringUtils.isEmpty(fileContent.getPath())) {
                            T.s("不支持外部分享文件重新发送");
                            return false;
                        }
                        MessageFileContent fileSource = new MessageFileContent();
                        fileSource.setName(fileContent.getName());
                        fileSource.setPath(fileContent.getPath());
                        fileSource.setIsPreView(false);
                        fileSource.setLength(fileContent.getLength());
                        dataCenterActivity.sendSingleFile(fileSource);
                    }

                }
            } else if (item.getItemId() == R.id.menu_multiple_select_del) {
                chatAdapter.setCheckMode(true);
                dataCenterActivity.setDeleteMode(true);
            } else if (item.getItemId() == 777) {
                messageContent.setTextSelection(!messageContent.isTextSelection());
                chatAdapter.refresh();
            }
            return false;
        });
        popupMenu.show();
        return true;
    }

    public List<MessageContent> getMessageContents() {
        return messageContentList;
    }


    @SuppressWarnings("all")
    public void updateMessage(MessageContent messageContent, boolean save) {
        if (Config.SAVE_TO_GALLERY) {
            // 扫描文件夹下的图片
            ImageUtils.scannerImage(getContext(), Config.FILE_SAVE_PATH);
        }
        MessageFileContent fileContent = (MessageFileContent) messageContent;
        // 获取数据在列表中的下标
        int dataPosition = chatAdapter.getDataPosition(messageContent);
        if (dataPosition == -1) return;
        if (fileContent.getViewType() == ChatAdapter.TYPE_FILE_MSG_LEFT || fileContent.getViewType() == ChatAdapter.TYPE_FILE_MSG_RIGHT
                || fileContent.getViewType() == ChatAdapter.TYPE_MEDIA_MSG_LEFT || fileContent.getViewType() == ChatAdapter.TYPE_MEDIA_MSG_RIGHT) {
            // 获取视图并更新视图数据
            FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
            if (viewHolder != null) {
                viewHolder.progressBar.setProgress(fileContent.getProgress());
                if (fileContent.existStatus(MessageContent.IN)) {
                    viewHolder.progressBar.setVisibility(View.VISIBLE);
                    viewHolder.stateTv.setVisibility(View.GONE);
                    viewHolder.stateTv.setTextColor(getContext().getResources().getColor(R.color.itemTextColor));
                } else {
                    viewHolder.progressBar.setVisibility(View.GONE);
                    viewHolder.stateTv.setVisibility(View.VISIBLE);
                    viewHolder.stateTv.setText(fileContent.getStateMessage());
                    if (fileContent.existStatus(MessageContent.SUCCESS)) {
                        viewHolder.stateTv.setTextColor(getContext().getResources().getColor(R.color.itemTextColor));
                    } else if (fileContent.existStatus(MessageContent.ERROR)) {
                        viewHolder.stateTv.setTextColor(Color.RED);
                    }
                }
            } /*else {
                chatAdapter.notifyItemChanged(dataPosition);
            }*/
        } /*else {
            chatAdapter.notifyItemChanged(dataPosition);
        }*/
        if (dataPosition != -1) {
            chatAdapter.notifyItemChanged(dataPosition);
        }
    }

    int i = 0;

    public void addMessage(MessageContent messageContent, boolean save) {
        checkAndAddChatTime(messageContent.getId(), save);
        // 保存消息内容
        messageContentList.add(messageContent);
        if (Config.SAVE_MESSAGE && save) {
            messsageDButil.addMessage(messageContent);
        }
        chatAdapter.refresh();
        recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        // 发送通知
        if (Config.MESSAGE_NOTIFICAION && !isFragmentVisible()) {
            NotificationUtils.showNotification(dataCenterActivity, messageContent.getUserName(), messageContent.getContent());
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addListMessage(List<MessageContent> messageContent) {
        messageContentList.addAll(messageContent);
        chatAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    public void deleteBindTime(String bindId) {
        String id = messsageDButil.queryByBindId(bindId);
        if (id != null) {
            messsageDButil.delMessage(id);
        }
    }

    public void deleteList(List<MessageContent> messageContents) {
        for (MessageContent messageContent : messageContents) {
            messsageDButil.delMessage(messageContent.getId());
        }
    }

    public void delMessage(MessageContent messageContent, boolean save) {
        if (save) {
            deleteBindTime(messageContent.getId());
            messsageDButil.delMessage(messageContent.getId());
        }
        if (messageContent instanceof MessageFileContent) {
            MessageFileContent fileContent = (MessageFileContent) messageContent;
            ThreadUtils.runThread(() -> {
                try {
                    fileContent.cancelFileTransfer();
                } catch (IOException e) {
                    LLog.error("取消传输异常: ", e);
                }
            });
        }
        List<MessageContent> times = new ArrayList<>();
        for (MessageContent content : messageContentList) {
            if (content.getViewType() == ChatAdapter.TYPE_TIME_MSG) {
                MessageTimeContent timeContent = (MessageTimeContent) content;
                if (Objects.equals(messageContent.getId(), timeContent.getBindId())) {
                    times.add(content);
                    break;
                }
            }
        }
        messageContentList.removeAll(times);
        // 移除数据
        messageContentList.remove(messageContent);
        // 更新列表
        chatAdapter.refresh();
    }


    @Override
    public boolean onBack() {
        if (chatAdapter.isCheckMode()) {
            chatAdapter.setCheckMode(false);
            dataCenterActivity.setDeleteMode(false);
            return true;
        }
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
                    T.s((R.string.Input_cannot_be_empty));
                    return;
                } /*else if (message.length() > 700) {
                    T.s("字符不长度能超出700个");
                    return;
                }*/
                MessageContent messageContent = new MessageContent();
                messageContent.setId(StringUtils.getUUID());
                messageContent.setLeft(false);
                messageContent.setContent(message);
                messageContent.setUserName(LANService.getInstance().getDevName());
                messageContent.setToUser(selectedDevice == null ? getContext().getString(R.string.all_devices) : selectedDevice.getDevName());
                LANService.getInstance().broadcastMessage(selectedDevice, message, false);
                addMessage(messageContent, true);
                editContent.setText("");
                break;
            }
            case R.id.chat_dev_select_tv: {
                selectDevice();
                break;
            }
            case R.id.chat_recy: {
                T.s("test");
                break;
            }
            default:
                break;
        }
    }

    public void selectDevice() {
        DeviceSelectDialog deviceSelectDialog = new DeviceSelectDialog(getContext());
        deviceSelectDialog.setOnDeviceSelect(device -> {
            if (device.getDevIP() == null) {
                editContent.setHint(getString(R.string.send_message_hint));
                devSelectLTv.setText(getString(R.string.all_devices));
                selectedDevice = null;
            } else {
                selectedDevice = device;
                editContent.setHint(String.format(getString(R.string.send_message_to), device.getDevName()));
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.chat_btn_send) {
            String message = editContent.getText().toString();
            if (message.isEmpty()) {
                T.s((R.string.Input_cannot_be_empty));
                return false;
            } else if (message.length() > 700) {
                T.s((R.string.text_length_max_700));
                return false;
            }
            MessageContent messageContent = new MessageContent();
            messageContent.setId(StringUtils.getUUID());
            messageContent.setLeft(false);
            messageContent.setContent(message);
            messageContent.setUserName(LANService.getInstance().getDevName());
            messageContent.setToUser(selectedDevice == null ? getString(R.string.all_devices) : selectedDevice.getDevName());
            LANService.getInstance().broadcastMessage(selectedDevice, message, true);
            addMessage(messageContent, true);
            editContent.setText("");
        }
        return true;
    }

    /**
     * 删除所有文件不存在的消息
     */
    private void deleteFileMessageByNotExist() {
        List<MessageContent> list = new ArrayList<>(messageContentList);
        List<MessageContent> times = new ArrayList<>();
        Iterator<MessageContent> iterator = messageContentList.iterator();
        while (iterator.hasNext()) {
            MessageContent next = iterator.next();
            if (next instanceof MessageFileContent) {
                MessageFileContent messageFileContent = (MessageFileContent) next;
                if (messageFileContent.existStatus(MessageContent.SUCCESS)) {
                    File file = new File(messageFileContent.getPath());
                    if (!file.exists()) {
                        deleteBindTime(next.getId());
                        messsageDButil.delMessage(next.getId());
                        iterator.remove();
                        for (MessageContent messageContent : list) {
                            if (messageContent.getViewType() == ChatAdapter.TYPE_TIME_MSG) {
                                MessageTimeContent timeContent = (MessageTimeContent) messageContent;
                                if (Objects.equals(next.getId(), timeContent.getBindId())) {
                                    times.add(timeContent);
                                    break;
                                }
                            }
                        }
                    }
                } else if (messageFileContent.existStatus(MessageContent.FILE_NOT_EXIST)) {
                    deleteBindTime(next.getId());
                    messsageDButil.delMessage(next.getId());
                    iterator.remove();
                    for (MessageContent messageContent : list) {
                        if (messageContent.getViewType() == ChatAdapter.TYPE_TIME_MSG) {
                            MessageTimeContent timeContent = (MessageTimeContent) messageContent;
                            if (Objects.equals(next.getId(), timeContent.getBindId())) {
                                times.add(timeContent);
                                break;
                            }
                        }
                    }
                }
            }
        }
        messageContentList.removeAll(times);
        deleteList(messageContentList);
        chatAdapter.refresh();
    }

    /**
     * 删除所有文本消息
     */
    public void deleteAllTextMessage() {
        List<MessageContent> list = new ArrayList<>(messageContentList);
        List<MessageContent> times = new ArrayList<>();
        Iterator<MessageContent> iterator = messageContentList.iterator();
        while (iterator.hasNext()) {
            MessageContent next = iterator.next();
            if (!(next instanceof MessageFileContent || next instanceof MessageTimeContent)) {
                deleteBindTime(next.getId());
                messsageDButil.delMessage(next.getId());
                iterator.remove();
                for (MessageContent messageContent : list) {
                    if (messageContent.getViewType() == ChatAdapter.TYPE_TIME_MSG) {
                        MessageTimeContent timeContent = (MessageTimeContent) messageContent;
                        if (Objects.equals(next.getId(), timeContent.getBindId())) {
                            times.add(timeContent);
                            break;
                        }
                    }
                }
            }
        }
        messageContentList.removeAll(times);
        deleteList(messageContentList);
        chatAdapter.refresh();
    }

    /**
     * 删除所有文件消息
     */
    public void deleteFileMessage() {
        List<MessageContent> list = new ArrayList<>(messageContentList);
        List<MessageContent> times = new ArrayList<>();
        Iterator<MessageContent> iterator = messageContentList.iterator();
        while (iterator.hasNext()) {
            MessageContent next = iterator.next();
            if (next instanceof MessageFileContent) {
                deleteBindTime(next.getId());
                messsageDButil.delMessage(next.getId());
                iterator.remove();
                for (MessageContent messageContent : list) {
                    if (messageContent.getViewType() == ChatAdapter.TYPE_TIME_MSG) {
                        MessageTimeContent timeContent = (MessageTimeContent) messageContent;
                        if (Objects.equals(next.getId(), timeContent.getBindId())) {
                            times.add(timeContent);
                            break;
                        }
                    }
                }
            }
        }
        messageContentList.removeAll(times);
        deleteList(messageContentList);
        chatAdapter.refresh();
    }

    /**
     * 删除所有消息
     */
    public void deleteAllMessage() {
        messsageDButil.deleteAllMessage();
        messageContentList.clear();
        chatAdapter.refresh();
    }

    public void setEditContent(String editContent) {
        this.editContent.setText(editContent);
    }

    public void messageDelete() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setTitle(getString(R.string.please_select_operation));
        String[] items = new String[]{getString(R.string.clear_all_messages), getString(R.string.clear_all_text_messages), getString(R.string.clear_all_file_messages), getString(R.string.clear_all_deleted_file_messages)};

        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0: {
                    deleteAllMessage();
                    break;
                }
                case 1: {
                    deleteAllTextMessage();
                    break;
                }
                case 2: {
                    deleteFileMessage();
                    break;
                }
                case 3: {
                    deleteFileMessageByNotExist();
                    break;
                }
                default:
                    break;
            }
            arg0.dismiss();
        });
        builder.show();
    }

    public void deleteSelectedMessages() {
        chatAdapter.setCheckMode(false);
        dataCenterActivity.setDeleteMode(false);
        List<MessageContent> list = new ArrayList<>(messageContentList);
        list.retainAll(checkedMessageList);
        messageContentList.removeAll(list);
        for (MessageContent messageContent : list) {
            String timeId = messsageDButil.queryByBindId(messageContent.getId());
            if (!StringUtils.isEmpty(timeId)) {
                messsageDButil.delMessage(timeId);
                MessageContent content = new MessageContent();
                content.setId(timeId);
                messageContentList.remove(content);
            }
            messsageDButil.delMessage(messageContent.getId());
        }
        chatAdapter.refresh();
        checkedMessageList.clear();
    }

    public void setChatEditVisibility(boolean deleteMode) {
        chatEditLayout.setVisibility(deleteMode ? View.GONE : View.VISIBLE);
    }
}
