package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.web.PrivacyWebActivity;
import com.fgsqw.lanshare.toast.T;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrivacyDialog extends Dialog implements View.OnClickListener {

    LinearLayout privacyLayoutAgree;
    LinearLayout privacyLayoutDisagree;
    private OnClickListener onClickListener;


    public PrivacyDialog(@NonNull Context context) {
        super(context);
    }

    public PrivacyDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected PrivacyDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.privacy_dialog);
        privacyLayoutAgree = findViewById(R.id.privacy_layout_agree);
        privacyLayoutDisagree = findViewById(R.id.privacy_layout_disagree);

        TextView privacyText = findViewById(R.id.privacy_text);
        privacyText.setText(setTextView("嗨，亲爱的用户，很高兴遇见你，为了您的隐私数据安全，在您使用App之前您需要仔细阅读《用户协议》和《隐私政策》。点击 \"同意\"按钮代表您此政策和协议", "#《用户协议》#和#《隐私政策》#", privacyText));
        privacyText.setHighlightColor(getContext().getResources().getColor(R.color.white));

        privacyLayoutAgree.setOnClickListener(this);
        privacyLayoutDisagree.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.privacy_layout_agree: {
                if (onClickListener != null) {
                    onClickListener.onClick(true);
                }
                break;
            }
            case R.id.privacy_layout_disagree: {
                if (onClickListener != null) {
                    onClickListener.onClick(false);
                }
                break;
            }
            default:
                break;
        }
    }

    private CharSequence setTextView(String desc, String target, TextView tv) {
        /*正则表达式  取出 两个#之间的内容 （不包含#） */
        Pattern p = Pattern.compile("#([^\\#|]+)#");
        /*android 提供的 具有强大的CharSequence 处理能力 各种区域处理*/
        SpannableString ss = new SpannableString(desc);
        Matcher m = p.matcher(target);
        /*由于@昵称、#话题#、http://等这些关键字是可以点击的，所以我们需要对TextView做一些处理，需要去设置它的MovementMethod*/
        if (m.find()) {
            // 要实现文字的点击效果，这里需要做特殊处理
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            // 重置正则位置
            m.reset();
        }
        while (m.find()) {
            final String s = m.group(1);

            int startIndex = desc.indexOf(s);
            int endIndex = startIndex + s.length();
            /*区域处理*/
            ss.setSpan(new ForegroundColorSpan(Color.BLUE), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    // 去掉两边的#号，获取点击的话题内容
                    String content = s.substring(1, s.length() - 1);
                    Intent intent = new Intent(getContext(), PrivacyWebActivity.class);
                    if (content.contains("用户协议")) {
                        intent.putExtra("name", "userAgreement.html");
                    } else {
                        intent.putExtra("name", "privacyPolicy.html");
                    }
                    getContext().startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    // 字体变色
                    ds.setColor(Color.parseColor("#507daf"));
                    // 设置下划线
                    ds.setUnderlineText(false);
                }
            }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ss;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(boolean agree);
    }
}
