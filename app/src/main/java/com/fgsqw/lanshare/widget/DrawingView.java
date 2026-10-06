package com.fgsqw.lanshare.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.constants.WSCmd;
import com.fgsqw.lanshare.utils.ThreadUtils;
import com.fgsqw.lanshare.web.LHttpServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义绘图View - 支持双向远程绘图
 * <p>
 * APP端触摸绘制 → WebSocket广播 → 网页Canvas同步渲染
 * 网页Canvas鼠标绘制 → WebSocket → APP端同步渲染
 * 坐标使用归一化值(0.0~1.0)，适配不同分辨率。
 * </p>
 *
 * @author fgsq
 */
public class DrawingView extends View {

    /** 单笔画数据：路径+颜色+粗细 */
    private static class Stroke {
        Path path;
        int color;
        float strokeWidth;

        Stroke(Path path, int color, float strokeWidth) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }

    /** 已完成的笔画列表 */
    private final List<Stroke> strokes = new ArrayList<>();
    /** 当前正在绘制的笔画（未完成） */
    private Path currentPath;
    private int currentColor = Color.RED;
    private float currentStrokeWidth = 5f;
    private boolean isDrawing = false;

    /** 通用画笔（每次onDraw根据Stroke设置颜色和粗细） */
    private Paint paint;

    /** 画布尺寸（用于坐标归一化） */
    private int canvasWidth = 1;
    private int canvasHeight = 1;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(currentStrokeWidth);
        paint.setColor(currentColor);
        currentPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasWidth = w;
        canvasHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制所有已完成的笔画
        synchronized (strokes) {
            for (Stroke stroke : strokes) {
                paint.setColor(stroke.color);
                paint.setStrokeWidth(stroke.strokeWidth);
                canvas.drawPath(stroke.path, paint);
            }
        }
        // 绘制当前正在进行的笔画
        if (currentPath != null && isDrawing) {
            paint.setColor(currentColor);
            paint.setStrokeWidth(currentStrokeWidth);
            canvas.drawPath(currentPath, paint);
        }
    }

    // ==================== 本地触摸绘制 ====================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float nx = x / canvasWidth;
        float ny = y / canvasHeight;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                isDrawing = true;
                sendDrawEvent("start", nx, ny, currentColor, currentStrokeWidth);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing) {
                    currentPath.lineTo(x, y);
                    sendDrawEvent("move", nx, ny, currentColor, currentStrokeWidth);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDrawing) {
                    currentPath.lineTo(x, y);
                    sendDrawEvent("end", nx, ny, currentColor, currentStrokeWidth);
                    // 将当前笔画存入已完成列表
                    synchronized (strokes) {
                        strokes.add(new Stroke(currentPath, currentColor, currentStrokeWidth));
                    }
                    currentPath = new Path();
                    isDrawing = false;
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    // ==================== 远程绘制（来自网页） ====================

    /**
     * 处理来自网页端的绘图事件（仅本地渲染，不广播）
     * 需在UI线程调用
     */
    public void drawFromRemote(String action, float nx, float ny, int color, float strokeWidth) {
        float x = nx * canvasWidth;
        float y = ny * canvasHeight;

        switch (action) {
            case "start":
                currentPath = new Path();
                currentPath.moveTo(x, y);
                currentColor = color;
                currentStrokeWidth = strokeWidth;
                isDrawing = true;
                invalidate();
                break;

            case "move":
                if (isDrawing && currentPath != null) {
                    currentColor = color;
                    currentStrokeWidth = strokeWidth;
                    currentPath.lineTo(x, y);
                    invalidate();
                }
                break;

            case "end":
                if (isDrawing && currentPath != null) {
                    currentPath.lineTo(x, y);
                    synchronized (strokes) {
                        strokes.add(new Stroke(currentPath, color, strokeWidth));
                    }
                    currentPath = new Path();
                    isDrawing = false;
                    invalidate();
                }
                break;

            case "clear":
                clearLocal();
                break;
        }
    }

    // ==================== WebSocket 广播 ====================

    /**
     * 通过WebSocket广播绘图事件（来源标识为"app"）
     */
    private void sendDrawEvent(String action, float nx, float ny, int color, float strokeWidth) {
        JSONObject json = new JSONObject();
        json.put("cmd", WSCmd.DRAW_EVENT);
        json.put("action", action);
        json.put("x", nx);
        json.put("y", ny);
        json.put("color", color);
        json.put("strokeWidth", strokeWidth);
        json.put("from", "app");
        String jsonStr = json.toJSONString();
        ThreadUtils.runThread(() -> LHttpServer.sendDrawEvent(jsonStr));
    }

    // ==================== 公共方法 ====================

    public void setColor(int color) {
        currentColor = color;
        paint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        currentStrokeWidth = width;
        paint.setStrokeWidth(width);
    }

    /**
     * 清空画布（本地+广播）
     */
    public void clearCanvas() {
        clearLocal();
        JSONObject json = new JSONObject();
        json.put("cmd", WSCmd.DRAW_EVENT);
        json.put("action", "clear");
        json.put("from", "app");
        String jsonStr = json.toJSONString();
        ThreadUtils.runThread(() -> LHttpServer.sendDrawEvent(jsonStr));
    }

    /**
     * 仅清空本地画布（不广播，用于远程清空）
     */
    private void clearLocal() {
        synchronized (strokes) {
            strokes.clear();
        }
        currentPath = new Path();
        isDrawing = false;
        invalidate();
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public float getCurrentStrokeWidth() {
        return currentStrokeWidth;
    }
}
