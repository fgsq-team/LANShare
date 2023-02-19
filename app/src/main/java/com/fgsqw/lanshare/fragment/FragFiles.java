package com.fgsqw.lanshare.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.fragment.adapter.ViewGroupAdapter;
import com.fgsqw.lanshare.fragment.child.*;
import com.fgsqw.lanshare.fragment.minterface.ChildBaseMethod;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class FragFiles extends BaseFragment implements ViewPager.OnPageChangeListener, ChildBaseMethod {

    View view;

    private final List<Fragment> fragments = new ArrayList<>();
    FragAppList fragAppList;
    FragFileList fragFileList;
    FragMediaList fragMediaList;
    FragTest fragTest;
    FragSearch fragSearch;

    String[] tabTitles = {"应用", "媒体", "文件", "待添加"};

    TabLayout tabFiles;
    ViewPager viewPager;

    int currentPosition;

    public List fileSelects = new LinkedList<>();

    public FragFiles() {
        fragAppList = new FragAppList();
        fragments.add(fragAppList);
        fragMediaList = new FragMediaList();
        fragments.add(fragMediaList);
        fragFileList = new FragFileList();
        fragments.add(fragFileList);

//        fragTest = new FragTest();
//        fragments.add(fragTest);

        fragSearch = new FragSearch();
        fragments.add(fragSearch);

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_files, container, false);
            initView();
            initFragment();
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void initView() {
        tabFiles = view.findViewById(R.id.tab_files);
        viewPager = view.findViewById(R.id.pag_files);
    }

    public void initFragment() {
        ViewGroupAdapter mAdapter = new ViewGroupAdapter(getActivity().getSupportFragmentManager(), tabTitles, fragments);
        viewPager.setAdapter(mAdapter);
        viewPager.addOnPageChangeListener(this);
        tabFiles.setTabMode(TabLayout.MODE_FIXED);
        tabFiles.setupWithViewPager(viewPager);
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        currentPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        BaseFragment fragment = (BaseFragment) fragments.get(viewPager.getCurrentItem());
        return fragment.onKeyDown(n, keyEvent);
    }

    public List getFileSelects() {
        return fileSelects;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void clearSelect() {
        for (Fragment fragment : fragments) {
            if (fragment instanceof ChildBaseMethod) {
                ((ChildBaseMethod) fragment).clearSelect();
            }
        }
    }
}
















