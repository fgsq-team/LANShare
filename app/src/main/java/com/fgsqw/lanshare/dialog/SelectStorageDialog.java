package com.fgsqw.lanshare.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.dialog.adapter.SelectStorageDialogAdapter;
import com.fgsqw.lanshare.pojo.StorageInfo;
import com.fgsqw.lanshare.utils.PermissionsUtils;
import com.fgsqw.lanshare.utils.StorageUtils;
import com.fgsqw.lanshare.utils.VersionUtils;

import java.util.List;

public class SelectStorageDialog extends BaseDialog implements SelectStorageDialogAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private SelectStorageDialogAdapter dialogAdapter;
    private OnStorageSelect onStorageSelect;
    private Context context;

    public SelectStorageDialog(@NonNull Context context) {
        super(context, R.style.AlertDialogTheme);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_select);
        initView();
        initList();
    }

    private void initList() {
        List<StorageInfo> storageList = StorageUtils.getStorageList(getContext());
        dialogAdapter.refresh(storageList);
    }

    private void initView() {
        recyclerView = findViewById(R.id.storage_dialog_recy);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        dialogAdapter = new SelectStorageDialogAdapter();
        recyclerView.setAdapter(dialogAdapter);
        dialogAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onClick(StorageInfo storageInfo, int position) {
        if (!storageInfo.getEmulated()) {
            Uri uri = PermissionsUtils.docPath2Uri(storageInfo.getUuid() + ":");
            //获取权限,没有权限返回null有权限返回授权uri字符串
            if(VersionUtils.isAfterAndroid11()){
                String existsPermission = PermissionsUtils.existsGrantedUriPermission(uri, context);
                if (existsPermission == null) {
                    if (!(context instanceof Activity)) {
                        throw new RuntimeException("请手动在软件中授权此文件夹");
                    }
                    PermissionsUtils.goApplyUriPermissionPage(uri, (Activity) context);
                    return;
                }
            }
        }
        if (onStorageSelect != null) {
            onStorageSelect.select(storageInfo);
        }
    }

    public void setOnStorageSelect(OnStorageSelect onStorageSelect) {
        this.onStorageSelect = onStorageSelect;
    }

    public interface OnStorageSelect {
        void select(StorageInfo storageInfo);
    }
}
