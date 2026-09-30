package com.fgsqw.lanshare.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;


import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.fragment.adapter.ViewGroupAdapter;
import com.fgsqw.lanshare.fragment.child.*;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class FragmentFiles extends BaseFragment implements ViewPager.OnPageChangeListener {
    View view;
    private final List<Fragment> fragments = new ArrayList<>();
    FragmentAppList fragmentAppList;
    FragmentFileList fragmentFileList;
    FragmentMediaList fragmentMediaList;
//    FragmentTest fragmentTest;
    FragmentSearch fragmentSearch;
    FragmentMusic fragmentMusic;

    TabLayout tabFiles;
    ViewPager viewPager;
    ViewGroupAdapter viewGroupAdapter;
    int currentPosition;

    public DataCenterActivity dataCenterActivity;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    public FragmentFiles() {
        fragmentAppList = new FragmentAppList();
        fragments.add(fragmentAppList);
        fragmentMediaList = new FragmentMediaList();
        fragments.add(fragmentMediaList);
        fragmentMusic = new FragmentMusic();
        fragments.add(fragmentMusic);
        fragmentFileList = new FragmentFileList();
        fragments.add(fragmentFileList);
        fragmentSearch = new FragmentSearch();
        fragments.add(fragmentSearch);
       /* fragTest = new FragTest();
        fragments.add(fragTest);*/
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
        dataCenterActivity.setShowUpdateApps(currentPosition == 0);
    }

    public void initFragment() {
        String[] tabTitles = {
                getString(R.string.application),
                getString(R.string.media),
                getString(R.string.audio),
                getString(R.string.file),
                getString(R.string.search),

                /*, "其他"*/
        };

        viewGroupAdapter = new ViewGroupAdapter(Objects.requireNonNull(getActivity())
                .getSupportFragmentManager(), tabTitles, fragments);
        viewPager.setAdapter(viewGroupAdapter);
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
        dataCenterActivity.setShowUpdateApps(position == 0);
        dataCenterActivity.setSortFileTypeMenuVisible(position == 3);
        dataCenterActivity.setSearchFileTypesVisible(position == 4);
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    public void addPosition() {
        currentPosition++;
        if (currentPosition > fragments.size() - 1) {
            currentPosition = 0;
        }
        TabLayout.Tab tab = tabFiles.getTabAt(currentPosition); // 获取要切换到的标签页索引
        tab.select(); // 选中指定索引的标签页
    }

    public void subPosition() {
        currentPosition--;
        if (currentPosition < 0) {
            currentPosition = fragments.size() - 1;
        }
        TabLayout.Tab tab = tabFiles.getTabAt(currentPosition); // 获取要切换到的标签页索引
        if (tab != null) {
            tab.select(); // 选中指定索引的标签页
        }
    }

    public void setPosition(int pos) {
        currentPosition = pos;
        TabLayout.Tab tab = tabFiles.getTabAt(currentPosition);
        if (tab != null) {
            tab.select();
        }
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (n == KeyEvent.KEYCODE_PAGE_UP) {
            subPosition();
            return true;
        } else if (n == KeyEvent.KEYCODE_PAGE_DOWN) {
            addPosition();
            return true;
        }
        BaseFragment fragment = (BaseFragment) fragments.get(viewPager.getCurrentItem());
        return fragment.onKeyDown(n, keyEvent);
    }

    public boolean onBack() {
        BaseFragment fragment = (BaseFragment) fragments.get(viewPager.getCurrentItem());
        return fragment.onBack();
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void clearSelect() {
        for (Fragment fragment : fragments) {
            ((BaseFragment) fragment).clearSelect();
        }
    }

    @Override
    public void handleMessage(Message message) {
        for (Fragment fragment : fragments) {
            ((BaseFragment) fragment).handleMessage(message);
        }
    }

    public void searchFile(String path, String str) {
        setPosition(4);
        fragmentSearch.searchFile(true, new File(path), str);
    }
}
















