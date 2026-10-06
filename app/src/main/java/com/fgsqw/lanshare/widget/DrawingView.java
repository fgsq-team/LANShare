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
import com.fgsqw.lanshare.activity.DrawingActivity;
import com.fgsqw.lanshare.constants.WSCmd;
import com.fgsqw.lanshare.service.DrawSyncManager;
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
    /** 本地用户正在绘制的路径 */
    private Path localCurrentPath;
    /** 远端正在绘制的路径 */
    private Path remoteCurrentPath;
    /** 用户选中的画笔颜色（仅由用户操作改变，不受远端影响） */
    private int currentColor = Color.RED;
    /** 用户选中的画笔粗细（仅由用户操作改变，不受远端影响） */
    private float currentStrokeWidth = 5f;
    /** 远端笔画的渲染颜色 */
    private int remoteStrokeColor = Color.RED;
    /** 远端笔画的渲染粗细 */
    private float remoteStrokeWidth = 5f;
    /** 本地是否正在绘制 */
    private boolean isLocalDrawing = false;
    /** 远端是否正在绘制 */
    private boolean isRemoteDrawing = false;

    /** 通用画笔（每次onDraw根据Stroke设置颜色和粗细） */
    private Paint paint;

    /** 画布尺寸（用于坐标归一化） */
    private int canvasWidth = 1;
    private int canvasHeight = 1;

    /** 当前同步模式 */
    private DrawSyncManager.SyncMode syncMode = DrawSyncManager.SyncMode.WEB;
    /** 设备同步管理器（由DrawingActivity设置） */
    private DrawSyncManager drawSyncManager;

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
        localCurrentPath = new Path();
        remoteCurrentPath = new Path();
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
        // 绘制本地正在进行的笔画
        if (localCurrentPath != null && isLocalDrawing) {
            paint.setColor(currentColor);
            paint.setStrokeWidth(currentStrokeWidth);
            canvas.drawPath(localCurrentPath, paint);
        }
        // 绘制远端正在进行的笔画（独立路径，互不干扰）
        if (remoteCurrentPath != null && isRemoteDrawing) {
            paint.setColor(remoteStrokeColor);
            paint.setStrokeWidth(remoteStrokeWidth);
            canvas.drawPath(remoteCurrentPath, paint);
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
                localCurrentPath = new Path();
                localCurrentPath.moveTo(x, y);
                isLocalDrawing = true;
                sendDrawEvent("start", nx, ny, currentColor, currentStrokeWidth);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isLocalDrawing) {
                    localCurrentPath.lineTo(x, y);
                    sendDrawEvent("move", nx, ny, currentColor, currentStrokeWidth);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isLocalDrawing) {
                    localCurrentPath.lineTo(x, y);
                    sendDrawEvent("end", nx, ny, currentColor, currentStrokeWidth);
                    synchronized (strokes) {
                        strokes.add(new Stroke(localCurrentPath, currentColor, currentStrokeWidth));
                    }
                    localCurrentPath = new Path();
                    isLocalDrawing = false;
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    // ==================== 远程绘制（来自网页） ====================

    /**
     * 处理来自远端的绘图事件（仅本地渲染，不广播）
     * <p>笔画的颜色和粗细同步（用远端值渲染），但不影响本地用户的画笔选择</p>
     * 需在UI线程调用
     */
    public void drawFromRemote(String action, float nx, float ny, int color, float strokeWidth) {
        float x = nx * canvasWidth;
        float y = ny * canvasHeight;

        switch (action) {
            case "start":
                remoteCurrentPath = new Path();
                remoteCurrentPath.moveTo(x, y);
                remoteStrokeColor = color;
                remoteStrokeWidth = strokeWidth;
                isRemoteDrawing = true;
                invalidate();
                break;

            case "move":
                if (isRemoteDrawing && remoteCurrentPath != null) {
                    remoteStrokeColor = color;
                    remoteStrokeWidth = strokeWidth;
                    remoteCurrentPath.lineTo(x, y);
                    invalidate();
                }
                break;

            case "end":
                if (isRemoteDrawing && remoteCurrentPath != null) {
                    remoteCurrentPath.lineTo(x, y);
                    synchronized (strokes) {
                        strokes.add(new Stroke(remoteCurrentPath, color, strokeWidth));
                    }
                    remoteCurrentPath = new Path();
                    isRemoteDrawing = false;
                    invalidate();
                }
                break;

            case "clear":
                clearLocal();
                break;
        }
    }

    // ==================== WebSocket / TCP 广播 ====================

    /**
     * 根据当前同步模式发送绘图事件
     * <p>WEB模式: 通过WebSocket广播到所有网页客户端</p>
     * <p>DEVICE模式: 通过TCP长连接发送到目标设备（支持发起方和接收方双向同步）</p>
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
        ThreadUtils.runThread(() -> {
            DrawSyncManager active = getActiveDeviceManager();
            if (active != null) {
                active.sendDrawEvent(jsonStr);
            } else {
                LHttpServer.sendDrawEvent(jsonStr);
            }
        });
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
     * 清空画布（本地+根据模式广播）
     */
    public void clearCanvas() {
        clearLocal();
        JSONObject json = new JSONObject();
        json.put("cmd", WSCmd.DRAW_EVENT);
        json.put("action", "clear");
        json.put("from", "app");
        String jsonStr = json.toJSONString();
        ThreadUtils.runThread(() -> {
            DrawSyncManager active = getActiveDeviceManager();
            if (active != null) {
                active.sendDrawEvent(jsonStr);
            } else {
                LHttpServer.sendDrawEvent(jsonStr);
            }
        });
    }

    /**
     * 仅清空本地画布（不广播，用于远程清空）
     */
    private void clearLocal() {
        synchronized (strokes) {
            strokes.clear();
        }
        localCurrentPath = new Path();
        remoteCurrentPath = new Path();
        isLocalDrawing = false;
        isRemoteDrawing = false;
        invalidate();
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public float getCurrentStrokeWidth() {
        return currentStrokeWidth;
    }

    // ==================== 同步模式 ====================

    public void setSyncMode(DrawSyncManager.SyncMode mode) {
        this.syncMode = mode;
    }

    public DrawSyncManager.SyncMode getSyncMode() {
        return syncMode;
    }

    public void setDrawSyncManager(DrawSyncManager manager) {
        this.drawSyncManager = manager;
    }

    /**
     * 获取当前活跃的TCP设备同步管理器
     * <p>优先检查发起方的drawSyncManager，再检查接收方的静态receiverSyncManager</p>
     *
     * @return 已连接的DrawSyncManager，或null（无设备连接）
     */
    private DrawSyncManager getActiveDeviceManager() {
        // 发起方：用户主动连接的设备
        if (drawSyncManager != null && drawSyncManager.isConnected()) {
            return drawSyncManager;
        }
        // 接收方：被其他设备请求并接受后创建的
        DrawSyncManager receiver = DrawingActivity.getReceiverSyncManager();
        if (receiver != null && receiver.isConnected()) {
            return receiver;
        }
        return null;
    }
}
