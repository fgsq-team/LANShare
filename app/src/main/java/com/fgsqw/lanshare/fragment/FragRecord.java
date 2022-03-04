package com.fgsqw.lanshare.fragment;

import android.content.Context;
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

import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.fragment.adapter.RecordAdapter;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.pojo.DataObject;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.RecordFile;
import com.fgsqw.lanshare.pojo.mSocket;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.ViewUpdate;
import com.fgsqw.lanshare.utils.mUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FragRecord extends BaseFragment implements RecordAdapter.OnItemClickListener {


    View view;
    private final List<RecordFile> fileInfoList = new ArrayList<>();
    RecyclerView recyclerView;    //列表
    RecordAdapter recordAdapter;
    TextView notFile;

    public DataCenterActivity dataCenterActivity;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    // 用户接收LANService的消息
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
                recordAdapter.updateProgress(recordFile);

            } else if (msg.what == mCmd.SERVICE_CLOSE_PROGRESS) {    // 完成
                RecordFile recordFile = (RecordFile) msg.obj;
                recordAdapter.updateMessage(recordFile);

            } else if (msg.what == mCmd.SERVICE_UPDATE_DEVICES) {
               /* Map<String, Device> devices = (Map<String, Device>) msg.obj;
                StringBuilder sb = new StringBuilder("设备数:" + devices.size());
                Set<String> strings = devices.keySet();
                for (String key : strings) {
                    Device device = devices.get(key);
                    sb.append("\n 设备:").append(device.getDeviceName())
                            .append(" 型号:").append(device.getDevName())
                            .append(" IP:").append(device.getDevIP())
                            .append(":").append(device.getDevPort());
                }*/
                //   textView.setText(sb);
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recordAdapter);

    }

    // 测试
    public void test() {
        for (int i = 0; i < 1000; i++) {
            RecordFile recordFile = new RecordFile();
            recordFile.setLength(i + 1);
            recordFile.setProgress(50);
            recordFile.setName("" + i);
            recordFile.setPath("");
            fileInfoList.add(recordFile);
        }

        ViewUpdate.runThread(() -> {
            while (true) {
                RecordFile recordFile = fileInfoList.get(3);
                recordFile.setProgress(mUtil.random(1, 100));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ViewUpdate.threadUi(() -> {
                    recordAdapter.updateProgress(recordFile);
                });


            }
        });

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
//        recordAdapter.notifyItemRemoved(position);//通知移除该条
//        recordAdapter.notifyItemRangeChanged(position, fileInfoList.size() - position);//更新适配器这条后面
    }

    @Override
    public void onCloseClick(int position) {
        RecordFile recordFile = fileInfoList.get(position);
        mSocket socket = recordFile.getSocket();

        if (recordFile.isRecv()) {
            LANService.service.sendCmd(recordFile, socket.getOut());
        } else {
            socket.mClose();
        }
        fileInfoList.remove(recordFile);
        refreshData();

    }

    @Override
    public void onLongClick(int position) {
    }

    public Handler getHandler() {
        return handler;
    }


}
