package com.fgsqw.lanshare.player;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 电量指示视图：横向电池外壳 + 按百分比填充的电量条。
 * 替代旧实现中的 5 档静态电池图，可平滑反映真实电量。
 */
public class BatteryView extends View {

    private int level = 0;

    private Paint bodyPaint;
    private Paint fillPaint;
    private RectF shellRect;
    private RectF fillRect;
    private int bodyColor;
    private int lowColor;

    public BatteryView(Context context) {
        this(context, null);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bodyColor = context.getResources().getColor(R.color.lp_battery_body);
        lowColor = context.getResources().getColor(R.color.lp_battery_low);
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BatteryView);
            level = ta.getInteger(R.styleable.BatteryView_lpBatteryLevel, level);
            ta.recycle();
        }
        bodyPaint = new Paint();
        bodyPaint.setAntiAlias(true);
        bodyPaint.setStyle(Paint.Style.STROKE);
        bodyPaint.setStrokeWidth(dp(context, 1.4f));
        bodyPaint.setColor(bodyColor);

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(bodyColor);

        shellRect = new RectF();
        fillRect = new RectF();
    }

    /** 设置电量百分比 0~100。 */
    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
        invalidate();
    }

    public int getLevel() {
        return level;
    }

    private static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float stroke = bodyPaint.getStrokeWidth();
        // 右侧正极帽宽度约占整体 6%
        float capWidth = Math.max(dp(getContext(), 1.5f), w * 0.06f);
        float shellRight = w - capWidth - stroke / 2f;
        float inset = stroke / 2f + 1;
        shellRect.set(inset, inset, shellRight, h - inset);
        // 电池外壳
        canvas.drawRoundRect(shellRect, dp(getContext(), 2f), dp(getContext(), 2f), bodyPaint);
        // 正极帽
        float capTop = h / 2f - dp(getContext(), 2.5f);
        float capBottom = h / 2f + dp(getContext(), 2.5f);
        canvas.drawRect(shellRight + stroke / 2f, capTop, w, capBottom, bodyPaint);
        // 电量填充
        float innerLeft = shellRect.left + dp(getContext(), 1.6f);
        float innerTop = shellRect.top + dp(getContext(), 1.6f);
        float innerRight = shellRect.right - dp(getContext(), 1.6f);
        float innerBottom = shellRect.bottom - dp(getContext(), 1.6f);
        float innerWidth = innerRight - innerLeft;
        fillPaint.setColor(level <= 20 ? lowColor : bodyColor);
        if (level > 0 && innerWidth > 0) {
            fillRect.set(innerLeft, innerTop,
                    innerLeft + innerWidth * level / 100f, innerBottom);
            canvas.drawRoundRect(fillRect, dp(getContext(), 1f), dp(getContext(), 1f), fillPaint);
        }
    }
}
