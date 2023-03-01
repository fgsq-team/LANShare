package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.dialog.adapter.DeviceDialogAdapter;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeviceSelectDialog extends Dialog implements DeviceDialogAdapter.OnItemClickListener, Runnable {
    RecyclerView recyclerView;
    TextView tvCount;
    TextView tvNotDev;
    private boolean flag = false;
    DeviceDialogAdapter adapter;


    public DeviceSelectDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_select);
        flag = true;
        initView();
        initList();
        ThreadUtils.runThread(this);
    }

    public void initView() {
        recyclerView = findViewById(R.id.dev_dialog_recy);
        tvCount = findViewById(R.id.dev_dialog_count_tv);
        tvNotDev = findViewById(R.id.dev_dialog_not_dev_tv);
        tvCount.setVisibility(View.GONE);
        adapter = new DeviceDialogAdapter(getContext());
        adapter.setOnItemClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    public void initList() {
        List<Device> deviceList = new ArrayList<>();
        Device device = new Device();
        device.setDevName("所有设备");
        deviceList.add(device);
        List<Device> deviceList1 = getDeviceList();
        if (deviceList1.size() > 0) {
            deviceList.addAll(deviceList1);
            tvNotDev.setVisibility(View.GONE);
        } else {
            tvNotDev.setVisibility(View.VISIBLE);
        }
        adapter.refresh(deviceList);
    }


    FileSendDialog.OnDeviceSelect onDeviceSelect;

    public void setOnDeviceSelect(FileSendDialog.OnDeviceSelect onDeviceSelect) {
        this.onDeviceSelect = onDeviceSelect;
    }


    private List<Device> getDeviceList() {
        LANService instance = LANService.getInstance();
        Map<String, Device> deviceMap = null;
        if (instance != null) {
            deviceMap = LANService.getInstance().devices;
        }
        List<Device> deviceList;
        if (deviceMap != null && deviceMap.size() > 0) {
            deviceList = new ArrayList<>(deviceMap.values());
        } else {
            deviceList = new ArrayList<>();
        }
        return deviceList;
    }


    @Override
    public void onClick(Device device, int position) {
        if (onDeviceSelect != null) {
            dismiss();
            onDeviceSelect.deviceSelect(device);
        }
    }

    @Override
    public void run() {
        while (flag) {
            List<Device> deviceList = new ArrayList<>();
            ThreadUtils.threadUi(() -> {
                        Device device = new Device();
                        device.setDevName("所有设备");
                        deviceList.add(device);
                        List<Device> deviceList1 = getDeviceList();
                        if (deviceList1.size() > 0) {
                            deviceList.addAll(deviceList1);
                            tvNotDev.setVisibility(View.GONE);
                        } else {
                            tvNotDev.setVisibility(View.VISIBLE);
                        }
                        adapter.refresh(deviceList);
                    }
            );
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        flag = false;
    }

    public interface OnDeviceSelect {
        void deviceSelect(Device device);
    }
}
