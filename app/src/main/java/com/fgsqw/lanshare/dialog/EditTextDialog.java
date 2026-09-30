package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.StringUtils;

public class EditTextDialog extends BaseDialog implements View.OnClickListener {

    private String str;
    private String hint;
    private String title;
    private LinearLayout okLayout;
    private LinearLayout cencelLayout;
    private EditText editText;
    private TextView tvTitle;
    private TextView tvOk;
    private TextView tvCancel;
    private OnClickListener onClickListener;
    private OnClickCheck onClickCheck;
    private boolean canEmpty;
    private int maxLen = -1;
    private String accepted = null;
    public static final String ACCEPTED_NUM = "0123456789";

    public EditTextDialog(@NonNull Context context, boolean canEmpty, String title, String str) {
        super(context);
        this.canEmpty = canEmpty;
        this.str = str;
        this.title = title;
    }

    public EditTextDialog(@NonNull Context context, boolean canEmpty, String title, String str, String hint) {
        super(context);
        this.canEmpty = canEmpty;
        this.str = str;
        this.hint = hint;
        this.title = title;
    }

    public EditTextDialog setMaxLen(int maxLen) {
        this.maxLen = maxLen;
        return this;
    }

    public void setLeftButtonText(String text) {
        tvCancel.setText(text);
    }

    public void setRightButtonText(String text) {
        tvOk.setText(text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_dialog);
        okLayout = findViewById(R.id.info_layout_ok);
        cencelLayout = findViewById(R.id.info_layout_cancel);
        editText = findViewById(R.id.dialog_str_edit);
        tvTitle = findViewById(R.id.info_tips);
        tvOk = findViewById(R.id.info_tv_ok);
        tvCancel = findViewById(R.id.info_tv_cancel);
        editText.setText(str);
        if (!StringUtils.isEmpty(str)) {
            editText.setSelection(str.length());
        }
        if (!StringUtils.isEmpty(hint)) {
            editText.setHint(hint);
        }
        if (maxLen != -1) {
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        }
        if (!StringUtils.isEmpty(accepted)) {
            editText.setKeyListener(DigitsKeyListener.getInstance(accepted));
        }
        tvTitle.setText(title);
        okLayout.setOnClickListener(this);
        cencelLayout.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.info_layout_ok: {
                String text = editText.getText().toString();
                if (!canEmpty && StringUtils.isEmpty(text)) {
                    T.ss(R.string.input_content_cannot_be_empty);
                    break;
                }
                if (onClickListener != null) {
                    if (onClickCheck != null) {
                        if (!onClickCheck.onClick(text)) {
                            return;
                        }
                    }
                    onClickListener.onClick(true, text);
                }
                break;
            }
            case R.id.info_layout_cancel: {
                break;
            }
        }
        dismiss();
    }

    public void setAccepted(String accepted) {
        this.accepted = accepted;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnClickCheck(OnClickCheck onClickCheck) {
        this.onClickCheck = onClickCheck;
    }

    public interface OnClickListener {
        void onClick(boolean ok, String str);
    }

    public interface OnClickCheck {
        boolean onClick(String str);
    }

}
