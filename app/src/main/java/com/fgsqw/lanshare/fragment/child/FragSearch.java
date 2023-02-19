package com.fgsqw.lanshare.fragment.child;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLinearLayoutManager;
import com.fgsqw.lanshare.fragment.adapter.FileAdapter;
import com.fgsqw.lanshare.fragment.adapter.SerachAdapter;
import com.fgsqw.lanshare.fragment.minterface.ChildBaseMethod;
import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.FileSource;
import com.fgsqw.lanshare.pojo.MediaInfo;
import com.fgsqw.lanshare.pojo.PhotoFolder;
import com.fgsqw.lanshare.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class FragSearch extends BaseFragment implements ChildBaseMethod, View.OnClickListener {


    public static final String TAG = "FragSearch";
    private View view;
    private EditText searchEdit;
    private TextView searchButton;
    private RecyclerView mRecyclerView;

    List<FileInfo> searchs = new ArrayList<>();
    private final List<FileInfo> selectFileList = new LinkedList<>();   // 当前文件列表
    private MLinearLayoutManager mLayoutManager;
    private SerachAdapter serachAdapter;

    public List<FileInfo> getSearchs() {
        return searchs;
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

        mLayoutManager = new MLinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        serachAdapter = new SerachAdapter(this);
        mRecyclerView.setAdapter(serachAdapter);

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

    }


    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");

    }


    @Override
    public void clearSelect() {

    }

    public void search(String str) {
        searchs.clear();
        List<ApkInfo> apkFileList = FragAppList.apkFileList;
        if (apkFileList != null && !apkFileList.isEmpty()) {
            for (ApkInfo apkInfo : apkFileList) {
                if (apkInfo.getName().contains(str)) {
                    searchs.add(apkInfo);
                }
            }
        }
        List<PhotoFolder> mFolders = FragMediaList.mFolders;
        if (mFolders != null && !mFolders.isEmpty()) {
            for (PhotoFolder mFolder : mFolders) {
                List<MediaInfo> images = mFolder.getImages();
                if (images != null && !images.isEmpty()) {
                    for (MediaInfo image : images) {
                        if (image.getName().contains(str)) {
                            searchs.add(image);
                        }
                    }
                }
            }
        }
        serachAdapter.refresh();
    }

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
