package com.fgsqw.lanshare.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.io.File;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static MusicService instance = null;
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MusicBinder();
    private String musicFilePath;
    private MusicProgressListener progressListener;
    private static final int UPDATE_INTERVAL = 1000; // 更新进度的时间间隔，单位毫秒
    private String name;

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        startProgressUpdate();
        int duration = mediaPlayer.getDuration();
        if (progressListener != null) {
            File file = new File(musicFilePath);
            progressListener.onPlay(file.getName(), timeParse(duration), duration);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (progressListener != null) {
            progressListener.onCompletion();
        }
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public static MusicService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            musicFilePath = intent.getStringExtra("musicFilePath");
            if (musicFilePath != null) {
                play(musicFilePath);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (mediaPlayer != null) {
            stopMusic();
        }
    }

    // 播放音乐的方法
    public void play(String filePath) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        musicFilePath = filePath;
        File file = new File(musicFilePath);
        name = file.getName();
        startMusic();
    }

    // 暂停音乐的方法
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        if (progressListener != null) {
            progressListener.onPause();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            startProgressUpdate();
        }
        if (progressListener != null) {
            progressListener.onPlay(getName(), getTotalTime(), getDuration());
        }
    }

    public String getTotalTime() {
        return timeParse(getDuration());
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public String getName() {
        return name;
    }

    public void seek(int seek) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(seek);
        }
    }

    // 开始播放音乐
    private void startMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            mediaPlayer.reset();
        }
        try {
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setDataSource(musicFilePath);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 停止音乐
    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // 开始更新播放进度
    private void startProgressUpdate() {
        ThreadUtils.runThread(() -> {
            while (mediaPlayer != null && mediaPlayer.isPlaying()) {
                try {
                    if (progressListener != null) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        String startTime = timeParse(currentPosition);
                        progressListener.onProgressChanged(currentPosition, startTime);
                    }
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static String timeParse(long duration) {
        String time = "";
        long minute = duration / 60000;
        long seconds = duration % 60000;
        long second = Math.round((float) seconds / 1000);
        if (minute < 10) {
            time += "0";
        }
        time += minute + ":";
        if (second < 10) {
            time += "0";
        }
        time += second;
        return time;
    }

    // 注册播放进度监听器
    public void setProgressListener(MusicProgressListener listener) {
        progressListener = listener;
    }

    // 回调接口用于通知活动更新播放进度
    public interface MusicProgressListener {
        void onPlay(String name, String endTime, int duration);

        void onPause();

        void onCompletion();

        void onProgressChanged(int progress, String time);
    }
}
