package com.fgsqw.lanshare.widget;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.fgsqw.lanshare.utils.LLog;

/**
 * 继承ViewPager并在onInterceptTouchEvent捕捉异常。
 * 因为ViewPager嵌套PhotoView使用，有时候会发生IllegalArgumentException异常。
 */
public class PreViewViewPager extends ViewPager {

    public PreViewViewPager(Context context) {
        super(context);
    }

    public PreViewViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            LLog.error(e);
            return false;
        }
    }
}
