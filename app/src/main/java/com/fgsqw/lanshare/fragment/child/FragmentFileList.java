package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLinearLayoutManager;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.dialog.EditTextDialog;
import com.fgsqw.lanshare.dialog.FileInfoDialog;
import com.fgsqw.lanshare.dialog.SelectStorageDialog;
import com.fgsqw.lanshare.fragment.adapter.FileAdapter;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageUriContent;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author fgsq
 * @comments 文件选择界面
 * @date 2024/7/13 10:51
 */
public class FragmentFileList extends BaseFragment implements View.OnClickListener {

    private View view;
    private TextView mPathTv;
    private TextView selectStorageTv;
    private SwipeRefreshLayout mSwipe;
    private RecyclerView mRecyclerView;
    private LinearLayout mSelectStorage;
    private FileAdapter mFileAdapter;
    private MLinearLayoutManager mLayoutManager;
    // 保存上一级item 位置
    private final List<Integer> mSign = new ArrayList<>();
    // 当前文件所有列表
    private List<MessageFileContent> fileList = new ArrayList<>();
    // 当前文件列表
    private final List<MessageFileContent> selectFileList = new LinkedList<>();
    // 当前文件夹路径
    private MessageFileContent currentDirectory;
    public DataCenterActivity dataCenterActivity;
    private boolean showHiddenFiles = false;
    private PrefUtil prefUtil;


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
            prefUtil = App.getPrefUtil();
            initView();
            initList();
            if (Config.LAST_FILE_PATH) {
                String text = prefUtil.getString(PreConfig.LAST_FILE_PATH);
                if (!StringUtils.isEmpty(text)) {
                    JSONObject jsonObject = JSONObject.parseObject(text);
                    String path = jsonObject.getString("path");
                    ArrayList<Integer> personList = (ArrayList<Integer>) JSON.parseArray(jsonObject.getString("sign"), Integer.class);
                    mSign.addAll(personList);
                    MessageFileContent fileInfo = new MessageFileContent();
                    fileInfo.setName(fileInfo.getName());
                    fileInfo.setPath(path);
                    initFileList(fileInfo);
                } else {
                    // 显示根目录
                    initFileList(null);
                }
            } else {
                initFileList(null);
            }
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;

    }

    public void initView() {
        mPathTv = view.findViewById(R.id.file_view_text);
        selectStorageTv = view.findViewById(R.id.file_view_select_storage_text);
        mRecyclerView = view.findViewById(R.id.file_view_recy);
        mSelectStorage = view.findViewById(R.id.file_select_storage);
        mSwipe = view.findViewById(R.id.file_view_swip);
        mSwipe.setOnRefreshListener(() -> {
            initFileList(currentDirectory);
            mSwipe.setRefreshing(false);
        });
        mSelectStorage.setOnClickListener(this);
    }

    private void savePath(MessageFileContent fileInfo) {
        if (!Config.LAST_FILE_PATH) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("path", fileInfo.getPath());
        jsonObject.put("sign", mSign);
        prefUtil.saveString(PreConfig.LAST_FILE_PATH, jsonObject.toString());
    }

    public void initList() {
        mLayoutManager = new MLinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mFileAdapter = new FileAdapter(this);
        mFileAdapter.setOnClickListener(new FileAdapter.OnClickListener() {
            @Override
            public void OnClick(int position) {//列表点击事件

                if (position == 0) {                              //列表第零位点击表示返回
                    if (!mSign.isEmpty()) {
                        upper();//文件列表返回上一级
                    }
                } else {
                    MessageFileContent fileSource = fileList.get(position);
                    if (fileSource.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {                 //点击的文件如果是文件夹的话
                        int i = ((LinearLayoutManager) Objects.requireNonNull(mRecyclerView.getLayoutManager()))
                                .findFirstVisibleItemPosition();
                        //获取当前屏幕第一个显示的item
                        mSign.add(i);
                        initFileList(fileSource);
                        savePath(fileSource);
                    } else/* if (fileSource.isFile())*/ {                      //点击的文件如果是文件的话
//                        if (!mFile.isDirectory()) {
                        dialog(fileSource);
//                        }
                    }/* else if (mFile.isAbsolute()) {
                        int i = ((LinearLayoutManager) Objects.requireNonNull(mRecyclerView.getLayoutManager()))
                                .findFirstVisibleItemPosition();//获取当前屏幕第一个显示的item
                        mSign.add(i);
                        initFileList(mFile);
                    }*/
                    requestFocusFirst();
                }

            }

            @Override
            public void OnLongClick(int position) {//列表长按时间
                if (position != 0) {
                    MessageFileContent fileSource = fileList.get(position);
                    dialog(fileSource);
                }

            }
        });
        mRecyclerView.setAdapter(mFileAdapter);
    }

    // 文件列表返回上一级
    private void upper() {
        MessageFolderContent fileSource = new MessageFolderContent();
        fileSource.setPath(new File(currentDirectory.getPath()).getParent());
        // 返回上一级
        initFileList(fileSource);
        mLayoutManager.scrollToPositionWithOffset(mSign.get(mSign.size() - 1), 0);
        mSign.remove(mSign.size() - 1);
        requestFocusFirst();
        savePath(fileSource);
    }


    private void requestFocusFirst() {
        ThreadUtils.runThread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {
            }
            ThreadUtils.threadUi(() -> {
                int firstVisibleItemPosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition);
                if (holder != null) {
                    holder.itemView.requestFocus();
                }
            });
        });

    }


    @SuppressLint("SetTextI18n")
    void initFileList(MessageFileContent f) {
        // 如果File为null则默认为跟目录
        if (f == null) {
            f = new MessageFolderContent();
            f.setPath(PermissionsUtils.ROOT_PATH);
        }
        try {
            int fileSortMethod = prefUtil.getInt(PreConfig.FILE_SORT_METHOD, 0);
            List<MessageFileContent> fileList = FileSearchUtils.getFileList(f, showHiddenFiles, fileSortMethod, dataCenterActivity);
            if (!fileList.isEmpty()) {
                this.fileList = fileList;
                mFileAdapter.refresh();
                currentDirectory = f;
                mPathTv.setText(f.getPath());
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            T.s(e.getMessage());
            LLog.error("error", e);
        }
    }


    /**
     * 删除文件
     *
     * @param files 文件列表
     */
    private void deleteFile(List<MessageFileContent> files) {
        AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(dataCenterActivity);
        normalDialog.setTitle(R.string.warning);
        normalDialog.setMessage(R.string.file_deletion_is_irreversible);
        normalDialog.setPositiveButton(getString(R.string.confirm),
                (dialog, which) -> {
                    for (MessageFileContent fileSource : files) {
                        if (fileSource instanceof MessageUriContent) {
                            MessageUriContent uriFileInfo = (MessageUriContent) fileSource;
                            DocumentFile documentFile = DocumentFile.fromTreeUri(getContext(), uriFileInfo.getUri());
                            if (documentFile != null && documentFile.delete()) {
                                initFileList(currentDirectory);
                                T.s((R.string.file_deletion_successful));
                            } else {
                                T.s((R.string.file_deletion_failed));
                            }
                        } else {
                            if (FileUtil.deleteFile(new File(fileSource.getPath()))) {
                                initFileList(currentDirectory);
                            } else {
                                T.s(getString(R.string.file_deletion_failed_operation_terminated) + fileSource.getPath());
                                return;
                            }
                            T.s((R.string.file_deletion_successful));
                        }
                    }
                });
        normalDialog.setNegativeButton(getString(R.string.cancel),
                (dialog, which) -> {
                    dialog.dismiss();
                });
        // 显示
        normalDialog.show();

    }

    private void showSelectStorageDialog() {
        SelectStorageDialog deviceSelectDialog = new SelectStorageDialog(dataCenterActivity);
        deviceSelectDialog.setOnStorageSelect(storageInfo -> {
            T.s(storageInfo.getPath());
            MessageFolderContent fileSource = new MessageFolderContent();
            fileSource.setPath(storageInfo.getPath() + "/");
            initFileList(fileSource);
            selectStorageTv.setText(StringUtils.isEmpty(storageInfo.getName()) ? "" : storageInfo.getName());
            deviceSelectDialog.dismiss();
        });
        deviceSelectDialog.show();
    }

    private void searchFile(String path) {
        EditTextDialog dialog = new EditTextDialog(getContext(), false, "搜索文件", "", "请输入搜索内容");
        dialog.setOnClickListener((ok, str) -> {
            dataCenterActivity.searchFile(path, str);
        });
        dialog.show();
    }


    private void dialog(MessageFileContent fileSource) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setTitle(R.string.please_select_operation);
        String[] items;
        if (fileSource.getFileType() == MessageFileContent.FILE_TYPE_FILE && fileSource.getLength() == 0) {
            T.s((R.string.file_size_is_zero));
            return;
        }
        if (!(fileSource.getFileType() == MessageFileContent.FILE_TYPE_URI)) {
            File file = new File(fileSource.getPath());
            if (!file.canRead()) {
                T.s((R.string.file_cannot_be_read));
                return;
            }
        }
        if (fileSource.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
            items = new String[]{
                    getString(R.string.send),
                    getString(R.string.open),
                    getString(R.string.search),
                    getString(R.string.delete),
                    getString(R.string.cancel),
            };
            // 绑定选项和点击事件
            builder.setItems(items, (arg0, arg1) -> {
                switch (arg1) {
                    case 0: {
                        dataCenterActivity.sendSingleFile(fileSource);
                        break;
                    }
                    case 1: {
                        if (fileSource instanceof MessageUriContent) {
                            MessageUriContent uriFileInfo = (MessageUriContent) fileSource;
                            if (uriFileInfo.isFile()) {
                                FileUtil.openFile((Activity) getContext(), uriFileInfo.getUri(), fileSource.getName());
                            } else {
                                initFileList(fileSource);
                            }
                        } else {
                            FileUtil.openFile((Activity) getContext(), new File(fileSource.getPath()));
                        }
                        break;
                    }
                    case 2: {
                        searchFile(fileSource.getPath());
                        break;
                    }
                    case 3: {
                        if (!selectFileList.isEmpty()) {
                            deleteFile(selectFileList);
                        } else {
                            deleteFile(Collections.singletonList(fileSource));
                        }
                        break;
                    }
                    default:
                        break;
                }
                arg0.dismiss();
            });
        } else {
            items = new String[]{
                    getString(R.string.send),
                    getString(R.string.open),
                    getString(R.string.info),
                    getString(R.string.delete),
                    getString(R.string.generate_ipv6_sharing_link),
                    getString(R.string.generate_ipv4_sharing_link),
                    getString(R.string.cancel),
            };
            // 绑定选项和点击事件
            builder.setItems(items, (arg0, arg1) -> {
                switch (arg1) {
                    case 0: {
                        dataCenterActivity.sendSingleFile(fileSource);
                        break;
                    }
                    case 1: {
                        FileUtil.openFile((Activity) getContext(), new File(fileSource.getPath()));
                        break;
                    }
                    case 2: {
                        FileInfoDialog fileInfoDialog = new FileInfoDialog(getContext(), fileSource.getPath());
                        fileInfoDialog.show();
                        break;
                    }
                    case 3: {
                        if (!selectFileList.isEmpty()) {
                            deleteFile(selectFileList);
                        } else {
                            deleteFile(Collections.singletonList(fileSource));
                        }
                        break;
                    }
                    case 4: {
                        mUtil.shareFile(false, fileSource, getContext());
                        break;
                    }
                    case 5: {
                        mUtil.shareFile(true, fileSource, getContext());
                        break;
                    }
                    default:
                        break;
                }
                arg0.dismiss();
            });
        }
        builder.show();
    }


    public List<MessageFileContent> getFileList() {
        return fileList;
    }

    public List<MessageFileContent> getSelectFileList() {
        return selectFileList;
    }

    @Override
    public boolean onBack() {
        if (!mSign.isEmpty()) {
            upper();
            return true;
        }
        return false;
    }

    @Override
    public void clearSelect() {
        if (!selectFileList.isEmpty() && isVisible()) {
            selectFileList.clear();
            mFileAdapter.refresh();
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.file_select_storage) {
            showSelectStorageDialog();
        }
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == LCmd.SERVICE_SYNC_SORT) {          // 是否接收文件弹窗
            initFileList(currentDirectory);
        }
    }
}
