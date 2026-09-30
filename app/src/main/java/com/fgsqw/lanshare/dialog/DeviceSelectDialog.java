package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.dialog.adapter.DeviceDialogAdapter;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeviceSelectDialog extends BaseDialog implements DeviceDialogAdapter.OnItemClickListener, Runnable {
    private RecyclerView recyclerView;
    private TextView tvCount;
    private TextView tvNotDev;
    private TextView tvTitle;
    private boolean flag = false;
    private boolean showAllDevices = true;
    private DeviceDialogAdapter adapter;
    private String title;

    public DeviceSelectDialog(@NonNull Context context) {
        super(context, R.style.AlertDialogTheme);
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

    @Override
    public void setTitle(@Nullable CharSequence title) {
        this.title = title.toString();
    }

    public void initView() {
        recyclerView = findViewById(R.id.dev_dialog_recy);
        tvCount = findViewById(R.id.dev_dialog_count_tv);
        tvNotDev = findViewById(R.id.dev_dialog_not_dev_tv);
        tvTitle = findViewById(R.id.dev_dialog_title);
        tvCount.setVisibility(View.GONE);
        adapter = new DeviceDialogAdapter(getContext());
        adapter.setOnItemClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    public void initList() {
        List<Device> deviceList = new ArrayList<>();
        Device device = new Device();
        device.setDevName(getContext().getString(R.string.all_devices));
        deviceList.add(device);
        List<Device> deviceList1 = getDeviceList();
        if (!deviceList1.isEmpty()) {
            deviceList.addAll(deviceList1);
            tvNotDev.setVisibility(View.GONE);
        } else {
            tvNotDev.setVisibility(View.VISIBLE);
        }
        adapter.refresh(deviceList);
    }

    OnDeviceSelect onDeviceSelect;

    public void setOnDeviceSelect(OnDeviceSelect onDeviceSelect) {
        this.onDeviceSelect = onDeviceSelect;
    }

    private List<Device> getDeviceList() {
        LANService instance = LANService.getInstance();
        Map<String, Device> deviceMap = null;
        if (instance != null) {
            deviceMap = LANService.getInstance().getOnLineDevices();
        }
        List<Device> deviceList;
        if (deviceMap != null && !deviceMap.isEmpty()) {
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

    public void setShowAllDevices(boolean showAllDevices) {
        this.showAllDevices = showAllDevices;
    }

    @Override
    public void run() {
        while (flag) {
            List<Device> deviceList = new ArrayList<>();
            ThreadUtils.threadUi(() -> {
                        if (showAllDevices) {
                            Device device = new Device();
                            device.setDevName(getContext().getString(R.string.all_devices));
                            deviceList.add(device);
                        }
                        List<Device> deviceList1 = getDeviceList();
                        if (!deviceList1.isEmpty()) {
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
            } catch (InterruptedException ignored) {
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
