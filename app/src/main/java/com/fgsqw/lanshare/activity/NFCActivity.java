package com.fgsqw.lanshare.activity;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;

import java.io.ByteArrayOutputStream;

public class NFCActivity extends BaseActivity {

    private NfcAdapter nfcAdapter;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        imageView = findViewById(R.id.nfc_imageView);

        // 获取NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "设备不支持NFC", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建要发送的图片数据
        NdefMessage ndefMessage = createImageNdefMessage();

        // 设置NDEF推送消息
        nfcAdapter.setNdefPushMessage(ndefMessage, this);
    }

    // 将图片转换为NdefMessage
// 将 Drawable 转换为 Bitmap 并压缩为字节数组
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private NdefMessage createImageNdefMessage() {
        try {
            // 从 ImageView 获取 Drawable
            android.graphics.drawable.Drawable drawable = imageView.getDrawable();
            if (drawable == null) {
                Toast.makeText(this, "图片为空，无法传输", Toast.LENGTH_SHORT).show();
                return null;
            }

            // 将 Drawable 转换为 Bitmap
            android.graphics.Bitmap bitmap = drawableToBitmap(drawable);

            // 将 Bitmap 压缩为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // 创建 NDEF 记录
            NdefRecord imageRecord = NdefRecord.createMime("image/png", imageBytes);

            // 组装 NDEF 消息
            return new NdefMessage(new NdefRecord[]{imageRecord});
        } catch (Exception e) {
            Log.e("NFC", "创建 NDEF 消息失败", e);
            return null;
        }
    }

    // 辅助方法：将 Drawable 转换为 Bitmap
    private android.graphics.Bitmap drawableToBitmap(android.graphics.drawable.Drawable drawable) {
        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            return ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
        }

        // 创建空的 Bitmap 并将 Drawable 绘制到其中
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // 检测接收到的NFC数据
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage[] messages = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }

                // 处理接收到的NDEF消息
                if (messages.length > 0) {
                    NdefRecord record = messages[0].getRecords()[0];
                    if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA) {
                        byte[] payload = record.getPayload();

                        // 将字节数组转换为图片
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(payload, 0, payload.length);
                        imageView.setImageBitmap(bitmap);

                        Toast.makeText(this, "图片传输完成！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}

