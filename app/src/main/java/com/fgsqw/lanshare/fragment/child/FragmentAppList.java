package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLayoutManager;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.FileInfoDialog;
import com.fgsqw.lanshare.dialog.InfoDialog;
import com.fgsqw.lanshare.fragment.adapter.AppAdapter;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.utils.*;

import java.util.LinkedList;
import java.util.List;

/**
 * @author fgsq
 * @comments APP选择界面
 * @date 2024/7/13 10:45
 */
public class FragmentAppList extends BaseFragment implements AppAdapter.OnItemClickListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private View view;


    private TextView tvCount;
    private CheckBox checkSelectAll;
    private CheckBox sysApp;
    private SwipeRefreshLayout appSwipe;
    private RecyclerView appRecy;

    public final List<MessageApkContent> mSelectlist = new LinkedList<>();

    private AppAdapter appAdapter;

    public DataCenterActivity dataCenterActivity;

    private AppInstallUninstallReceiver uninstallReceiver;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uninstallReceiver = new AppInstallUninstallReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        getContext().registerReceiver(uninstallReceiver, filter);
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
        loading(false);
        return view;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
    }

    @SuppressLint("CutPasteId")
    public void initView() {
        tvCount = view.findViewById(R.id.app_tv_count);
        checkSelectAll = view.findViewById(R.id.app_check_select_all);
        sysApp = view.findViewById(R.id.app_check_sys_app);
        appSwipe = view.findViewById(R.id.app_swipe);
        appRecy = view.findViewById(R.id.app_recy);
        PrefUtil prefUtil = App.getPrefUtil();
        boolean flag = prefUtil.getBoolean(PreConfig.DISPLAY_SYSTEM_APP, false);
        sysApp.setChecked(flag);
        checkSelectAll.setOnCheckedChangeListener(this);
        sysApp.setOnCheckedChangeListener(this);
    }

    public void initList() {
        appAdapter = new AppAdapter(this);
        appRecy.setLayoutManager(new MLayoutManager(getActivity(), 4));
        appRecy.setAdapter(appAdapter);
        appAdapter.setOnItemClickListener(this);
        appSwipe.setOnRefreshListener(() -> loading(true));
    }

    @Override
    public void OnLongItenClick(MessageApkContent apkInfo, int position) {
        InfoDialog dialog = new InfoDialog(getContext(), R.style.AlertDialogTheme);
        dialog.setTitle(getString(R.string.please_select_operation));
        String[] items = new String[]{
                getString(R.string.send),
                getString(R.string.backup),
                getString(R.string.info),
                getString(R.string.open),
                getString(R.string.uninstall),
                getString(R.string.generate_ipv6_sharing_link),
                getString(R.string.generate_ipv4_sharing_link),
                getString(R.string.cancel),
        };
        dialog.setItems(items);
        dialog.setOnItemClickListener(arg1 -> {
            switch (arg1) {
                case 0:
                    dataCenterActivity.sendSingleFile(apkInfo);
                    break;
                case 1:
                    new CopFileTask(getContext(), apkInfo.getPath(), Config.FILE_SAVE_PATH + "备份/" + apkInfo.getName()).execute(0);
                    break;
                case 2:
                    FileInfoDialog fileInfoDialog = new FileInfoDialog(getContext(), apkInfo.getPath());
                    fileInfoDialog.show();
                    break;
                case 3:
                    FileUtil.startApp(getContext(), apkInfo.getPackageName());
                    break;
                case 4:
                    FileUtil.uninstallApp(getContext(), apkInfo.getPackageName());
                    break;
                case 5:
                    mUtil.shareFile(false, apkInfo, getContext());
                    break;
                case 6:
                    mUtil.shareFile(true, apkInfo, getContext());
                    break;
                case 7:
                    break;
                default:
                    break;
            }
        });
        dialog.show();
    }

    @Override
    public void OnItemClick(MessageApkContent apkInfo, boolean isSelect, int position) {

    }

    @SuppressLint("SetTextI18n")
    private void loading(boolean refresh) {
        tvCount.setText(getString(R.string.loading));
        appSwipe.setRefreshing(true);
        ThreadUtils.runThread(() -> {
            DeviceDataScanner.scanInstalledApps(getContext(), refresh);
            if (AnyData.apkFileList != null && !AnyData.apkFileList.isEmpty()) {
                if (appAdapter != null) {
                    ThreadUtils.threadUi(() -> {
                        appAdapter.refresh();
                        tvCount.setText(AnyData.apkFileList.size() + " " + getString(R.string.applications));
                        appSwipe.setRefreshing(false);
                    });
                }
            }
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


    // 开启应用

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.app_check_select_all: {
                if (isChecked) {
                    appAdapter.setSelecteByApkinfo(AnyData.apkFileList);
                } else {
                    appAdapter.clearImageSelect();
                }
                break;
            }
            case R.id.app_check_sys_app: {
                PrefUtil prefUtil = App.getPrefUtil();
                prefUtil.saveBoolean(PreConfig.DISPLAY_SYSTEM_APP, isChecked);
                loading(true);
                break;
            }
            default:
                break;
        }
    }

    public List<MessageApkContent> getApkFileList() {
        return AnyData.apkFileList;
    }

    public List<MessageApkContent> getSelectlist() {
        return mSelectlist;
    }

    @Override
    public void clearSelect() {
        if (!mSelectlist.isEmpty() && isVisible()) {
            mSelectlist.clear();
            appAdapter.refresh();
            checkSelectAll.setChecked(false);
        }

    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (n == KeyEvent.KEYCODE_REFRESH || n == KeyEvent.KEYCODE_AVR_INPUT) {
            loading(true);
        }
        return super.onKeyDown(n, keyEvent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(uninstallReceiver);
    }

    class AppInstallUninstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Intent.ACTION_PACKAGE_ADDED:
                        // 应用程序被安装
                        loading(true);
                        break;
                    case Intent.ACTION_PACKAGE_REMOVED:
                        loading(true);
                        break;
                }
            }
        }
    }

}
