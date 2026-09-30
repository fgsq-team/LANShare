package com.fgsqw.lanshare.activity;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.fgsqw.lanshare.R;

import java.io.IOException;

public class VideoActivity1 extends Activity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private MediaExtractor extractor;
    private MediaCodec codec;

    private int videoTrackIndex;
    private int frameRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Surface已经创建，可以进行解码的初始化操作
        initializeDecoder();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface发生变化时的处理（如横竖屏切换）
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface销毁时的处理
        stopDecoding();
    }

    private void initializeDecoder() {
        try {
            // 替换成您的MKV视频文件路径
            String videoFilePath = Environment.getExternalStorageDirectory() + "/a.mkv";
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFilePath);

            // 寻找视频轨道
            videoTrackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    try {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    } catch (NullPointerException e) {
                        frameRate = 30;
                    }
                    break;
                }
            }

            if (videoTrackIndex == -1) {
                // 没有找到视频轨道
                return;
            }

            // 选择视频轨道
            extractor.selectTrack(videoTrackIndex);

            // 创建MediaCodec并配置
            codec = MediaCodec.createDecoderByType(extractor.getTrackFormat(videoTrackIndex).getString(MediaFormat.KEY_MIME));
            codec.configure(extractor.getTrackFormat(videoTrackIndex), surfaceHolder.getSurface(), null, 0);
            codec.start();

            // 开始解码
            startDecoding();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startDecoding() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (true) {
                    int inputBufferIndex = codec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        // 将数据填充到输入缓冲区
                        int sampleSize = extractor.readSampleData(codec.getInputBuffer(inputBufferIndex), 0);
                        if (sampleSize < 0) {
                            // 输入结束
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            break; // 退出解码循环
                        } else {
                            long sampleTime = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0);
                            extractor.advance();
                        }
                    }

                    int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                    if (outputBufferIndex >= 0) {
                        // 将解码后的数据渲染到Surface
                        codec.releaseOutputBuffer(outputBufferIndex, true);

                        // 调整播放速度
                        adjustPlaybackSpeed();
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // 格式变化，可能是视频尺寸变化等
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // 输出缓冲区变化
                    }
                }
            }
        }).start();
    }

    private void adjustPlaybackSpeed() {
        try {
            // 根据视频帧率调整播放速度
            int sleepTime = (int) (1000 / (float) frameRate);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopDecoding() {
        if (codec != null) {
            codec.stop();
            codec.release();
        }
        if (extractor != null) {
            extractor.release();
        }
    }
}


