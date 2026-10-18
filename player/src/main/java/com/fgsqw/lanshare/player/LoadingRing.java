package com.fgsqw.lanshare.player;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 自绘旋转弧加载指示器，替代旧实现中引入的第三方 ProgressWheel。
 * 仅绘制一段带缺口的圆弧并持续旋转。
 */
public class LoadingRing extends View {

    private static final int FRAME_MS = 16;

    private Paint ringPaint;
    private RectF ringRect;
    private int barColor;
    private float barWidth;
    private int sweepAngle;
    private float rotateDegrees;
    private boolean running;

    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            rotateDegrees = (rotateDegrees + 8f) % 360f;
            invalidate();
            postDelayed(this, FRAME_MS);
        }
    };

    public LoadingRing(Context context) {
        this(context, null);
    }

    public LoadingRing(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingRing(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        barColor = context.getResources().getColor(R.color.lp_loading);
        barWidth = VideoHelper.dip2px(context, 3);
        sweepAngle = 270;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LoadingRing);
            barColor = ta.getColor(R.styleable.LoadingRing_lpLoadingBarColor, barColor);
            barWidth = ta.getDimension(R.styleable.LoadingRing_lpLoadingBarWidth, barWidth);
            sweepAngle = ta.getInteger(R.styleable.LoadingRing_lpLoadingSweepAngle, sweepAngle);
            ta.recycle();
        }
        ringPaint = new Paint();
        ringPaint.setAntiAlias(true);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStrokeWidth(barWidth);
        ringPaint.setColor(barColor);
        ringRect = new RectF();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        post(animator);
    }

    public void stop() {
        running = false;
        removeCallbacks(animator);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getVisibility() != VISIBLE) {
            return;
        }
        float padding = barWidth / 2f + 1;
        ringRect.set(padding, padding, getWidth() - padding, getHeight() - padding);
        canvas.save();
        canvas.rotate(rotateDegrees, getWidth() / 2f, getHeight() / 2f);
        canvas.drawArc(ringRect, 0, sweepAngle, false, ringPaint);
        canvas.restore();
    }
}
