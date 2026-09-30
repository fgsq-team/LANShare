package com.fgsqw.lanshare.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 聊天气泡背景：圆角矩形 + 一侧小尖角，宽高自适应，
 * 并提供与气泡外形一致的 Outline，供 elevation 阴影使用。
 */
public class BubbleDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF body = new RectF();
    private final float radius;
    private final float tailW;
    private final float tailH;
    private final boolean tailLeft;

    /**
     * @param color     气泡填充色
     * @param tailLeft  true=尖角在左侧（接收），false=尖角在右侧（发送）
     * @param radius    圆角半径（px）
     * @param tailW     尖角宽度（px）
     * @param tailH     尖角高度（px）
     */
    public BubbleDrawable(int color, boolean tailLeft, float radius, float tailW, float tailH) {
        this.tailLeft = tailLeft;
        this.radius = radius;
        this.tailW = tailW;
        this.tailH = tailH;
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        super.onBoundsChange(b);
        float w = b.width();
        float h = b.height();
        float tw = Math.min(tailW, w * 0.3f);
        float th = Math.min(tailH, h * 0.5f);

        body.set(b);
        if (tailLeft) {
            body.left += tw;
        } else {
            body.right -= tw;
        }

        path.reset();
        path.setFillType(Path.FillType.WINDING);
        path.addRoundRect(body, radius, radius, Path.Direction.CW);

        float cy = b.centerY();
        Path tail = new Path();
        if (tailLeft) {
            tail.moveTo(b.left, cy);
            tail.lineTo(body.left + 1, cy - th / 2);
            tail.lineTo(body.left + 1, cy + th / 2);
        } else {
            tail.moveTo(b.right, cy);
            tail.lineTo(body.right - 1, cy + th / 2);
            tail.lineTo(body.right - 1, cy - th / 2);
        }
        tail.close();
        path.addPath(tail);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (path.isEmpty()) {
            return;
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        if (body.isEmpty()) {
            outline.setAlpha(0);
            return;
        }
        outline.setRoundRect(Math.round(body.left), Math.round(body.top),
                Math.round(body.right), Math.round(body.bottom), radius);
        outline.setAlpha(1f);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
