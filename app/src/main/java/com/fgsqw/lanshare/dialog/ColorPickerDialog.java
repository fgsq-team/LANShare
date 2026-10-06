package com.fgsqw.lanshare.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 自定义HSV颜色选择器对话框
 * <p>
 * 提供色相(H)、饱和度(S)、明度(V)三个渐变色条，支持选择任意颜色。
 * 实时显示十六进制颜色值和大面积颜色预览。
 * </p>
 *
 * @author fgsq
 */
public class ColorPickerDialog extends Dialog {

    /** 颜色选择回调 */
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private final OnColorSelectedListener listener;
    private final float[] hsv = {0, 1, 1};
    private String title = "选择颜色";

    // UI
    private View colorPreview;
    private TextView hexText;
    private ColorSlider hueSlider, satSlider, valSlider;

    /**
     * 构造函数
     *
     * @param context  上下文
     * @param listener 颜色选择回调
     */
    public ColorPickerDialog(Context context, OnColorSelectedListener listener) {
        super(context);
        this.listener = listener;
    }

    /**
     * 设置初始颜色
     *
     * @param color 初始颜色（ARGB）
     * @return this
     */
    public ColorPickerDialog setInitialColor(int color) {
        Color.colorToHSV(color & 0x00FFFFFF, hsv);
        return this;
    }

    /**
     * 设置对话框标题
     *
     * @param title 标题文字
     * @return this
     */
    public ColorPickerDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(title);
        setContentView(buildContent());
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().density * 320),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        updateAll();
    }

    // ==================== 构建UI ====================

    private View buildContent() {
        float d = getContext().getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * d);
        root.setPadding(pad, (int) (12 * d), pad, pad);

        // 大面积颜色预览（圆角矩形）
        colorPreview = new View(getContext());
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (72 * d));
        previewLp.bottomMargin = (int) (8 * d);
        colorPreview.setLayoutParams(previewLp);
        colorPreview.setBackground(createRoundedBg((int) (10 * d), 0xFFFF0000));
        root.addView(colorPreview);

        // HEX 颜色值显示
        hexText = new TextView(getContext());
        LinearLayout.LayoutParams hexLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexLp.bottomMargin = (int) (16 * d);
        hexText.setLayoutParams(hexLp);
        hexText.setGravity(Gravity.CENTER);
        hexText.setTextSize(14);
        hexText.setTypeface(android.graphics.Typeface.MONOSPACE);
        root.addView(hexText);

        // 色相 (H) 色条：彩虹渐变
        hueSlider = new ColorSlider(getContext(), new int[]{
                0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
                0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
        });
        LinearLayout.LayoutParams hueLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (32 * d));
        hueLp.bottomMargin = (int) (4 * d);
        hueSlider.setLayoutParams(hueLp);
        hueSlider.setOnColorChangeListener(pos -> {
            hsv[0] = pos * 360f;
            updateAll();
        });
        root.addView(hueSlider);
        addSliderLabel(root, "色相");

        // 饱和度 (S) 色条：白 → 纯色（动态更新）
        satSlider = new ColorSlider(getContext(), new int[]{0xFFFFFFFF, 0xFFFF0000});
        LinearLayout.LayoutParams satLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (32 * d));
        satLp.topMargin = (int) (8 * d);
        satLp.bottomMargin = (int) (4 * d);
        satSlider.setLayoutParams(satLp);
        satSlider.setOnColorChangeListener(pos -> {
            hsv[1] = pos;
            updateAll();
        });
        root.addView(satSlider);
        addSliderLabel(root, "饱和度");

        // 明度 (V) 色条：黑 → 纯色（动态更新）
        valSlider = new ColorSlider(getContext(), new int[]{0xFF000000, 0xFFFF0000});
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (32 * d));
        valLp.topMargin = (int) (8 * d);
        valLp.bottomMargin = (int) (4 * d);
        valSlider.setLayoutParams(valLp);
        valSlider.setOnColorChangeListener(pos -> {
            hsv[2] = pos;
            updateAll();
        });
        root.addView(valSlider);
        addSliderLabel(root, "明度");

        // 按钮行
        LinearLayout btnRow = new LinearLayout(getContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnRowLp.topMargin = (int) (18 * d);
        btnRow.setLayoutParams(btnRowLp);

        Button cancelBtn = new Button(getContext());
        cancelBtn.setText("取消");
        cancelBtn.setOnClickListener(v -> dismiss());
        btnRow.addView(cancelBtn);

        Button okBtn = new Button(getContext());
        okBtn.setText("确定");
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        okLp.setMarginStart((int) (12 * d));
        okBtn.setLayoutParams(okLp);
        okBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorSelected(getSelectedColor());
            }
            dismiss();
        });
        btnRow.addView(okBtn);

        root.addView(btnRow);
        return root;
    }

    /** 添加色条下方的小标签 */
    private void addSliderLabel(LinearLayout parent, String text) {
        TextView label = new TextView(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (2 * d());
        label.setLayoutParams(lp);
        label.setText(text);
        label.setTextSize(11);
        label.setTextColor(0xFF888888);
        parent.addView(label);
    }

    // ==================== 颜色计算 ====================

    /** 获取当前选中的颜色（不透明） */
    public int getSelectedColor() {
        return Color.HSVToColor(hsv);
    }

    /** 更新所有UI元素 */
    private void updateAll() {
        int color = Color.HSVToColor(hsv);
        colorPreview.setBackgroundColor(color);

        // 更新饱和度色条：白 → 当前色相纯色
        float[] fullSatHsv = {hsv[0], 1f, 1f};
        int pureColor = Color.HSVToColor(fullSatHsv);
        satSlider.setGradientColors(new int[]{0xFFFFFFFF, pureColor});

        // 更新明度色条：黑 → 当前饱和度下的颜色
        valSlider.setGradientColors(new int[]{0xFF000000, color});

        // 更新HEX显示
        hexText.setText(String.format("#%06X", color & 0xFFFFFF));
    }

    /** 创建圆角背景 */
    private android.graphics.drawable.GradientDrawable createRoundedBg(int radius, int color) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setColor(color);
        return drawable;
    }

    /** 获取dp转换因子 */
    private float d() {
        return getContext().getResources().getDisplayMetrics().density;
    }

    // ==================== 自定义渐变色条 ====================

    /**
     * 自定义颜色滑块条
     * <p>绘制水平渐变色条，触摸拖动选择颜色位置(0.0~1.0)</p>
     */
    private static class ColorSlider extends View {

        interface Listener {
            void onChange(float position);
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private LinearGradient shader;
        private int[] colors;
        private float position; // 0.0 ~ 1.0
        private Listener listener;
        private boolean needsShaderUpdate = true;

        ColorSlider(Context context, int[] colors) {
            super(context);
            this.colors = colors;
            // 初始化指示器画笔（避免onDraw中创建对象）
            indicatorPaint.setColor(0xFFFFFFFF);
            indicatorPaint.setStyle(Paint.Style.STROKE);
            indicatorPaint.setStrokeWidth(2.5f);
            outlinePaint.setColor(0x44000000);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(5f);
        }

        void setOnColorChangeListener(Listener listener) {
            this.listener = listener;
        }

        /** 动态更新渐变色（用于饱和度/明度色条跟随色相变化） */
        void setGradientColors(int[] newColors) {
            this.colors = newColors;
            needsShaderUpdate = true;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            rect.set(0, 0, w, h);
            needsShaderUpdate = true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needsShaderUpdate && getWidth() > 0) {
                shader = new LinearGradient(0, 0, getWidth(), 0,
                        colors, null, Shader.TileMode.CLAMP);
                paint.setShader(shader);
                needsShaderUpdate = false;
            }
            // 绘制渐变色条（圆角）
            float r = getHeight() / 2f;
            canvas.drawRoundRect(rect, r, r, paint);

            // 绘制位置指示器（黑色描边 + 白色竖线）
            float x = position * getWidth();
            float top = 2 * getResources().getDisplayMetrics().density;
            float bottom = getHeight() - top;
            // 黑色描边增强对比
            canvas.drawLine(x, top, x, bottom, outlinePaint);
            // 白色覆盖线
            canvas.drawLine(x, top, x, bottom, indicatorPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = Math.max(0, Math.min(event.getX(), getWidth()));
            position = x / getWidth();
            if (listener != null) {
                listener.onChange(position);
            }
            invalidate();
            return true;
        }
    }
}
