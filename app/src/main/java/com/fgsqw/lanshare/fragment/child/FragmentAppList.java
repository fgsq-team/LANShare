package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.fgsqw.lanshare.fragment.adapter.AppAdapter;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.file.ApkInfo;
import com.fgsqw.lanshare.toast.T;
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

    public final List<ApkInfo> mSelectlist = new LinkedList<>();

    private AppAdapter appAdapter;

    public DataCenterActivity dataCenterActivity;

    private AppInstallUninstallReceiver uninstallReceiver;

    /** 本次会话是否已发起过系统「获取应用列表」权限申请（被拒后不再循环弹） */
    private boolean appListPermissionRequested;

    /** 首屏就绪/后台补图标共用的加载回调 */
    private final FileSearchUtils.AppLoadCallback appLoadCallback = new FileSearchUtils.AppLoadCallback() {
        @Override
        public void onListReady() {
            // 首屏20个图标就绪后立即显示列表，不等全部加载完
            showAppList();
        }

        @Override
        public void onIconsLoaded(int start, int end) {
            // 后台补齐的图标按区间增量刷新，不用整个列表重刷
            ThreadUtils.threadUi(() -> {
                if (appAdapter != null) {
                    appAdapter.notifyItemRangeChanged(start, end - start);
                }
            });
        }
    };

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
    public void OnLongItenClick(ApkInfo apkInfo, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.please_select_operation));
        String[] items;
        items = new String[]{
                getString(R.string.send),
                getString(R.string.backup),
                getString(R.string.info),
                getString(R.string.open),
                getString(R.string.uninstall),
                getString(R.string.generate_ipv6_sharing_link),
                getString(R.string.generate_ipv4_sharing_link),
                getString(R.string.cancel),
        };
        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0:
                    dataCenterActivity.sendSingleFile(apkInfo);
                    break;
                case 1:
                    // 备份至本地
                    new CopFileTask(getContext(), apkInfo.getPath(), Config.FILE_SAVE_PATH + "备份/" + apkInfo.getName()).execute(0);
                    break;
                case 2:
                    FileInfoDialog fileInfoDialog = new FileInfoDialog(getContext(), apkInfo.getPath());
                    fileInfoDialog.show();
                    break;
                case 3:
                    // 打开程序
                    FileUtil.startApp(getContext(), apkInfo.getPackageName());
                    break;
                case 4:
                    // 卸载程序
                    FileUtil.uninstallApp(getContext(), apkInfo.getPackageName());
                    break;
                case 5:
                    mUtil.shareFile(false, apkInfo, getContext());
                    break;
                case 6:
                    mUtil.shareFile(true, apkInfo, getContext());
                    break;
                case 7:
                    //取消
                    break;
                default:
                    break;
            }
        });
        builder.show();
    }

    @Override
    public void OnItemClick(ApkInfo apkInfo, boolean isSelect, int position) {

    }

    @SuppressLint("SetTextI18n")
    private void loading(boolean refresh) {
        if (!refresh && !PermissionsUtils.hasAppListPermission(getContext())) {
            if (appListPermissionRequested) {
                // 已申请过被拒：直接加载能读到的部分（MIUI 未授权只能返回受限列表）
                doLoad(false);
            } else {
                // 点进应用页弹出系统「获取应用列表」授权申请（MIUI/HyperOS）
                appListPermissionRequested = true;
                requestPermissions(new String[]{PermissionsUtils.MIUI_GET_INSTALLED_APPS},
                        PermissionsUtils.REQUEST_APP_LIST_PERMISSION);
            }
            return;
        }
        doLoad(refresh);
    }

    private void doLoad(boolean refresh) {
        if (!refresh && AnyData.apkFileList != null && !AnyData.apkFileList.isEmpty()) {
            // 缓存命中：毫秒级直接展示
            showAppList();
            return;
        }
        tvCount.setText(getString(R.string.loading));
        appSwipe.setRefreshing(true);
        ThreadUtils.runThread(() -> {
            if (refresh) {
                FileSearchUtils.loadApp(getContext(), true, appLoadCallback);
            } else {
                // 未就绪：按需扫描；撞上预加载正在进行会复用其结果，不重复扫
                FileSearchUtils.loadAppIfNeeded(getContext(), appLoadCallback);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_APP_LIST_PERMISSION) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                T.s(R.string.app_list_permission_denied);
            }
            // 授权成功强制重新扫描（未授权期间预加载被跳过，缓存为空或受限）
            doLoad(granted);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showAppList() {
        ThreadUtils.threadUi(() -> {
            if (appAdapter == null) {
                return;
            }
            appAdapter.refresh();
            if (AnyData.apkFileList != null) {
                tvCount.setText(AnyData.apkFileList.size() + " " + getString(R.string.applications));
            }
            appSwipe.setRefreshing(false);
            // 后台图标分批补齐，1.5s 后兜底重刷可见区，防止先绑定的条目图标留白
            appRecy.postDelayed(() -> {
                if (appAdapter != null) {
                    appAdapter.notifyItemRangeChanged(0, appAdapter.getItemCount());
                }
            }, 1500);
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

    public List<ApkInfo> getApkFileList() {
        return AnyData.apkFileList;
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
