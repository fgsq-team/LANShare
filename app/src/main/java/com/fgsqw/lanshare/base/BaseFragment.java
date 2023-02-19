package com.fgsqw.lanshare.base;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

public class BaseFragment extends Fragment {
    private Context context;

    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public Context getContext() {
        Context supeContext = super.getContext();
        if (supeContext == null) {
            supeContext = this.context;
        }
        return supeContext;
    }


}
