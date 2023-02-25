package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLinearLayoutManager;
import com.fgsqw.lanshare.fragment.adapter.SerachAdapter;
import com.fgsqw.lanshare.fragment.interfaces.IChildBaseMethod;
import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.MediaInfo;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FragSearch extends BaseFragment implements IChildBaseMethod, View.OnClickListener {


    public static final String TAG = "FragSearch";
    private View view;
    private EditText searchEdit;
    private Button searchButton;
    private RecyclerView mRecyclerView;

    List<FileInfo> searchs = new ArrayList<>();
    private final List<FileInfo> selectFileList = new LinkedList<>();   // 当前文件列表
    private MLinearLayoutManager mLayoutManager;
    private SerachAdapter serachAdapter;

    public List<FileInfo> getSearchs() {
        return searchs;
    }

    public DataCenterActivity dataCenterActivity;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate:" + this.hashCode());
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView:" + (view == null));
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_search, container, false);
            initView();
        }
        return view;
    }

    public void initView() {
        searchEdit = view.findViewById(R.id.search_edit);
        searchButton = view.findViewById(R.id.search_button);
        mRecyclerView = view.findViewById(R.id.search_recy);
        searchButton.setOnClickListener(this);
        searchEdit.addTextChangedListener(editListener);

        mLayoutManager = new MLinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        serachAdapter = new SerachAdapter(this);
        serachAdapter.setOnClickListener(new SerachAdapter.OnClickListener() {
            @Override
            public void OnClick(int position) {
            }

            @Override
            public void OnLongClick(int position) {
                dialog(position);
            }
        });
        mRecyclerView.setAdapter(serachAdapter);

    }

    private final TextWatcher editListener = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String s1 = searchEdit.getText().toString();
            search(s1);
        }
    };


    @Override
    public void clearSelect() {
        if (selectFileList.size() > 0 && isVisible()) {
            selectFileList.clear();
            serachAdapter.refresh();
        }

    }


    private void dialog(final int position) {
        FileInfo fileInfo = searchs.get(position);
        File file = new File(fileInfo.getPath());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("请选择操作");
        String[] items;
        if (fileInfo.getLength() == 0) {
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
                    dataCenterActivity.sendOneFile(fileInfo);
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

    public void search(String str) {
        for (FileInfo fileInfo : selectFileList) {
            dataCenterActivity.removeSendFile(fileInfo);
        }
        searchs.clear();
        selectFileList.clear();
        if (!StringUtils.isEmpty(str)) {
            List<ApkInfo> apkFileList = FragAppList.apkFileList;
            if (apkFileList != null && !apkFileList.isEmpty()) {
                for (ApkInfo apkInfo : apkFileList) {
                    if (apkInfo.getName().contains(str)) {
                        if (searchs.size() < 100) {
                            searchs.add(apkInfo);
                        } else {
                            break;
                        }
                    }
                }
            }
            List<MediaInfo> mediaInfos = FragMediaList.mediaResult.getAllMedia();
            if (mediaInfos != null && !mediaInfos.isEmpty()) {
                for (MediaInfo image : mediaInfos) {
                    if (image.getName().contains(str)) {
                        if (searchs.size() < 100) {
                            searchs.add(image);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        serachAdapter.refresh();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_button: {
                String str = searchEdit.getText().toString();
                if (!StringUtils.isEmpty(str)) {
                    search(str);
                }
            }
            break;
        }
    }

    public List<FileInfo> getSelectFileList() {
        return selectFileList;
    }
}
