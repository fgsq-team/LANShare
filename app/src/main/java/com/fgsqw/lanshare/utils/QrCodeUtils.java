package com.fgsqw.lanshare.utils;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

public class QrCodeUtils {

    public static Bitmap qrcode(String content, int width, int height) {
        //HashMap设置二维码参数
        Map<EncodeHintType, Object> map = new HashMap<>();
        //   设置容错率 L>M>Q>H  等级越高扫描时间越长,准确率越高
        map.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        //设置字符集（必须传字符集名称字符串；ZXing 内部会对其 toString()，
        //   直接传 Charset 对象在 Android 4.4 上会得到 "java.nio.charset.CharsetICU[UTF-8]" 导致 UnsupportedEncodingException）
        map.put(EncodeHintType.CHARACTER_SET, FileUtil.UTF_8.name());
        //设置外边距
        map.put(EncodeHintType.MARGIN, 1);
        //利用编码器，生成二维码
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = null;
        try {
            bitmap = barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, width, height, map);
        } catch (WriterException e) {
            LLog.error(e);
        }
        return bitmap;
    }
}
