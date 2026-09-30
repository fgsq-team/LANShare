package com.fgsqw.lanshare.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MKVtoMP4Converter {


    public static void convertMKVtoMP4(String inputPath, String outputPath) {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaExtractor subtitleExtractor = new MediaExtractor();
        MediaMuxer muxer = null;

        try {
            videoExtractor.setDataSource(inputPath);
            audioExtractor.setDataSource(inputPath);
            subtitleExtractor.setDataSource(inputPath);

            int videoTrackIndex = selectTrack(videoExtractor, "video/");
//            int audioTrackIndex = selectTrack(audioExtractor, "audio/");
            int subtitleTrackIndex = selectTrack(subtitleExtractor, "application/x-subrip");

//            if (videoTrackIndex == -1 /*|| audioTrackIndex == -1*/) {
//                // No video or audio track found
//                return;
//            }

            videoExtractor.selectTrack(videoTrackIndex);
            MediaFormat videoInputFormat = videoExtractor.getTrackFormat(videoTrackIndex);

//            audioExtractor.selectTrack(audioTrackIndex);
//            MediaFormat audioInputFormat = audioExtractor.getTrackFormat(audioTrackIndex);

            subtitleExtractor.selectTrack(subtitleTrackIndex);
            MediaFormat subtitleInputFormat = subtitleExtractor.getTrackFormat(subtitleTrackIndex);

            // Create a MediaMuxer
            muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Add video, audio, and subtitle tracks to the muxer
            int outputVideoTrackIndex = muxer.addTrack(videoInputFormat);
//            int outputAudioTrackIndex = muxer.addTrack(audioInputFormat);
            int outputSubtitleTrackIndex = muxer.addTrack(subtitleInputFormat);
            muxer.start();

            // Read and write video data
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // Adjust buffer size as needed
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int sampleSize;

            while ((sampleSize = videoExtractor.readSampleData(buffer, 0)) >= 0) {
                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                bufferInfo.flags = videoExtractor.getSampleFlags();
                muxer.writeSampleData(outputVideoTrackIndex, buffer, bufferInfo);
                videoExtractor.advance();
            }

            // Read and write audio data
//            while ((sampleSize = audioExtractor.readSampleData(buffer, 0)) >= 0) {
//                bufferInfo.size = sampleSize;
//                bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
//                bufferInfo.flags = audioExtractor.getSampleFlags();
//                muxer.writeSampleData(outputAudioTrackIndex, buffer, bufferInfo);
//                audioExtractor.advance();
//            }

            // Read and write subtitle data
            while ((sampleSize = subtitleExtractor.readSampleData(buffer, 0)) >= 0) {
                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs = subtitleExtractor.getSampleTime();
                bufferInfo.flags = subtitleExtractor.getSampleFlags();
                muxer.writeSampleData(outputSubtitleTrackIndex, buffer, bufferInfo);
                subtitleExtractor.advance();
            }

            // Stop and release resources
            muxer.stop();
            muxer.release();
            videoExtractor.release();
            audioExtractor.release();
            subtitleExtractor.release();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure resources are released in case of an exception
            if (muxer != null) {
                muxer.stop();
                muxer.release();
            }
            videoExtractor.release();
            audioExtractor.release();
            subtitleExtractor.release();
        }
    }

    private static int selectTrack(MediaExtractor extractor, String mimeTypePrefix) {
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeTypePrefix)) {
                return i;
            }
        }
        return -1; // No track found
    }

    public static void convertMKVtoMP41(String inputPath, String outputPath) {
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;

        try {
            extractor.setDataSource(inputPath);
            int trackCount = extractor.getTrackCount();

            // Find and select the video track
            int videoTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            // Find and select a supported audio track
            int audioTrackIndex = selectSupportedAudioTrack(extractor);

            if (videoTrackIndex == -1 || audioTrackIndex == -1) {
                // No video or audio track found
                return;
            }

            extractor.selectTrack(videoTrackIndex);
            MediaFormat videoInputFormat = extractor.getTrackFormat(videoTrackIndex);

            extractor.selectTrack(audioTrackIndex);
            MediaFormat audioInputFormat = extractor.getTrackFormat(audioTrackIndex);

            // Create a MediaMuxer
            muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Add video and audio tracks to the muxer
            int outputVideoTrackIndex = muxer.addTrack(videoInputFormat);
            int outputAudioTrackIndex = muxer.addTrack(audioInputFormat);
            muxer.start();

            // Read and write video data
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // Adjust buffer size as needed
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int sampleSize;

            while ((sampleSize = extractor.readSampleData(buffer, 0)) >= 0) {
                int trackIndex = extractor.getSampleTrackIndex();
                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                bufferInfo.flags = extractor.getSampleFlags();
                muxer.writeSampleData(trackIndex == videoTrackIndex ? outputVideoTrackIndex : outputAudioTrackIndex, buffer, bufferInfo);
                extractor.advance();
            }

            // Stop and release resources
            muxer.stop();
            muxer.release();
            extractor.release();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure resources are released in case of an exception
            if (muxer != null) {
                muxer.stop();
                muxer.release();
            }
            extractor.release();
        }
    }

    private static int selectSupportedAudioTrack(MediaExtractor extractor) {
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                // Check if the device supports this audio format
//                if (isAudioFormatSupported(format)) {
                    return i;
//                }
            }
        }
        return -1; // No supported audio track found
    }

    private static boolean isAudioFormatSupported(MediaFormat format) {
        // Check if the device supports the given audio format
        // You may need to customize this based on the supported audio formats on your device
        // Example: Check for support of AAC
        return "audio/mp4a-latm".equals(format.getString(MediaFormat.KEY_MIME));
    }
}