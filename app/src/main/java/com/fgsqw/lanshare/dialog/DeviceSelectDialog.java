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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeviceSelectDialog extends Dialog implements DeviceDialogAdapter.OnItemClickListener {
    RecyclerView recyclerView;
    TextView tvCount;
    TextView tvNotDev;

    public DeviceSelectDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_select);
        initView();
        initList();
    }

    public void initView() {
        recyclerView = findViewById(R.id.dev_dialog_recy);
        tvCount = findViewById(R.id.dev_dialog_count_tv);
        tvNotDev = findViewById(R.id.dev_dialog_not_dev_tv);
        tvCount.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    public void initList() {
        Map<String, Device> deviceMap = LANService.getInstance().devices;

        List<Device> deviceList = new ArrayList<>();
        Device device = new Device();
        device.setDevName("所有设备");
        deviceList.add(device);
        if (deviceMap.size() > 0) {
            deviceList.addAll(deviceMap.values());
            tvNotDev.setVisibility(View.GONE);
        }

        DeviceDialogAdapter adapter = new DeviceDialogAdapter(getContext(), deviceList);
        adapter.setOnItemClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }


    FileSendDialog.OnDeviceSelect onDeviceSelect;

    public void setOnDeviceSelect(FileSendDialog.OnDeviceSelect onDeviceSelect) {
        this.onDeviceSelect = onDeviceSelect;
    }

    @Override
    public void onClick(Device device, int position) {
        if (onDeviceSelect != null) {
            dismiss();
            onDeviceSelect.deviceSelect(device);
        }
    }

    public interface OnDeviceSelect {
        void deviceSelect(Device device);
    }
}
