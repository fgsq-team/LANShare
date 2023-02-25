package com.fgsqw.lanshare.fragment.child;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.fragment.interfaces.IChildBaseMethod;

public class FragVideoList extends BaseFragment implements IChildBaseMethod {


    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_video, container, false);
            initView();
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void initView() {

    }

    @Override
    public void clearSelect() {

    }
}