package com.fgsqw.lanshare.base.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

public class MLinearLayoutManager extends LinearLayoutManager {
    //... constructor
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            Log.e("probe", "meet app IOOBE in RecyclerView");
        }
    }
	public MLinearLayoutManager(Context context)
	{
		super(context);
	}
}
