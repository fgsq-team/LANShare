package com.fgsqw.lanshare.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 鼠标模拟View
 * 在屏幕上绘制一个可移动的鼠标光标，并显示移动轨迹
 * 光标位置由外部（传感器数据）驱动
 */
public class MousePadView extends View {

    // 光标位置（像素坐标）
    private float cursorX, cursorY;

    // 光标移动轨迹（保留最近的点）
    private final List<float[]> trail = new ArrayList<>();
    private static final int MAX_TRAIL = 300;

    // 灵敏度（像素/秒 每单位倾斜）
    private float sensitivity = 800f;

    // 画笔
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 颜色常量
    private static final int COLOR_BG = 0xFF1A1A2E;
    private static final int COLOR_GRID = 0xFF252545;
    private static final int COLOR_CURSOR = 0xFFFFFFFF;
    private static final int COLOR_CURSOR_FILL = 0xCC00E5FF;
    private static final int COLOR_CROSSHAIR = 0x33FFFFFF;
    private static final int COLOR_TRAIL_START = 0xFF00E5FF;
    private static final int COLOR_BORDER = 0xFF0F3460;

    // 光标尺寸
    private static final float CURSOR_SIZE = 28f;
    private static final float CURSOR_STROKE = 3f;

    public MousePadView(Context context) {
        this(context, null);
    }

    public MousePadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MousePadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(COLOR_BG);

        // 光标描边
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(CURSOR_STROKE);
        cursorPaint.setColor(COLOR_CURSOR);

        // 光标填充
        cursorFillPaint.setStyle(Paint.Style.FILL);
        cursorFillPaint.setColor(COLOR_CURSOR_FILL);

        // 轨迹
        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeWidth(2f);
        trailPaint.setColor(COLOR_TRAIL_START);

        // 网格
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(COLOR_GRID);

        // 十字准线
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1f);
        crosshairPaint.setColor(COLOR_CROSSHAIR);

        // 文字
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);

        // 文字背景
        textBgPaint.setColor(0xAA000000);
        textBgPaint.setStyle(Paint.Style.FILL);

        // 边框
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(COLOR_BORDER);
    }

    /**
     * 设置光标位置
     */
    public void setCursorPosition(float x, float y) {
        this.cursorX = x;
        this.cursorY = y;
        // 记录轨迹
        synchronized (trail) {
            trail.add(new float[]{x, y});
            if (trail.size() > MAX_TRAIL) {
                trail.remove(0);
            }
        }
        postInvalidate();
    }

    /**
     * 获取光标X坐标
     */
    public float getCursorX() {
        return cursorX;
    }

    /**
     * 获取光标Y坐标
     */
    public float getCursorY() {
        return cursorY;
    }

    /**
     * 设置灵敏度
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public float getSensitivity() {
        return sensitivity;
    }

    /**
     * 重置光标到中心
     */
    public void resetCursor() {
        cursorX = getWidth() / 2f;
        cursorY = getHeight() / 2f;
        synchronized (trail) {
            trail.clear();
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 绘制网格背景
        drawGrid(canvas, w, h);

        // 绘制边框
        canvas.drawRect(1, 1, w - 1, h - 1, borderPaint);

        // 绘制十字准线（跟随光标）
        drawCrosshair(canvas, w, h);

        // 绘制移动轨迹
        drawTrail(canvas);

        // 绘制鼠标光标
        drawCursor(canvas);

        // 绘制坐标信息
        drawInfo(canvas, w, h);
    }

    /**
     * 绘制网格背景
     */
    private void drawGrid(Canvas canvas, int w, int h) {
        float step = 60f;
        // 竖线
        for (float x = step; x < w; x += step) {
            canvas.drawLine(x, 0, x, h, gridPaint);
        }
        // 横线
        for (float y = step; y < h; y += step) {
            canvas.drawLine(0, y, w, y, gridPaint);
        }
        // 中心十字（参考线）
        Paint centerPaint = new Paint(gridPaint);
        centerPaint.setColor(0xFF333366);
        centerPaint.setStrokeWidth(2f);
        canvas.drawLine(w / 2f, 0, w / 2f, h, centerPaint);
        canvas.drawLine(0, h / 2f, w, h / 2f, centerPaint);
    }

    /**
     * 绘制十字准线
     */
    private void drawCrosshair(Canvas canvas, int w, int h) {
        canvas.drawLine(cursorX, 0, cursorX, h, crosshairPaint);
        canvas.drawLine(0, cursorY, w, cursorY, crosshairPaint);
    }

    /**
     * 绘制移动轨迹
     */
    private void drawTrail(Canvas canvas) {
        synchronized (trail) {
            int size = trail.size();
            if (size < 2) return;

            for (int i = 1; i < size; i++) {
                float[] prev = trail.get(i - 1);
                float[] curr = trail.get(i);
                // 越新的轨迹越不透明
                float alpha = (float) i / size;
                trailPaint.setAlpha((int) (alpha * 200));
                canvas.drawLine(prev[0], prev[1], curr[0], curr[1], trailPaint);
            }

            // 在轨迹末端画一个小圆点
            if (size > 0) {
                float[] last = trail.get(size - 1);
                Paint dotPaint = new Paint(trailPaint);
                dotPaint.setStyle(Paint.Style.FILL);
                dotPaint.setAlpha(150);
                canvas.drawCircle(last[0], last[1], 3f, dotPaint);
            }
        }
    }

    /**
     * 绘制鼠标光标（箭头形状）
     */
    private void drawCursor(Canvas canvas) {
        float size = CURSOR_SIZE;
        float x = cursorX;
        float y = cursorY;

        // 箭头路径（经典鼠标指针形状）
        Path arrow = new Path();
        arrow.moveTo(x, y);                           // 尖端
        arrow.lineTo(x, y + size);                    // 下
        arrow.lineTo(x + size * 0.35f, y + size * 0.7f);  // 右下凹
        arrow.lineTo(x + size * 0.55f, y + size * 1.1f); // 柄右下
        arrow.lineTo(x + size * 0.7f, y + size * 1.0f);  // 柄右上
        arrow.lineTo(x + size * 0.5f, y + size * 0.6f);  // 右上凹
        arrow.lineTo(x + size * 0.85f, y + size * 0.55f); // 右
        arrow.close();

        // 绘制阴影
        Paint shadowPaint = new Paint(cursorFillPaint);
        shadowPaint.setColor(0x44000000);
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.translate(3, 3);
        canvas.drawPath(arrow, shadowPaint);
        canvas.restore();

        // 绘制填充
        canvas.drawPath(arrow, cursorFillPaint);
        // 绘制描边
        canvas.drawPath(arrow, cursorPaint);
    }

    /**
     * 绘制坐标信息
     */
    private void drawInfo(Canvas canvas, int w, int h) {
        String info = String.format("光标: (%.0f, %.0f)  屏幕: %dx%d",
                cursorX, cursorY, w, h);
        float textWidth = textPaint.measureText(info);
        float padding = 8f;
        // 背景
        canvas.drawRect(8, h - 48, 16 + textWidth + padding * 2, h - 8, textBgPaint);
        // 文字
        canvas.drawText(info, 8 + padding, h - 20, textPaint);
    }
}
