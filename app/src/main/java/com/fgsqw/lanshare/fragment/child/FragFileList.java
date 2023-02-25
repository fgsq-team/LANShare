package com.fgsqw.lanshare.fragment.child;

import static android.graphics.BitmapFactory.decodeResource;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLinearLayoutManager;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.fragment.adapter.FileAdapter;
import com.fgsqw.lanshare.fragment.interfaces.IChildBaseMethod;
import com.fgsqw.lanshare.pojo.FileSource;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.FIleSerachUtils;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.PermissionsUtils;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.VersionUtils;
import com.fgsqw.lanshare.utils.mUtil;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FragFileList extends BaseFragment implements IChildBaseMethod {

    private ViewPager vp;
    private View view;

    private TextView mPathTv;
    private SwipeRefreshLayout mSwipe;
    private RecyclerView mRecyclerView;
    private FileAdapter mFileAdapter;
    private MLinearLayoutManager mLayoutManager;

    private final List<Integer> mSign = new ArrayList<>();      // 保存上一级item 位置
    private final List<FileSource> fileList = new ArrayList<>();  // 当前文件所有列表
    private final List<FileSource> paths = new ArrayList<>();   // 当前文件列表

    private final List<FileSource> selectFileList = new LinkedList<>();   // 当前文件列表

    private File currentDirectory;    // 当前文件夹路径

    public DataCenterActivity dataCenterActivity;

    private boolean showHiddenFiles = false;


    @Override
    public void onStart() {
        super.onStart();
        boolean aBoolean = App.getPrefUtil().getBoolean(PreConfig.SHOW_HIDDEN_FILES, false);
        if (aBoolean != showHiddenFiles) {
            showHiddenFiles = aBoolean;
            initFileList(currentDirectory);
        }
    }

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        showHiddenFiles = App.getPrefUtil().getBoolean(PreConfig.SHOW_HIDDEN_FILES, false);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_file, container, false);
            initView();
            initList();
            initFileList(Environment.getExternalStorageDirectory());//显示根目录
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;

    }

    public void initView() {
        mPathTv = view.findViewById(R.id.file_view_text);
        mRecyclerView = view.findViewById(R.id.file_view_recy);
        mSwipe = view.findViewById(R.id.file_view_swip);

        mSwipe.setOnRefreshListener(() -> {
            initFileList(currentDirectory);
            mSwipe.setRefreshing(false);
        });
    }


    public void initList() {
        mLayoutManager = new MLinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mFileAdapter = new FileAdapter(this);
        mFileAdapter.setOnClickListener(new FileAdapter.OnClickListener() {
            @Override
            public void OnClick(int position) {//列表点击事件

                if (position == 0) {                              //列表第零位点击表示返回
                    if (mSign.size() != 0) {
                        upper();//文件列表返回上一级
                    }
                } else {
                    File mFile = new File(fileList.get(position).getPath());
                    if (mFile.isDirectory()) {                 //点击的文件如果是文件夹的话
                        int i = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();//获取当前屏幕第一个显示的item
                        mSign.add(i);
                        initFileList(mFile);
                    } else if (mFile.isFile()) {                      //点击的文件如果是文件的话
                        if (!mFile.isDirectory()) {
                            dialog(position);
                        }
                    } else if (mFile.isAbsolute()) {
                        int i = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();//获取当前屏幕第一个显示的item
                        mSign.add(i);
                        initFileList(mFile);
                    }
                }
            }

            @Override
            public void OnLongClick(int position) {//列表长按时间
                if (position != 0) {
                    final File mFile = new File(fileList.get(position).getPath());
                    if (!mFile.isDirectory()) {
                        dialog(position);
                    } else {
                        dialogFolder(position);

                    }
                }

            }
        });
        mRecyclerView.setAdapter(mFileAdapter);

    }

    //文件列表返回上一级
    private void upper() {
        initFileList(currentDirectory.getParentFile());//返回上一级
        mLayoutManager.scrollToPositionWithOffset(mSign.get(mSign.size() - 1), 0);
        mSign.remove(mSign.size() - 1);
    }


    @SuppressLint("SetTextI18n")
    void initFileList(File f) {
        // 如果File为null则默认为跟目录
        if (f == null) {
            f = new File("/");
        }
        if (VersionUtils.isAndroid11() && PermissionsUtils.isAndroidData(f.getPath())) {
            boolean isGet = XXPermissions.isGranted(requireContext(), Permission.MANAGE_EXTERNAL_STORAGE);
            //已有权限则返回
            if (!isGet) {
                XXPermissions.with(requireContext())
                        // 申请单个权限
                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                        // 设置不触发错误检测机制（局部设置）
                        .unchecked()
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                T.s("成功");
                            }

                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                if (never) {
                                    //如果是被永久拒绝就跳转到应用权限系统设置页面
                                    XXPermissions.startPermissionActivity(requireActivity(), permissions);
                                } else {
                                }

                            }
                        });
                return;
            }

            // 保存当前路径
            currentDirectory = f;
            // 显示当前路径到界面
            mPathTv.setText("    " + f.getPath());
            fileList.clear();
            Resources res = getResources();
            FileSource fileSource = new FileSource();
            fileSource.setName("...");
            fileSource.setPreView(decodeResource(res, R.drawable.ic_folder_upload));
            fileSource.setIsPreView(false);
            fileList.add(fileSource);
            List<String> packageNames = FIleSerachUtils.getPackageNames(getContext());
            for (String packageName : packageNames) {
                File file = new File(PermissionsUtils.ANDROID_DATA + "/" + packageName);
                if (file.exists()) {
                    String name = file.getName();
                    Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                    fileSource = new FileSource();
                    fileSource.setName(mUtil.StringSize(name, 20));
                    fileSource.setPath(file.getPath());
                    fileSource.setPreView(bmp);
                    fileSource.setIsPreView(false);
                    fileSource.setTime(file.lastModified());
                    fileSource.setFile(false);
                    fileList.add(fileSource);
                }
            }
            mFileAdapter.refresh();
        } else if (PermissionsUtils.isSubAndroidData(f.getPath())) {
            Uri uri = PermissionsUtils.path2Uri(f.getPath());
            //获取权限,没有权限返回null有权限返回授权uri字符串
            String existsPermission = PermissionsUtils.existsGrantedUriPermission(uri, getContext());
            if (existsPermission == null) {
                PermissionsUtils.goApplyUriPermissionPage(uri, dataCenterActivity);
                return;
            }
            Uri targetUri = Uri.parse(existsPermission + uri.toString()
                    .replaceFirst(PermissionsUtils.URI_PERMISSION_REQUEST_COMPLETE_PREFIX, ""));
            //Mtools.log(targetUri);
            DocumentFile rootDocumentFile = DocumentFile.fromSingleUri(getContext(), targetUri);
            Objects.requireNonNull(rootDocumentFile, "rootDocumentFile is null");
            //创建一个 DocumentFile表示以给定的 Uri根的文档树。其实就是获取子目录的权限
            DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), targetUri);
            Objects.requireNonNull(pickedDir, "pickedDir is null");
            DocumentFile[] documentFiles = pickedDir.listFiles();
            currentDirectory = f;
            // 显示当前路径到界面
            mPathTv.setText("    " + f.getPath());
            fileList.clear();
            paths.clear();
            Resources res = getResources();
            FileSource fileSource = new FileSource();
            fileSource.setName("...");
            fileSource.setPreView(decodeResource(res, R.drawable.ic_folder_upload));
            fileSource.setIsPreView(false);
            fileList.add(fileSource);
            for (DocumentFile documentFile : documentFiles) {
                String name = documentFile.getName();
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                if (documentFile.isDirectory()) {
                    if (!showHiddenFiles) {
                        if (name.startsWith(".")) {
                            continue;
                        }
                    }
                    Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                    fileSource = new FileSource();
                    fileSource.setName(mUtil.StringSize(name, 20));
                    fileSource.setPath(f.getPath() + "/" + name);
                    fileSource.setPreView(bmp);
                    fileSource.setIsPreView(false);
                    fileSource.setTime(documentFile.lastModified());
                    fileSource.setFile(false);

                    fileList.add(fileSource);
                } else if (documentFile.isFile()) {
                    fileSource = new FileSource();
                    fileSource.setName(name);
                    fileSource.setPath(f.getPath() + "/" + name);
                    fileSource.setPreView(decodeResource(res, R.drawable.ic_file_file));
                    fileSource.setIsPreView(false);
                    fileSource.setTime(documentFile.lastModified());
                    fileSource.setLength(documentFile.length());
                    fileSource.setFile(true);
                    paths.add(fileSource);
                }
            }
            fileList.addAll(paths);
            mFileAdapter.refresh();
        } else if (f.isDirectory()) {  // 如果是文件夹

            if (f.canRead()) {  // 如果能读取
                // 保存当前路径
                currentDirectory = f;
                // 显示当前路径到界面
                mPathTv.setText("    " + f.getPath());
                // 在文件列表视图添加子文件或目录
                // 当前文件列表
                File[] files = f.listFiles();
                // 排序
                Arrays.sort(files);
                fileList.clear();
                paths.clear();
                Resources sres = getResources();

                FileSource fileSource = new FileSource();
                fileSource.setName("...");
                fileSource.setPreView(decodeResource(sres, R.drawable.ic_folder_upload));
                fileSource.setIsPreView(false);

                fileList.add(fileSource);

                for (File file : files) {
                    // 如果是文件夹
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Resources res = getResources();
                        Bitmap bmp = decodeResource(res, R.drawable.ic_folder);
                        fileSource = new FileSource();
                        fileSource.setName(mUtil.StringSize(name, 20));
                        fileSource.setPath(file.getPath());
                        fileSource.setPreView(bmp);
                        fileSource.setIsPreView(false);
                        fileSource.setTime(file.lastModified());
                        fileSource.setFile(false);

                        fileList.add(fileSource);
                        // 如果是文件
                    } else if (file.isFile()) {
                        String name = file.getName();
                        if (!showHiddenFiles) {
                            if (name.startsWith(".")) {
                                continue;
                            }
                        }
                        Resources res = getResources();
                        Bitmap bmp = null;
                        String suffixName = name.substring(name.lastIndexOf(".") + 1);
                        boolean isPreView = false;
                        if (suffixName.equalsIgnoreCase("txt")) {
                            bmp = decodeResource(res, R.drawable.ic_txts_file);
                        } else if (suffixName.equalsIgnoreCase("jpg")//图片及视频文件
                                || suffixName.equalsIgnoreCase("png")
                                || suffixName.equalsIgnoreCase("jpeg")
                                || suffixName.equalsIgnoreCase("gif")
                                || suffixName.equalsIgnoreCase("mp4")
                        ) {
                            isPreView = true;
                        } else if (suffixName.equalsIgnoreCase("zip")) {//压缩包文件
                            bmp = decodeResource(res, R.drawable.ic_zip_file);
                        } else if (suffixName.equalsIgnoreCase("apk")) {//apk文件
                           /* Drawable drawable = FileUtil.getApkIcon(getContext(), file.getPath());
                            if (drawable != null) {
                                bmp = ImageUtils.drawableToBitmap(drawable);
                            } else {
                                bmp = decodeResource(res, R.drawable.ic_null_android_file);
                            }*/
                            bmp = decodeResource(res, R.drawable.ic_null_android_file);
                        } else if (suffixName.equalsIgnoreCase("java")) {//java文件
                            bmp = decodeResource(res, R.drawable.ic_coder_file);
                        } else {
                            bmp = decodeResource(res, R.drawable.ic_file_file);
                        }

                        fileSource = new FileSource();
                        fileSource.setName(name);
                        fileSource.setPath(file.getPath());
                        fileSource.setPreView(bmp);
                        fileSource.setIsPreView(isPreView);
                        fileSource.setTime(file.lastModified());
                        fileSource.setLength(file.length());
                        fileSource.setFile(true);

                        paths.add(fileSource);
                    }
                }
                fileList.addAll(paths);
                mFileAdapter.refresh();
            } else {
                // 不能读取文件夹
                T.s("此文件夹不能被读取");
            }
        }

    }

    private void dialogFolder(final int position) {
        FileSource fileSource = fileList.get(position);
        File file = new File(fileSource.getPath());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("请选择操作");
        String[] items;

        if (file.canRead()) {
            items = new String[]{"发送", "取消",};
        } else {
            T.s("文件夹不能读取");
            return;
        }

        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0: {
                    dataCenterActivity.sendOneFile(fileSource);
                    break;
                }
                case 1: {
                    break;
                }
            }
            arg0.dismiss();
        });
        builder.show();
    }


    private void dialog(final int position) {
        FileSource fileSource = fileList.get(position);
        File file = new File(fileSource.getPath());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("请选择操作");
        String[] items;
        if (fileSource.getLength() == 0) {
            T.s("文件大小为0");
            return;
        }
        if (file.canRead()) {
            items = new String[]{"发送", "打开", "取消",};
        } else {
            T.s("文件不能读取");
            return;
        }

        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0: {
                    dataCenterActivity.sendOneFile(fileSource);
                    break;
                }
                case 1: {
                    FileUtil.openFile((Activity) getContext(), file);
                    break;
                }
                case 2: {

                    break;
                }

            }
            arg0.dismiss();
        });
        builder.show();
    }

    public List<FileSource> getFileList() {
        return fileList;
    }

    public List<FileSource> getSelectFileList() {
        return selectFileList;
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (mSign.size() != 0) {
            upper();
            return true;
        }
        return false;
    }

    @Override
    public void clearSelect() {
        if (selectFileList.size() > 0 && isVisible()) {
            selectFileList.clear();
            mFileAdapter.refresh();
        }

    }
}