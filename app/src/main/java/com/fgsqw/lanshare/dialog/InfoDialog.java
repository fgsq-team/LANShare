package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.utils.StringUtils;

public class InfoDialog extends BaseDialog implements View.OnClickListener {

    private LinearLayout infoLayoutLeft;
    private LinearLayout infoLayoutRight;
    private TextView infoTextLeft;
    private TextView infoTextRight;
    private TextView infoText;
    private TextView infoTips;
    private OnClickListener onClickListener;
    private String text;
    private String title;
    private String leftButtonText;
    private String rightButtonText;

    public InfoDialog(@NonNull Context context) {
        super(context, R.style.AlertDialogTheme);
    }

    public InfoDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected InfoDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String text) {
        this.title = text;
    }

    public void setLeftButtonText(String text) {
        leftButtonText = text;
    }

    public void setRightButtonText(String text) {
        rightButtonText = text;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_dialog);
        infoLayoutLeft = findViewById(R.id.info_layout_left);
        infoLayoutRight = findViewById(R.id.info_layout_right);
        infoTextLeft = findViewById(R.id.info_text_left);
        infoTextRight = findViewById(R.id.info_text_right);
        infoText = findViewById(R.id.info_text);
        infoTips = findViewById(R.id.info_tips);
        infoLayoutLeft.requestFocus();
        infoLayoutLeft.setOnClickListener(this);
        infoLayoutRight.setOnClickListener(this);
        if (StringUtils.isEmpty(rightButtonText)) {
            rightButtonText = getContext().getString(R.string.confirm);
        }
        if (StringUtils.isEmpty(leftButtonText)) {
            leftButtonText = getContext().getString(R.string.cancel);
        }
        infoText.setText(text);
        if (!StringUtils.isEmpty(title)) {
            infoTips.setText(title);
        }
        infoTextRight.setText(rightButtonText);
        infoTextLeft.setText(leftButtonText);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.info_layout_left: {
                if (onClickListener != null) {
                    onClickListener.onClick(false);
                }
                dismiss();
                break;
            }
            case R.id.info_layout_right: {
                if (onClickListener != null) {
                    onClickListener.onClick(true);
                }
                dismiss();
                break;
            }
            default:
                break;
        }
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(boolean agree);
    }
}
