package com.fgsqw.lanshare.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
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

    private DrawingView drawingView;
    private View colorRed, colorGreen, colorBlue, colorBlack, colorWhite;
    private SeekBar strokeSeekbar;
    private View activeColorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);
        sInstance = this;
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void initViews() {
        drawingView = bind(R.id.drawing_view);
        colorRed = bind(R.id.draw_color_red);
        colorGreen = bind(R.id.draw_color_green);
        colorBlue = bind(R.id.draw_color_blue);
        colorBlack = bind(R.id.draw_color_black);
        colorWhite = bind(R.id.draw_color_white);
        strokeSeekbar = bind(R.id.draw_stroke_seekbar);

        // 返回按钮
        bind(R.id.draw_back).setOnClickListener(this);
        // 清空按钮
        bind(R.id.draw_clear).setOnClickListener(this);

        // 颜色选择
        colorRed.setOnClickListener(this);
        colorGreen.setOnClickListener(this);
        colorBlue.setOnClickListener(this);
        colorBlack.setOnClickListener(this);
        colorWhite.setOnClickListener(this);

        // 默认红色选中
        activeColorView = colorRed;
        highlightColor(colorRed);

        // 画笔粗细
        strokeSeekbar.setProgress(3);
        drawingView.setStrokeWidth(3 + 2); // 最小2px
        strokeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    drawingView.setStrokeWidth(progress + 2);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.draw_back) {
            finish();
        } else if (id == R.id.draw_clear) {
            drawingView.clearCanvas();
        } else if (id == R.id.draw_color_red) {
            drawingView.setColor(Color.RED);
            highlightColor(colorRed);
        } else if (id == R.id.draw_color_green) {
            drawingView.setColor(Color.parseColor("#4CAF50"));
            highlightColor(colorGreen);
        } else if (id == R.id.draw_color_blue) {
            drawingView.setColor(Color.parseColor("#2196F3"));
            highlightColor(colorBlue);
        } else if (id == R.id.draw_color_black) {
            drawingView.setColor(Color.BLACK);
            highlightColor(colorBlack);
        } else if (id == R.id.draw_color_white) {
            drawingView.setColor(Color.WHITE);
            highlightColor(colorWhite);
        }
    }

    /**
     * 高亮当前选中的颜色（加边框）
     */
    private void highlightColor(View colorView) {
        if (activeColorView != null) {
            activeColorView.setElevation(0);
        }
        activeColorView = colorView;
        colorView.setElevation(8);
    }
}
