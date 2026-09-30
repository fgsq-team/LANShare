package com.fgsqw.lanshare.fragment.adapter;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

// 适配器
public class ViewGroupAdapter extends FragmentStatePagerAdapter {

    String[] mTitles;
    List<Fragment> mFragments;

    public ViewGroupAdapter(FragmentManager fm, String[] titles, List<Fragment> fragments) {
        super(fm);
        mTitles = titles;
        mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

}
