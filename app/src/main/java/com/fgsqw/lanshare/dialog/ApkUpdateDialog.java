package com.fgsqw.lanshare.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.dialog.adapter.ApkUpdateDialogAdapter;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;

import java.util.List;

public class ApkUpdateDialog extends BaseDialog implements ApkUpdateDialogAdapter.OnClickListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private RecyclerView recyclerView;
    private ApkUpdateDialogAdapter adapter;
    private CheckBox appUpdateSelectAll;
    private LinearLayout appUpdateLayoutCancel;
    private LinearLayout appUpdateLayoutConfirm;
    private OnUpdate onUpdate = null;
    private List<MessageApkContent> fileList;

    public ApkUpdateDialog(@NonNull Context context,List<MessageApkContent> fileList) {
        super(context, R.style.AlertDialogTheme);
        this.fileList = fileList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_update_dialog);
        initView();
    }


    public void initView() {
        recyclerView = findViewById(R.id.app_update_recy);
        appUpdateSelectAll = findViewById(R.id.app_update_select_all);
        appUpdateLayoutCancel = findViewById(R.id.app_update_layout_cancel);
        appUpdateLayoutConfirm = findViewById(R.id.app_update_layout_confirm);
        adapter = new ApkUpdateDialogAdapter(getContext(),fileList);
        adapter.setOnClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        appUpdateSelectAll.setOnCheckedChangeListener(this);
        appUpdateLayoutCancel.setOnClickListener(this);
        appUpdateLayoutConfirm.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
    }

    @Override
    public void OnClick(int position) {

    }

    @Override
    public void OnLongClick(int position) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.app_update_select_all) {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.unSelectAll();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.app_update_layout_cancel: {
                dismiss();
                break;
            }
            case R.id.app_update_layout_confirm: {
                if (onUpdate != null) {
                    onUpdate.update(adapter.getFileSelects());
                }
                dismiss();
                break;
            }
            default:
                break;
        }
    }


    public void setOnUpdate(OnUpdate onUpdate) {
        this.onUpdate = onUpdate;
    }

    public interface OnUpdate {
        void update(List<MessageApkContent> apkInfos);
    }
}
