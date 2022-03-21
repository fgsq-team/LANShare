package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
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
import com.fgsqw.lanshare.fragment.minterface.ChildBaseMethod;
import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.utils.CopFileTask;
import com.fgsqw.lanshare.utils.FIleSerachUtils;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FragAppList extends BaseFragment implements AppAdapter.OnItemClickListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener, ChildBaseMethod {

    private ViewPager vp;
    View view;


    TextView tvCount;
    CheckBox checkSelectAll;
    SwipeRefreshLayout appSwipe;
    RecyclerView appRecy;

    public List<ApkInfo> apkFileList;//所有扫描到的Apk文件
    public final List<ApkInfo> mSelectlist = new LinkedList<>();

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
        appAdapter = new AppAdapter(this);
        appRecy.setLayoutManager(mManagerLayout);
        appRecy.setAdapter(appAdapter);
        appAdapter.setOnItemClickListener(this);
        appSwipe.setOnRefreshListener(this::loading);
    }


    @Override
    public void OnLongItenClick(ApkInfo apkInfo, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("请选择操作");
        String[] items;
        items = new String[]{"发送", "备份", "打开", "卸载", "取消",};
        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0:
                    dataCenterActivity.sendOneFile(apkInfo);
                    break;
                case 1:
                    // 备份至本地
                    new CopFileTask(getContext(), apkInfo.getPath(), Config.FILE_SAVE_PATH + "备份/" + apkInfo.getName()).execute(0);
                    break;
                case 2:
                    // 打开程序
                    startApp(apkInfo.getPackageName());
                    break;
                case 3:
                    // 卸载程序
                    unstallApp(apkInfo.getPackageName());
                    break;
                case 4:
                    //取消
                    break;
            }
        });
        builder.show();
    }

    @Override
    public void OnItemClick(ApkInfo apkInfo, boolean isSelect, int position) {

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
                        appAdapter.refresh();
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

    //卸载应用
    public void unstallApp(String packageName) {
        Intent uninstall_intent = new Intent();
        uninstall_intent.setAction(Intent.ACTION_DELETE);
        uninstall_intent.setData(Uri.parse("package:" + packageName));
        startActivity(uninstall_intent);
    }

    // 开启应用
    public void startApp(String packageName) {
        getContext().startActivity(getContext().getPackageManager().getLaunchIntentForPackage(packageName));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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

    public List<ApkInfo> getApkFileList() {
        return apkFileList;
    }

    public List<ApkInfo> getSelectlist() {
        return mSelectlist;
    }

    @Override
    public void clearSelect() {
        if (mSelectlist.size() > 0 && isVisible()) {
            mSelectlist.clear();
            appAdapter.refresh();
            checkSelectAll.setChecked(false);
        }

    }
}
