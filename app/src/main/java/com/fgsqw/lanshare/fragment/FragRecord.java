package com.fgsqw.lanshare.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.fragment.adapter.RecordAdapter;
import com.fgsqw.lanshare.pojo.DataObject;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.RecordFile;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.pojo.mSocket;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 弃用
 */
@Deprecated
public class FragRecord extends BaseFragment implements RecordAdapter.OnItemClickListener {


    View view;
    private final List<RecordFile> fileInfoList = new ArrayList<>();
    RecyclerView recyclerView;
    RecordAdapter recordAdapter;
    LinearLayoutManager layoutManager;
    TextView notFile;

    public DataCenterActivity dataCenterActivity;

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

                List<RecordFile> recordFileList = (List<RecordFile>) msg.obj;
                fileInfoList.addAll(recordFileList);
                refreshData();
                recyclerView.scrollToPosition(recordAdapter.getItemCount() - 1);
            } else if (msg.what == mCmd.SERVICE_PROGRESS) {          // 更新进度

                RecordFile recordFile = (RecordFile) msg.obj;
                // 获取数据在列表中的下标
                int dataPosition = recordAdapter.getDataPosition(recordFile);
                // 获取视图并更新视图数据
                RecordAdapter.ViewHolder viewHolder = (RecordAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
                if (viewHolder != null) {
                    viewHolder.mProgressBar.setProgress(recordFile.getProgress());
                } else {
                    recordAdapter.notifyItemChanged(dataPosition);
                }
            } else if (msg.what == mCmd.SERVICE_CLOSE_PROGRESS) {    // 完成

                RecordFile recordFile = (RecordFile) msg.obj;
                // 获取数据在列表中的下标
                int dataPosition = recordAdapter.getDataPosition(recordFile);
                // 获取视图并更新视图数据
                RecordAdapter.ViewHolder viewHolder = (RecordAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
                if (viewHolder != null) {
                    viewHolder.mProgressBar.setProgress(recordFile.getProgress());
                    if (recordFile.getSuccess() != null) {
                        viewHolder.mProgressBar.setVisibility(View.GONE);
                        viewHolder.mMessage.setVisibility(View.VISIBLE);
                        viewHolder.mMessage.setText(recordFile.getMessage());
                        if (!recordFile.getSuccess()) {
                            viewHolder.mMessage.setTextColor(Color.RED);
                        } else {
                            viewHolder.mMessage.setTextColor(0xFF939393);
                        }
                    } else {
                        viewHolder.mProgressBar.setVisibility(View.VISIBLE);
                        viewHolder.mMessage.setVisibility(View.GONE);
                        viewHolder.mMessage.setTextColor(0xFF939393);
                    }
                } else {
                    recordAdapter.notifyItemChanged(dataPosition);
                }
            }
        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_record, container, false);
            initView();
            initList();
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }

        return view;

    }

    public void initView() {
        recyclerView = view.findViewById(R.id.record_recy);
        notFile = view.findViewById(R.id.record_not_file);
    }

    public void initList() {
        recordAdapter = new RecordAdapter(fileInfoList);
        recordAdapter.setOnItemClickListener(this);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recordAdapter);

    }


    public void refreshData() {
        recordAdapter.refresh(fileInfoList);
        if (fileInfoList.size() <= 0) {
            notFile.setVisibility(View.VISIBLE);
        } else {
            notFile.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(int position) {
        RecordFile recordFile = fileInfoList.get(position);
        if (recordFile.getSuccess() != null && recordFile.getSuccess()) {
            FileUtil.openFile(dataCenterActivity, new File(recordFile.getPath()));
        }
    }

    // 点击了取消按钮
    @Override
    public void onCloseClick(int position) {
        RecordFile recordFile = fileInfoList.get(position);
        mSocket socket = recordFile.getSocket();

        if (recordFile.isRecv()) {
            //  LANService.service.sendCloseCmd(recordFile, socket.getOut());
        } else {
            socket.mClose();
        }
        // 移除数据
        fileInfoList.remove(recordFile);
        // 更新列表
        refreshData();

    }

    @Override
    public void onLongClick(int position) {
    }

    public Handler getHandler() {
        return handler;
    }


}
