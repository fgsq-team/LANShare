package com.fgsqw.lanshare.utils;


import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.SystemClock;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraUtils  {
    Camera camera;
    private final int mPreviewFormat;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private Rect mPreviewRect;
    private final int mJpegQuality = 80;

    ByteArrayOutputStream mJpegOutputStream;
    JpegStream mMJpegStream = null;


    private final Camera.PreviewCallback mPreviewCallback = (data, camera) -> {
        Long timestamp = SystemClock.elapsedRealtime();
        // 旋转90度
//        byte[] bytes = rotateYUV420Degree(data, mPreviewWidth, mPreviewHeight, 270);
        sendPreviewFrame(data, camera, timestamp);
    };


    private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp) {
        final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight, null);
        image.compressToJpeg(mPreviewRect, mJpegQuality, mJpegOutputStream);
        mMJpegStream.streamJpeg(mJpegOutputStream.toByteArray(), mJpegOutputStream.size(), timestamp);
        mJpegOutputStream.reset();
        camera.addCallbackBuffer(data);
    }


    public CameraUtils(SurfaceHolder holder, int port) {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        //获取相机参数
        Camera.Parameters parameters = camera.getParameters();
        //获取相机支持的预览的大小
//        Camera.Size previewSize = getCameraPreviewSize(parameters);
//        mPreviewWidth = previewSize.width;
//        mPreviewHeight = previewSize.height;

        mPreviewWidth = 640;
        mPreviewHeight = 480;

        //设置预览图像分辨率
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
        //设置预览格式（也就是每一帧的视频格式）YUV420下的NV21
        parameters.setPreviewFormat(ImageFormat.NV21);
        mPreviewFormat = parameters.getPreviewFormat();

        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE;
        // 缓冲区大小计算 width * height  * bytesPerPixel 但是有时候还是容量不够 还需要增加1.5倍容量才正好
        int mPreviewBufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        camera.setPreviewCallbackWithBuffer(mPreviewCallback);

        mJpegOutputStream = new ByteArrayOutputStream(mPreviewBufferSize);

        mMJpegStream = new JpegStream(port, mPreviewBufferSize);
        mMJpegStream.start();

        //相机旋转90度
        camera.setDisplayOrientation(90);
        //配置camera参数
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }


    /**
     * 获取设备支持的最大分辨率
     */
    private Camera.Size getCameraPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size needSize = null;
        for (Camera.Size size : list) {
            if (needSize == null) {
                needSize = size;
                continue;
            }
            if (size.width >= needSize.width) {
                if (size.height > needSize.height) {
                    needSize = size;
                }
            }
        }
        return needSize;
    }


    public static byte[] rotateYUV420Degree(byte[] input, int width, int height, int rotation) {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;
        byte[] output = new byte[frameSize + 2 * qFrameSize];

        boolean swap = (rotation == 90 || rotation == 270);
        boolean yflip = (rotation == 90 || rotation == 180);
        boolean xflip = (rotation == 270 || rotation == 180);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                if (swap) {
                    xi = w * yo / h;
                    yi = h * xo / w;
                }
                if (yflip) {
                    yi = h - yi - 1;
                }
                if (xflip) {
                    xi = w - xi - 1;
                }
                output[w * yo + xo] = input[w * yi + xi];
                int fs = w * h;
                xi = (xi >> 1);
                yi = (yi >> 1);
                xo = (xo >> 1);
                yo = (yo >> 1);
                w = (w >> 1);
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + (w * yo + xo) * 2;
                int vi = ui + 1;
                int vo = uo + 1;
                output[uo] = input[ui];
                output[vo] = input[vi];
            }
        }
        return output;
    }


}
