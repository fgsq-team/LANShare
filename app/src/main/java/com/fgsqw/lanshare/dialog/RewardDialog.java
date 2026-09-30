package com.fgsqw.lanshare.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.toast.T;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RewardDialog extends BaseDialog {

    private ImageView ivWechatQr;
    private ImageView ivAlipayQr;
    private LinearLayout layoutClose;

    public RewardDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_reward);

        setCancelable(true);
        setCanceledOnTouchOutside(true);

        initViews();
        initListeners();
    }

    private void initViews() {
        ivWechatQr = findViewById(R.id.iv_wechat_qr);
        ivAlipayQr = findViewById(R.id.iv_alipay_qr);
        layoutClose = findViewById(R.id.layout_close);
    }

    private void initListeners() {
        // 微信二维码长按事件
        ivWechatQr.setOnLongClickListener(v -> {
            saveDrawableToGallery(R.drawable.wx, "wechat_reward_qr.png", "微信");
            return true;
        });

        // 支付宝二维码长按事件
        ivAlipayQr.setOnLongClickListener(v -> {
            saveDrawableToGallery(R.drawable.alipay, "alipay_reward_qr.png", "支付宝");
            return true;
        });

        layoutClose.setOnClickListener(v -> dismiss());
    }


    /**
     * 保存drawable资源到相册
     */
    private void saveDrawableToGallery(int drawableResId, String fileName, String payType) {
        try {
            // 获取drawable资源
            Drawable drawable = ContextCompat.getDrawable(getContext(), drawableResId);
            if (drawable == null) {
                return;
            }

            // 将drawable转换为bitmap
            Bitmap bitmap = drawableToBitmap(drawable);

            // 保存到相册
            String savedPath = saveBitmapToGallery(bitmap, fileName);

            if (savedPath != null) {
                T.ss(payType + "二维码已保存至: " + savedPath);
            } else {
                T.s("保存失败，请重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将Drawable转换为Bitmap
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * 保存Bitmap到相册
     */
    private String saveBitmapToGallery(Bitmap bitmap, String fileName) {
        // 创建保存路径
        String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + "RewardQR";

        File folder = new File(galleryPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 创建文件
        File file = new File(folder, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // 通知媒体库更新
            MediaScannerConnection.scanFile(getContext(),
                    new String[]{file.getAbsolutePath()},
                    new String[]{"image/png"},
                    null);

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
