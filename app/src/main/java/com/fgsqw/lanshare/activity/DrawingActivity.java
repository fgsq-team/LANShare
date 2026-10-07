package com.fgsqw.lanshare.activity;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.dialog.ColorPickerDialog;
import com.fgsqw.lanshare.dialog.DeviceSelectDialog;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.service.DrawSyncManager;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.widget.DrawingView;

/**
 * 远程绘图Activity
 * <p>
 * 提供触摸绘图界面，绘制内容通过WebSocket实时同步到网页端，
 * 支持颜色选择、画笔粗细调节和清空画布操作。
 * </p>
 *
 * @author fgsq
 */
public class DrawingActivity extends BaseActivity implements View.OnClickListener {

    /** 静态实例，供WebSocket处理器调用 */
    private static DrawingActivity sInstance;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    /** 接收端设备同步管理器（静态，供FileServer设置，DrawingView读取） */
    private static volatile DrawSyncManager sReceiverSyncManager;

    private DrawingView drawingView;
    private SeekBar strokeSeekbar;
    private TextView modeBtn;

    // 可展开面板
    private View compactBar;
    private View detailPanel;
    private TextView expandArrow;
    private View currentColorPreview;
    private View strokePreviewCompact;
    private View strokePreviewDetail;
    private TextView strokeSizeCompact;
    private TextView strokeSizeDetail;
    private boolean panelExpanded = false;

    // 颜色选择
    private View colorPickerPreview;
    private TextView colorHexText;
    private int currentColor = 0xFFFF0000;

    /** 设备同步管理器 */
    private DrawSyncManager drawSyncManager;
    /** 当前同步模式 */
    private DrawSyncManager.SyncMode currentMode = DrawSyncManager.SyncMode.WEB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);
        sInstance = this;
        drawSyncManager = new DrawSyncManager();
        initViews();
        setupDrawSyncManager();

        // 检查是否为接收端模式（从DataCenterActivity确认后跳转过来）
        boolean receiverMode = getIntent().getBooleanExtra("receiver_mode", false);
        if (receiverMode && sReceiverSyncManager != null) {
            String deviceName = getIntent().getStringExtra("device_name");
            // 切换到设备同步模式
            currentMode = DrawSyncManager.SyncMode.DEVICE;
            drawingView.setSyncMode(DrawSyncManager.SyncMode.DEVICE);
            drawingView.setDrawSyncManager(sReceiverSyncManager);
            modeBtn.setText(getString(R.string.device_prefix) + deviceName);
            T.s(getString(R.string.synced_with_device, deviceName));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawSyncManager != null) {
            drawSyncManager.disconnect();
        }
        sReceiverSyncManager = null;
        sInstance = null;
    }

    /**
     * 处理来自网页端的绘图事件（静态方法，供WebSocket处理器调用）
     * 会自动切换到UI线程执行
     */
    public static void handleRemoteDraw(String action, float nx, float ny, int color, float strokeWidth) {
        if (sInstance == null) return;
        sHandler.post(() -> {
            if (sInstance != null && sInstance.drawingView != null) {
                sInstance.drawingView.drawFromRemote(action, nx, ny, color, strokeWidth);
            }
        });
    }

    /**
     * 处理来自远端设备的绘图事件（静态方法，供FileServer TCP处理器调用）
     * <p>解析JSON格式的绘图数据，然后转发到handleRemoteDraw</p>
     *
     * @param drawJson 绘图事件JSON字符串
     */
    public static void handleRemoteDrawFromJson(String drawJson) {
        if (sInstance == null) return;
        try {
            JSONObject json = JSON.parseObject(drawJson);
            String action = json.getString("action");
            float nx = json.getFloatValue("x");
            float ny = json.getFloatValue("y");
            int color = json.getIntValue("color");
            float strokeWidth = json.getFloatValue("strokeWidth");
            handleRemoteDraw(action, nx, ny, color, strokeWidth);
        } catch (Exception e) {
            // 解析失败，忽略
        }
    }

    /**
     * 设置接收端DrawSyncManager（供FileServer调用，在启动DrawingActivity之前调用）
     *
     * @param manager 接收端的DrawSyncManager
     */
    public static void setReceiverSyncManager(DrawSyncManager manager) {
        sReceiverSyncManager = manager;
    }

    /**
     * 已在绘图界面时，直接设置接收端同步（不跳转，供FileServer调用）
     *
     * @param manager    接收端的DrawSyncManager
     * @param deviceName 请求方设备名称
     */
    public static void setupReceiverSync(DrawSyncManager manager, String deviceName) {
        sReceiverSyncManager = manager;
        sHandler.post(() -> {
            if (sInstance != null) {
                sInstance.currentMode = DrawSyncManager.SyncMode.DEVICE;
                sInstance.drawingView.setSyncMode(DrawSyncManager.SyncMode.DEVICE);
                sInstance.drawingView.setDrawSyncManager(manager);
                sInstance.modeBtn.setText(sInstance.getString(R.string.device_prefix) + deviceName);
                T.s(sInstance.getString(R.string.synced_with_device, deviceName));
            }
        });
    }

    /**
     * 获取接收端DrawSyncManager（供DrawingView发送绘图事件）
     */
    public static DrawSyncManager getReceiverSyncManager() {
        return sReceiverSyncManager;
    }

    /**
     * 清理接收端同步引用（供断连回调调用，防止重连时DrawingView找到已死的manager）
     */
    public static void clearReceiverSync() {
        sReceiverSyncManager = null;
        sHandler.post(() -> {
            if (sInstance != null) {
                sInstance.currentMode = DrawSyncManager.SyncMode.WEB;
                sInstance.drawingView.setSyncMode(DrawSyncManager.SyncMode.WEB);
                sInstance.drawingView.setDrawSyncManager(null);
                sInstance.modeBtn.setText(sInstance.getString(R.string.web_sync));
            }
        });
    }

    private void initViews() {
        drawingView = bind(R.id.drawing_view);
        modeBtn = bind(R.id.draw_mode_btn);

        // 返回按钮 & 清空按钮 & 模式按钮
        bind(R.id.draw_back).setOnClickListener(this);
        bind(R.id.draw_clear).setOnClickListener(this);
        modeBtn.setOnClickListener(this);

        // 可展开面板
        compactBar = bind(R.id.draw_compact_bar);
        detailPanel = bind(R.id.draw_detail_panel);
        expandArrow = bind(R.id.draw_expand_arrow);
        currentColorPreview = bind(R.id.draw_current_color);
        strokePreviewCompact = bind(R.id.draw_stroke_preview);
        strokePreviewDetail = bind(R.id.draw_stroke_detail_preview);
        strokeSizeCompact = bind(R.id.draw_stroke_size_text);
        strokeSizeDetail = bind(R.id.draw_stroke_detail_size);

        // 点击收起栏展开/收起
        compactBar.setOnClickListener(v -> togglePanel());

        // 颜色选择器
        colorPickerPreview = bind(R.id.draw_color_picker_preview);
        colorHexText = bind(R.id.draw_color_hex_text);
        bind(R.id.draw_color_pick_btn).setOnClickListener(v -> showColorPicker());
        updateColorDisplay(currentColor);

        // 画笔粗细
        strokeSeekbar = bind(R.id.draw_stroke_seekbar);
        strokeSeekbar.setProgress(3);
        drawingView.setStrokeWidth(3 + 2);
        updateStrokePreview(3 + 2);
        strokeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float width = progress + 2;
                drawingView.setStrokeWidth(width);
                updateStrokePreview(width);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * 显示颜色选择器对话框
     */
    private void showColorPicker() {
        new ColorPickerDialog(this, color -> {
            currentColor = color;
            drawingView.setColor(color);
            updateColorDisplay(color);
        }).setInitialColor(currentColor).show();
    }

    /**
     * 更新颜色显示（收起栏预览 + 展开栏预览 + HEX文字）
     */
    private void updateColorDisplay(int color) {
        // 收起栏预览圆
        currentColorPreview.setBackgroundColor(color);
        if (color == 0xFFFFFFFF) {
            GradientDrawable border = new GradientDrawable();
            border.setStroke(1, 0xFFCCCCCC);
            border.setColor(0xFFFFFFFF);
            currentColorPreview.setBackground(border);
        } else {
            currentColorPreview.setBackgroundColor(color);
        }
        // 展开栏预览圆
        if (colorPickerPreview != null) {
            colorPickerPreview.setBackgroundColor(color);
        }
        // HEX文字
        if (colorHexText != null) {
            colorHexText.setText(String.format("#%06X", color & 0xFFFFFF));
        }
    }

    /**
     * 展开/收起详细面板
     */
    private void togglePanel() {
        panelExpanded = !panelExpanded;
        ViewGroup parent = (ViewGroup) detailPanel.getParent();
        TransitionManager.beginDelayedTransition(parent, new AutoTransition().setDuration(200));
        if (panelExpanded) {
            detailPanel.setVisibility(View.VISIBLE);
            expandArrow.setText("▲");
        } else {
            detailPanel.setVisibility(View.GONE);
            expandArrow.setText("▼");
        }
    }

    /**
     * 更新收起栏的颜色预览（供外部调用时兼容）
     */
    private void updateColorPreview(int color) {
        updateColorDisplay(color);
    }

    /**
     * 更新粗细预览（同时更新收起栏和展开栏的预览圆）
     */
    private void updateStrokePreview(float width) {
        String sizeText = (int) width + "px";
        strokeSizeCompact.setText(sizeText);
        strokeSizeDetail.setText(sizeText);
        // 预览圆大小 = 基础8dp + 宽度映射
        float density = getResources().getDisplayMetrics().density;
        int previewSize = (int) ((8 + Math.min(width, 20)) * density);
        updatePreviewCircleSize(strokePreviewCompact, previewSize);
        updatePreviewCircleSize(strokePreviewDetail, previewSize);
    }

    private void updatePreviewCircleSize(View preview, int sizePx) {
        ViewGroup.LayoutParams lp = preview.getLayoutParams();
        lp.width = sizePx;
        lp.height = sizePx;
        preview.setLayoutParams(lp);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.draw_back) {
            finish();
        } else if (id == R.id.draw_clear) {
            drawingView.clearCanvas();
        } else if (id == R.id.draw_mode_btn) {
            onModeBtnClick();
        }
    }

    // ==================== 模式切换 ====================

    /**
     * 模式按钮点击处理
     * <p>WEB模式: 点击后切换到DEVICE模式，弹出设备选择对话框</p>
     * <p>DEVICE模式: 点击后切换到WEB模式，断开设备连接</p>
     */
    private void onModeBtnClick() {
        if (currentMode == DrawSyncManager.SyncMode.WEB) {
            // 切换到设备同步模式，弹出设备选择
            showDeviceSelectDialog();
        } else {
            // 切换回网页同步模式
            switchToWebMode();
        }
    }

    /**
     * 显示设备选择对话框
     */
    private void showDeviceSelectDialog() {
        DeviceSelectDialog dialog = new DeviceSelectDialog(this);
        dialog.setTitle("选择同步设备");
        dialog.setShowAllDevices(false);
        dialog.setOnDeviceSelect(device -> {
            if (device.getDevIP() != null) {
                switchToDeviceMode(device);
            }
        });
        dialog.show();
    }

    /**
     * 切换到设备同步模式
     */
    private void switchToDeviceMode(Device device) {
        currentMode = DrawSyncManager.SyncMode.DEVICE;
        drawingView.setSyncMode(DrawSyncManager.SyncMode.DEVICE);
        drawingView.setDrawSyncManager(drawSyncManager);
        modeBtn.setText(R.string.connecting);
        drawSyncManager.connect(device);
    }

    /**
     * 切换到网页同步模式
     */
    private void switchToWebMode() {
        currentMode = DrawSyncManager.SyncMode.WEB;
        drawingView.setSyncMode(DrawSyncManager.SyncMode.WEB);
        drawingView.setDrawSyncManager(null);
        // 断开所有设备连接（发起方 + 接收方）
        drawSyncManager.disconnect();
        if (sReceiverSyncManager != null) {
            sReceiverSyncManager.disconnect();
            sReceiverSyncManager = null;
        }
        modeBtn.setText(getString(R.string.web_sync));
    }

    /**
     * 设置DrawSyncManager回调
     */
    private void setupDrawSyncManager() {
        drawSyncManager.setConnectionListener(new DrawSyncManager.ConnectionListener() {
            @Override
            public void onConnected(Device device) {
                modeBtn.setText(getString(R.string.device_prefix) + device.getDevName());
                T.s(getString(R.string.connected_to_device, device.getDevName()));
            }

            @Override
            public void onConnectFailed(Device device, String error) {
                T.s(getString(R.string.connect_device_failed, error));
                switchToWebMode();
            }

            @Override
            public void onDisconnected(Device device) {
                // 重置UI状态（不调用switchToWebMode()，避免其disconnect()杀死可能的新连接）
                // Toast已由DrawSyncManager.handleDisconnect()显示
                currentMode = DrawSyncManager.SyncMode.WEB;
                drawingView.setSyncMode(DrawSyncManager.SyncMode.WEB);
                drawingView.setDrawSyncManager(null);
                modeBtn.setText(getString(R.string.web_sync));
            }
        });
    }
}
