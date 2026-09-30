package com.fgsqw.lanshare.base.view;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import com.fgsqw.lanshare.utils.LLog;

public class MLayoutManager extends GridLayoutManager {
    //... constructor
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            Log.e("probe", "meet app IOOBE in RecyclerView");
            LLog.error("onLayoutChildren", e);
        }
    }

    public MLayoutManager(Context context, int s) {
        super(context, s);
    }
}

