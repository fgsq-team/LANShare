package com.fgsqw.lanshare.base;

import android.content.Context;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.view.KeyEvent;

public class BaseFragment extends Fragment {
    private Context context;

    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        return false;
    }

    public boolean onBack(){
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


    public void handleMessage(Message message) {
        // to do
    }

    public void clearSelect() {
    }

}
