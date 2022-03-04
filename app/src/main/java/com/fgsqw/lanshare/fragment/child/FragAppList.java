package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLayoutManager;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.fragment.adapter.AppAdapter;
import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.utils.CopFileTask;
import com.fgsqw.lanshare.utils.FIleSerachUtils;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.util.Arrays;
import java.util.List;

public class FragAppList extends BaseFragment implements AppAdapter.OnItemClickListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private ViewPager vp;
    View view;


    TextView tvCount;
    CheckBox checkSelectAll;
    SwipeRefreshLayout appSwipe;
    RecyclerView appRecy;

    private List<ApkInfo> apkFileList;//所有扫描到的Apk文件
    public List<ApkInfo> mSelectlist;

    AppAdapter appAdapter;
    MLayoutManager mManagerLayout;

    public DataCenterActivity dataCenterActivity;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_app, container, false);
            initView();
            initList();
        }

        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        loading();
        return view;

    }

    @SuppressLint("CutPasteId")
    public void initView() {
        tvCount = view.findViewById(R.id.app_tv_count);
        checkSelectAll = view.findViewById(R.id.app_check_select_all);
        appSwipe = view.findViewById(R.id.app_swipe);
        appRecy = view.findViewById(R.id.app_recy);

        checkSelectAll.setOnCheckedChangeListener(this);
    }

    public void initList() {
        mManagerLayout = new MLayoutManager(getActivity(), 4);
        appAdapter = new AppAdapter(this, false);
        appRecy.setLayoutManager(mManagerLayout);
        appRecy.setAdapter(appAdapter);
        appAdapter.setOnItemClickListener(this);
        appSwipe.setOnRefreshListener(this::loading);
    }


    @Override
    public void OnLongItenClick(ApkInfo fileUtils, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("请选择操作");
        String[] items;
        items = new String[]{"发送", "备份", "取消",};
        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0:
                    dataCenterActivity.sendFiles(Arrays.asList(fileUtils));
                    break;
                case 1:
                    new CopFileTask(getContext(), fileUtils.getPath(), Config.FILE_SAVE_PATH + "备份/" + fileUtils.getName()).execute(0);
                    break;
                case 2:
                    //取消
                    break;
            }
        });
        builder.show();
    }

    @Override
    public void OnItemClick(ApkInfo fileUtils, boolean isSelect, List<ApkInfo> mSelectlist, View view) {
        this.mSelectlist = mSelectlist;
    }


    @SuppressLint("SetTextI18n")
    private void loading() {
        ViewUpdate.runThread(() -> {
            ViewUpdate.threadUi(() -> {
                tvCount.setText("加载中");
                appSwipe.setRefreshing(true);
            });
            apkFileList = FIleSerachUtils.loadApkForSDCard(getContext());
            ViewUpdate.threadUi(() -> {
                if (apkFileList != null && !apkFileList.isEmpty()) {
                    if (appAdapter != null) {
                        appAdapter.refresh(apkFileList);
                        tvCount.setText(apkFileList.size() + "个应用");
                        appSwipe.setRefreshing(false);
                    }
                }
            });
        });
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
      /*  switch (v.getId()) {
            case R.id.app_check_select_all: {
                  appAdapter.setSelecteByApkinfo(apkFileList);
                break;
            }
            default:
                break;
        }*/
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.app_check_select_all: {
                if (isChecked) {
                    appAdapter.setSelecteByApkinfo(apkFileList);
                } else {
                    appAdapter.clearImageSelect();
                }
                break;
            }
            default:
                break;
        }
    }
}
