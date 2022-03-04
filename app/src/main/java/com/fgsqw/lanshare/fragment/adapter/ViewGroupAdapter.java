package com.fgsqw.lanshare.fragment.adapter;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by lenovo on 2016/3/8.
 */

//适配器
public class ViewGroupAdapter extends FragmentStatePagerAdapter{

    String[] mTitles;
    List<Fragment> mFragments;

    public ViewGroupAdapter(FragmentManager fm, String[] titles, List<Fragment> fragments){
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
