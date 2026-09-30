package com.fgsqw.lanshare.activity;


import android.app.Activity;
import android.media.*;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.fgsqw.lanshare.R;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "VideoActivity";

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private MediaExtractor extractor;
    private MediaCodec videoCodec;
    private MediaCodec audioCodec;

    private int videoTrackIndex;
    private int audioTrackIndex;

    private int videoFrameRate;
    private int audioSampleRate;
    private int audioChannelCount;

    private AudioTrack audioTrack;

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
        initializeDecoder();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface发生变化时的处理（如横竖屏切换）
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopDecoding();
    }

    private void initializeDecoder() {
        try {
            String videoFilePath = Environment.getExternalStorageDirectory() + "/a.mkv";
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFilePath);

            // 寻找视频轨道和音频轨道
            videoTrackIndex = -1;
            audioTrackIndex = -1;

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
//                    videoFrameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    videoFrameRate = 30;
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    audioChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                }

                if (videoTrackIndex != -1 && audioTrackIndex != -1) {
                    break;
                }
            }

            if (videoTrackIndex == -1 || audioTrackIndex == -1) {
                // 没有找到视频或音频轨道
                return;
            }

            // 选择视频轨道
            extractor.selectTrack(videoTrackIndex);

            // 创建视频解码器并配置
            videoCodec = MediaCodec.createDecoderByType(extractor.getTrackFormat(videoTrackIndex).getString(MediaFormat.KEY_MIME));
            videoCodec.configure(extractor.getTrackFormat(videoTrackIndex), surfaceHolder.getSurface(), null, 0);
            videoCodec.start();

            // 选择音频轨道
            extractor.selectTrack(audioTrackIndex);

            // 创建音频解码器并配置
            String audioMime = extractor.getTrackFormat(audioTrackIndex).getString(MediaFormat.KEY_MIME);
            if (isCodecSupported(audioMime)) {
                audioCodec = MediaCodec.createDecoderByType(audioMime);
                audioCodec.configure(extractor.getTrackFormat(audioTrackIndex), null, null, 0);
                audioCodec.start();
            } else {
                Log.e(TAG, "音频编解码器不受支持，使用备用音频编解码器");
                // 选择备用音频编解码器，例如AAC
                String alternativeAudioCodec = "audio/mp4a-latm"; // AAC
                audioCodec = MediaCodec.createDecoderByType(alternativeAudioCodec);
                audioCodec.configure(extractor.getTrackFormat(audioTrackIndex), null, null, 0);
                audioCodec.start();
            }

            // 创建音频播放器
            createAudioTrack();

            // 开始解码
            startDecoding();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isCodecSupported(String mimeType) {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();

        for (MediaCodecInfo codecInfo : codecInfos) {
            String[] supportedTypes = codecInfo.getSupportedTypes();
            for (String supportedType : supportedTypes) {
                if (supportedType.equalsIgnoreCase(mimeType)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void createAudioTrack() {
        int channelConfig = (audioChannelCount == 1) ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                audioSampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
        );

        audioTrack.play();
    }

    private void startDecoding() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (true) {
                    // 解码视频
                    decodeVideo(info);

                    // 解码音频
                    decodeAudio(info);

//                    if (isVideoEOS() && isAudioEOS()) {
//                        // 视频和音频都结束了，退出循环
//                        break;
//                    }
                }
            }
        }).start();
    }

    private void decodeVideo(MediaCodec.BufferInfo info) {
        int inputBufferIndex = videoCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            // 将数据填充到输入缓冲区
            int sampleSize = extractor.readSampleData(videoCodec.getInputBuffer(inputBufferIndex), 0);
            if (sampleSize < 0) {
                // 输入结束
                videoCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                videoCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }

        int outputBufferIndex = videoCodec.dequeueOutputBuffer(info, 10000);
        if (outputBufferIndex >= 0) {
            // 将解码后的数据渲染到Surface
            videoCodec.releaseOutputBuffer(outputBufferIndex, true);
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // 格式变化，可能是视频尺寸变化等
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            // 输出缓冲区变化
        }
    }

    private void decodeAudio(MediaCodec.BufferInfo info) {
        int inputBufferIndex = audioCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            // 将数据填充到输入缓冲区
            int sampleSize = extractor.readSampleData(audioCodec.getInputBuffer(inputBufferIndex), 0);
            if (sampleSize < 0) {
                // 输入结束
                audioCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                audioCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }

        int outputBufferIndex = audioCodec.dequeueOutputBuffer(info, 10000);
        if (outputBufferIndex >= 0) {
            // 将解码后的音频数据写入AudioTrack
            ByteBuffer outputBuffer = audioCodec.getOutputBuffer(outputBufferIndex);
            if (outputBuffer != null) {
                byte[] audioData = new byte[info.size];
                outputBuffer.get(audioData);
                audioTrack.write(audioData, 0, audioData.length);
                audioCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // 格式变化，可能是音频格式变化等
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            // 输出缓冲区变化
        }
    }

    private boolean isVideoEOS() {
        return (videoCodec == null || (videoCodec.getOutputFormat() == null));
    }

    private boolean isAudioEOS() {
        return (audioCodec == null || (audioCodec.getOutputFormat() == null));
    }

    private void stopDecoding() {
        if (videoCodec != null) {
            videoCodec.stop();
            videoCodec.release();
        }
        if (audioCodec != null) {
            audioCodec.stop();
            audioCodec.release();
        }
        if (extractor != null) {
            extractor.release();
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
    }
}

