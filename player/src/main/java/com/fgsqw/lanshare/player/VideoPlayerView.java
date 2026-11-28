package com.fgsqw.lanshare.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * LANShare 内置视频播放控件（系统 MediaPlayer + SurfaceView）。
 * <p>
 * 对外接口：{@link #setDataSource(String, String)}、{@link #playVideo(String, String)}、
 * {@link #startVideo()}、{@link #pauseVideo()}、{@link #isFullScreen()}、
 * {@link #setOrientationPortrait()}、{@link #setOrientationLandscape()}、
 * {@link #destroyVideo()} 等。
 */
public class VideoPlayerView extends FrameLayout implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, SurfaceHolder.Callback,
        GestureDetector.OnGestureListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "VideoPlayerView";

    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_PROGRESS = 1;
    private static final int GESTURE_VOLUME = 2;
    private static final int GESTURE_BRIGHTNESS = 3;
    private static final float STEP_PROGRESS = 2f;
    private static final float STEP_VOLUME = 2f;
    private static final float STEP_LIGHT = 2f;
    private static final long HIDE_CONTROLLER_DELAY = 5000L;
    private static final long PROGRESS_UPDATE_INTERVAL = 1000L;

    private final Context context;
    private final Activity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;

    private boolean isPlaying = true;
    private boolean isFirstPlay;
    private boolean isPrepare;
    private boolean isFullscreen;
    private boolean isLockScreen;
    private boolean isNeedBatteryListen = true;
    private boolean isNeedNetChangeListen = true;

    private String videoPath;
    private String videoTitle;
    private int videoPosition = 0;

    private int playerViewW;
    private int playerViewH;

    // 控件
    private LinearLayout surfaceHolder0;
    private SurfaceView surfaceView;
    private ImageView thumbnail;
    private RelativeLayout statusLayer;
    private LoadingRing loading;
    private ImageView centerPlay;
    private LinearLayout errorLayer;
    private TextView errorText;
    private LinearLayout noNetLayer;
    private RelativeLayout topBar;
    private RelativeLayout bottomBar;
    private ImageView backView;
    private TextView clockView;
    private BatteryView batteryView;
    private TextView titleView;
    private ImageView toggleView;
    private ImageView fullscreenView;
    private TextView timeText;
    private SeekBar seekBar;
    private ImageView lockView;

    // 手势
    private RelativeLayout gestureVolumeLayer;
    private ImageView gestureVolumeIcon;
    private ProgressBar gestureVolumeBar;
    private RelativeLayout gestureLightLayer;
    private ImageView gestureLightIcon;
    private ProgressBar gestureLightBar;
    private RelativeLayout gestureSeekLayer;
    private ImageView gestureSeekIcon;
    private TextView gestureSeekText;
    private GestureDetector gestureDetector;
    private AudioManager audioManager;
    private int maxVolume;
    private int currentVolume;
    private int gestureFlag = GESTURE_NONE;

    // 回调
    private NetworkListener networkListener;
    private CompletionListener completionListener;
    private OrientationListener orientationListener;

    private BatteryReceiver batteryReceiver;
    private NetChangeReceiver netChangeReceiver;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer == null) {
                return;
            }
            try {
                int current = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                timeText.setText(VideoHelper.formatTime(current) + " / "
                        + VideoHelper.formatTime(duration));
                seekBar.setProgress(current);
            } catch (IllegalStateException e) {
                Log.w(TAG, "progress update failed", e);
            }
            mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL);
        }
    };

    private final Runnable hideControllerRunnable = this::dismissControllerMenu;

    private final Runnable dismissLockRunnable = this::dismissLockButton;

    public VideoPlayerView(Context context) {
        this(context, null);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.activity = (Activity) context;
        VideoHelper.hideSystemBars(activity);
        initAttrs(context, attrs);
        init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LanPlayer);
        isFirstPlay = typedArray.getBoolean(R.styleable.LanPlayer_lpFirstPlay, false);
        isNeedBatteryListen = typedArray.getBoolean(R.styleable.LanPlayer_lpNeedBattery, true);
        isNeedNetChangeListen = typedArray.getBoolean(R.styleable.LanPlayer_lpNeedNetwork, true);
        typedArray.recycle();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        View root = View.inflate(context, R.layout.player_video_view, this);

        surfaceHolder0 = root.findViewById(R.id.lp_surface_holder);
        surfaceView = root.findViewById(R.id.lp_surface);
        thumbnail = root.findViewById(R.id.lp_thumbnail);
        statusLayer = root.findViewById(R.id.lp_status_layer);
        loading = root.findViewById(R.id.lp_loading);
        centerPlay = root.findViewById(R.id.lp_center_play);
        errorLayer = root.findViewById(R.id.lp_error_layer);
        errorText = root.findViewById(R.id.lp_error_text);
        noNetLayer = root.findViewById(R.id.lp_no_net_layer);
        topBar = root.findViewById(R.id.lp_top_bar);
        bottomBar = root.findViewById(R.id.lp_bottom_bar);
        backView = root.findViewById(R.id.lp_back);
        clockView = root.findViewById(R.id.lp_clock);
        batteryView = root.findViewById(R.id.lp_battery);
        titleView = root.findViewById(R.id.lp_title);
        toggleView = root.findViewById(R.id.lp_toggle);
        fullscreenView = root.findViewById(R.id.lp_fullscreen);
        timeText = root.findViewById(R.id.lp_time);
        seekBar = root.findViewById(R.id.lp_seek);
        lockView = root.findViewById(R.id.lp_lock);

        gestureVolumeLayer = root.findViewById(R.id.lp_gesture_volume);
        gestureVolumeIcon = root.findViewById(R.id.lp_gesture_volume_icon);
        gestureVolumeBar = root.findViewById(R.id.lp_gesture_volume_bar);
        gestureLightLayer = root.findViewById(R.id.lp_gesture_light);
        gestureLightIcon = root.findViewById(R.id.lp_gesture_light_icon);
        gestureLightBar = root.findViewById(R.id.lp_gesture_light_bar);
        gestureSeekLayer = root.findViewById(R.id.lp_gesture_seek);
        gestureSeekIcon = root.findViewById(R.id.lp_gesture_seek_icon);
        gestureSeekText = root.findViewById(R.id.lp_gesture_seek_text);

        seekBar.setOnSeekBarChangeListener(this);
        toggleView.setOnClickListener(this);
        fullscreenView.setOnClickListener(this);
        backView.setOnClickListener(this);
        lockView.setOnClickListener(this);
        centerPlay.setOnClickListener(this);
        errorLayer.setOnClickListener(this);
        noNetLayer.setOnClickListener(this);

        initViews();

        if (!isFirstPlay) {
            centerPlay.setVisibility(VISIBLE);
            loading.setVisibility(GONE);
        }

        initSurfaceView();
        initGesture();

        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (playerViewW == 0) {
                playerViewW = getWidth();
                playerViewH = getHeight();
            }
        });
    }

    private void initViews() {
        surfaceView.setAlpha(0);
        clockView.setText(VideoHelper.nowClock());
        topBar.setVisibility(GONE);
        bottomBar.setVisibility(GONE);
        lockView.setVisibility(GONE);
        statusLayer.setVisibility(VISIBLE);
        loading.setVisibility(VISIBLE);
        centerPlay.setVisibility(GONE);
        errorLayer.setVisibility(GONE);
        noNetLayer.setVisibility(GONE);
        thumbnail.setVisibility(GONE);
        titleView.setText(videoTitle);
    }

    private void initSurfaceView() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(this);
    }

    private void initGesture() {
        gestureVolumeLayer.setVisibility(GONE);
        gestureSeekLayer.setVisibility(GONE);
        gestureLightLayer.setVisibility(GONE);

        gestureDetector = new GestureDetector(context, this);
        setLongClickable(true);
        gestureDetector.setIsLongpressEnabled(true);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void setVideoThumbnail() {
        if (!VideoHelper.isOnline(context)) {
            return;
        }
        new Thread(() -> {
            final Bitmap bmp = VideoHelper.createThumbnail(videoPath, getWidth(), getHeight());
            mainHandler.post(() -> {
                if (bmp != null) {
                    thumbnail.setVisibility(VISIBLE);
                    thumbnail.setImageBitmap(bmp);
                } else {
                    thumbnail.setVisibility(GONE);
                }
            });
        }).start();
    }

    // ------------------------------------------------------------------
    // 控制器显隐
    // ------------------------------------------------------------------

    private void showControllerMenu() {
        clockView.setText(VideoHelper.nowClock());
        topBar.setVisibility(VISIBLE);
        bottomBar.setVisibility(VISIBLE);
        if (isFullscreen) {
            lockView.setVisibility(VISIBLE);
        }
        startProgressTask();
        scheduleHideController();
    }

    private void toggleControllerMenu() {
        if (bottomBar.getVisibility() == VISIBLE) {
            dismissControllerMenu();
        } else {
            showControllerMenu();
        }
    }

    private void dismissControllerMenu() {
        topBar.setVisibility(GONE);
        bottomBar.setVisibility(GONE);
        if (!isLockScreen) {
            lockView.setVisibility(GONE);
        }
        stopProgressTask();
    }

    /** 锁定状态下单击任意处：仅显示锁定按钮，点它才解锁。 */
    private void showLockButton() {
        stopProgressTask();
        mainHandler.removeCallbacks(hideControllerRunnable);
        topBar.setVisibility(GONE);
        bottomBar.setVisibility(GONE);
        lockView.setVisibility(VISIBLE);
        mainHandler.removeCallbacks(dismissLockRunnable);
        mainHandler.postDelayed(dismissLockRunnable, HIDE_CONTROLLER_DELAY);
    }

    private void dismissLockButton() {
        if (!isLockScreen) {
            return;
        }
        lockView.setVisibility(GONE);
    }

    private void scheduleHideController() {
        mainHandler.removeCallbacks(hideControllerRunnable);
        mainHandler.postDelayed(hideControllerRunnable, HIDE_CONTROLLER_DELAY);
    }

    private void startProgressTask() {
        mainHandler.removeCallbacks(progressRunnable);
        mainHandler.post(progressRunnable);
    }

    private void stopProgressTask() {
        mainHandler.removeCallbacks(progressRunnable);
    }

    // ------------------------------------------------------------------
    // 横竖屏
    // ------------------------------------------------------------------

    private void setLandscape() {
        isFullscreen = true;
        fullscreenView.setImageResource(R.drawable.lp_ic_fullscreen_exit);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (bottomBar.getVisibility() == VISIBLE) {
            lockView.setVisibility(VISIBLE);
        }
    }

    private void setPortrait() {
        if (isFullscreen) {
            isFullscreen = false;
            fullscreenView.setImageResource(R.drawable.lp_ic_fullscreen);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            unLockScreen();
            lockView.setVisibility(GONE);
        } else {
            activity.finish();
        }
    }

    private void lockScreen() {
        isLockScreen = true;
        lockView.setImageResource(R.drawable.lp_ic_lock_closed);
        showLockButton();
    }

    private void unLockScreen() {
        isLockScreen = false;
        lockView.setImageResource(R.drawable.lp_ic_lock_open);
        mainHandler.removeCallbacks(dismissLockRunnable);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int screenWidth = VideoHelper.screenWidth(activity);
        int screenHeight = VideoHelper.screenHeight(activity);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                layoutParams.width = screenWidth;
                layoutParams.height = screenHeight;
                if (orientationListener != null) {
                    orientationListener.onPortrait();
                }
            } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                layoutParams.width = screenWidth;
                layoutParams.height = screenHeight;
                if (orientationListener != null) {
                    orientationListener.onLandscape();
                }
            }
            setLayoutParams(layoutParams);
            playerViewW = screenWidth;
            playerViewH = layoutParams.height;
            fitVideoSize();
        }
    }

    // ------------------------------------------------------------------
    // 点击
    // ------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.lp_toggle) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    pauseVideo();
                } else {
                    startVideo();
                }
            }
        } else if (id == R.id.lp_fullscreen) {
            if (isFullscreen) {
                setPortrait();
            } else {
                setLandscape();
            }
        } else if (id == R.id.lp_back) {
            setPortrait();
        } else if (id == R.id.lp_lock) {
            if (isLockScreen) {
                unLockScreen();
                showControllerMenu();
            } else {
                lockScreen();
            }
        } else if (id == R.id.lp_error_layer || id == R.id.lp_no_net_layer
                || id == R.id.lp_center_play) {
            playVideo(videoPath, videoTitle, videoPosition);
        }
    }

    // ------------------------------------------------------------------
    // SeekBar
    // ------------------------------------------------------------------

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mainHandler.removeCallbacks(hideControllerRunnable);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int maxCanSeekTo = seekBar.getMax() - 5 * 1000;
            int target = Math.min(seekBar.getProgress(), Math.max(maxCanSeekTo, 0));
            mediaPlayer.seekTo(target);
        }
        scheduleHideController();
    }

    // ------------------------------------------------------------------
    // SurfaceHolder.Callback
    // ------------------------------------------------------------------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDisplay(holder);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        if (isFirstPlay) {
            playVideo(videoPath, videoTitle);
        }
        isFirstPlay = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (IllegalStateException ignored) {
            }
            toggleView.setImageResource(R.drawable.lp_ic_play);
            videoPosition = safeCurrentPosition();
        }
        dismissControllerMenu();
    }

    // ------------------------------------------------------------------
    // MediaPlayer 回调
    // ------------------------------------------------------------------

    @Override
    public void onCompletion(MediaPlayer mp) {
        toggleView.setImageResource(R.drawable.lp_ic_play);
        isPlaying = false;
        if (!isLockScreen) {
            dismissControllerMenu();
        }
        videoPosition = 0;
        if (completionListener != null) {
            completionListener.onCompletion(mp);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (percent >= 0 && percent <= 100 && seekBar != null) {
            int secondProgress = mp.getDuration() * percent / 100;
            seekBar.setSecondaryProgress(secondProgress);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what != -38) {
            showErrorView(getContext().getString(R.string.lp_loading_exception));
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        if (!isPlaying) {
            mp.pause();
            toggleView.setImageResource(R.drawable.lp_ic_play);
        } else {
            toggleView.setImageResource(R.drawable.lp_ic_pause);
        }
        isPrepare = true;
        seekBar.setMax(mp.getDuration());
        timeText.setText(VideoHelper.formatTime(mp.getCurrentPosition())
                + " / " + VideoHelper.formatTime(mp.getDuration()));
        statusLayer.setVisibility(GONE);
        showControllerMenu();
        if (videoPosition > 0) {
            mp.seekTo(videoPosition);
            videoPosition = 0;
        }
        fitVideoSize();
        surfaceView.setAlpha(1);
        thumbnail.setVisibility(GONE);
    }

    private void fitVideoSize() {
        if (mediaPlayer == null) {
            return;
        }
        if (playerViewW == 0) {
            playerViewW = getWidth();
            playerViewH = getHeight();
        }
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        int parentWidth = playerViewW;
        int parentHeight = playerViewH;
        if (videoWidth == 0 || videoHeight == 0 || parentWidth == 0 || parentHeight == 0) {
            return;
        }
        int surfaceW;
        int surfaceH;
        if ((float) videoWidth / (float) videoHeight > (float) parentWidth / (float) parentHeight) {
            surfaceW = parentWidth;
            surfaceH = videoHeight * surfaceW / videoWidth;
        } else {
            surfaceH = parentHeight;
            surfaceW = videoWidth * parentHeight / videoHeight;
        }
        ViewGroup.LayoutParams params = surfaceHolder0.getLayoutParams();
        params.width = surfaceW;
        params.height = surfaceH;
        surfaceHolder0.setLayoutParams(params);
    }

    // ------------------------------------------------------------------
    // 手势
    // ------------------------------------------------------------------

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (!isPrepare) {
            return false;
        }
        if (isLockScreen) {
            showLockButton();
            return true;
        }
        toggleControllerMenu();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!isPrepare || isLockScreen || e1 == null || mediaPlayer == null) {
            return false;
        }

        int flag;
        if (Math.abs(distanceX) >= Math.abs(distanceY)) {
            flag = mediaPlayer.isPlaying() ? GESTURE_PROGRESS : GESTURE_NONE;
        } else {
            int screenWidth = VideoHelper.screenWidth(activity);
            flag = e1.getX() > screenWidth / 2 ? GESTURE_VOLUME : GESTURE_BRIGHTNESS;
        }

        if (gestureFlag != GESTURE_NONE && gestureFlag != flag) {
            return false;
        }
        gestureFlag = flag;

        if (flag == GESTURE_PROGRESS) {
            gestureVolumeLayer.setVisibility(GONE);
            gestureLightLayer.setVisibility(GONE);
            gestureSeekLayer.setVisibility(VISIBLE);
            try {
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    if (distanceX >= VideoHelper.dip2px(context, STEP_PROGRESS)) {
                        gestureSeekIcon.setImageResource(R.drawable.lp_ic_backward);
                        if (mediaPlayer.getCurrentPosition() > 3 * 1000) {
                            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 3000);
                        } else {
                            mediaPlayer.seekTo(3000);
                        }
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    } else if (distanceX <= -VideoHelper.dip2px(context, STEP_PROGRESS)) {
                        gestureSeekIcon.setImageResource(R.drawable.lp_ic_forward);
                        if (mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration() - 5 * 1000) {
                            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 3000);
                            seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                    }
                }
                gestureSeekText.setText(VideoHelper.formatTime(mediaPlayer.getCurrentPosition())
                        + " / " + VideoHelper.formatTime(mediaPlayer.getDuration()));
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else if (flag == GESTURE_VOLUME) {
            gestureVolumeLayer.setVisibility(VISIBLE);
            gestureLightLayer.setVisibility(GONE);
            gestureSeekLayer.setVisibility(GONE);
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (Math.abs(distanceY) > Math.abs(distanceX)) {
                if (distanceY >= VideoHelper.dip2px(context, STEP_VOLUME)) {
                    if (currentVolume < maxVolume) {
                        currentVolume++;
                    }
                } else if (distanceY <= -VideoHelper.dip2px(context, STEP_VOLUME)) {
                    if (currentVolume > 0) {
                        currentVolume--;
                    }
                }
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
            int percentage = maxVolume == 0 ? 0 : (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume;
            gestureVolumeBar.setProgress(percentage);
            gestureVolumeIcon.setImageResource(
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                            ? R.drawable.lp_ic_volume_off : R.drawable.lp_ic_volume_up);
        } else if (flag == GESTURE_BRIGHTNESS) {
            gestureVolumeLayer.setVisibility(GONE);
            gestureLightLayer.setVisibility(VISIBLE);
            gestureSeekLayer.setVisibility(GONE);
            if (Math.abs(distanceY) > Math.abs(distanceX)) {
                if (distanceY >= VideoHelper.dip2px(context, STEP_LIGHT)) {
                    VideoHelper.adjustBrightness(activity, 10);
                } else if (distanceY <= -VideoHelper.dip2px(context, STEP_LIGHT)) {
                    VideoHelper.adjustBrightness(activity, -10);
                }
            }
            int currentLight = VideoHelper.brightness(activity);
            gestureLightBar.setProgress((currentLight * 100) / 255);
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            gestureFlag = GESTURE_NONE;
            gestureVolumeLayer.setVisibility(GONE);
            gestureSeekLayer.setVisibility(GONE);
            gestureLightLayer.setVisibility(GONE);
        }
        return gestureDetector.onTouchEvent(event);
    }

    // ------------------------------------------------------------------
    // 对外方法
    // ------------------------------------------------------------------

    /**
     * 设置视频信息（不立即播放）。
     */
    public void setDataSource(String url, String title) {
        videoPath = url;
        videoTitle = title;
        setVideoThumbnail();
    }

    public void playVideo(String url, String title) {
        playVideo(url, title, videoPosition);
    }

    /**
     * 播放视频。
     *
     * @param position 起始位置（毫秒）
     */
    public void playVideo(String url, String title, int position) {
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context, context.getString(R.string.lp_url_empty_hint), Toast.LENGTH_SHORT).show();
            return;
        }

        dismissControllerMenu();
        videoPath = url;
        videoTitle = title;
        videoPosition = position;
        isPrepare = false;
        isPlaying = true;
        initViews();

        boolean isNetwork = url.startsWith("http");
        if (!VideoHelper.isOnline(context) && isNetwork) {
            Toast.makeText(context, context.getString(R.string.lp_no_net_hint), Toast.LENGTH_SHORT).show();
            showNoNetView();
            return;
        }
        if (VideoHelper.isMobile(context) && isNetwork) {
            Toast.makeText(context, context.getString(R.string.lp_mobile_net_hint), Toast.LENGTH_SHORT).show();
        }
        if (!isNetwork && !new File(url).canRead()) {
            showErrorView(getContext().getString(R.string.lp_no_read_permission));
            return;
        }

        resetMediaPlayer();

        if (isNeedBatteryListen) {
            registerBatteryReceiver();
        } else {
            unRegisterBatteryReceiver();
            if (batteryView != null) {
                batteryView.setVisibility(GONE);
            }
        }
        if (isNeedNetChangeListen) {
            registerNetReceiver();
        } else {
            unRegisterNetReceiver();
        }
    }

    private void resetMediaPlayer() {
        try {
            if (mediaPlayer == null) {
                // Surface 尚未就绪，等待 surfaceCreated 后由 isFirstPlay 逻辑触发
                isFirstPlay = true;
                return;
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(videoPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorView(getContext().getString(R.string.lp_play_fail_hint));
        }
    }

    /**
     * 继续播放。
     */
    public void startVideo() {
        if (mediaPlayer != null) {
            try {
                if (isPrepare) {
                    mediaPlayer.start();
                } else {
                    playVideo(videoPath, videoTitle);
                }
                toggleView.setImageResource(R.drawable.lp_ic_pause);
                isPlaying = true;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else {
            playVideo(videoPath, videoTitle);
        }
    }

    /**
     * 暂停。
     */
    public void pauseVideo() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                videoPosition = mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            toggleView.setImageResource(R.drawable.lp_ic_play);
            isPlaying = false;
        }
    }

    public void setOrientationPortrait() {
        setPortrait();
    }

    public void setOrientationLandscape() {
        setLandscape();
    }

    public void setIsNeedBatteryListen(boolean need) {
        this.isNeedBatteryListen = need;
    }

    public void setIsNeedNetChangeListen(boolean need) {
        this.isNeedNetChangeListen = need;
    }

    public boolean isFullScreen() {
        return isFullscreen;
    }

    public int getVideoCurrentPosition() {
        return safeCurrentPosition();
    }

    public int getVideoTotalDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * 销毁资源，务必在 Activity onDestroy 中调用。
     */
    public void destroyVideo() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        surfaceHolder = null;
        videoPosition = 0;
        unRegisterBatteryReceiver();
        unRegisterNetReceiver();
        networkListener = null;
        completionListener = null;
        orientationListener = null;
        stopProgressTask();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private int safeCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // 视图状态
    // ------------------------------------------------------------------

    private void showErrorView(String msg) {
        centerPlay.setVisibility(GONE);
        noNetLayer.setVisibility(GONE);
        loading.setVisibility(GONE);
        thumbnail.setVisibility(GONE);
        statusLayer.setVisibility(VISIBLE);
        errorLayer.setVisibility(VISIBLE);
        errorText.setText(TextUtils.isEmpty(msg)
                ? getContext().getString(R.string.lp_loading_exception) : msg);
    }

    private void showNoNetView() {
        centerPlay.setVisibility(GONE);
        loading.setVisibility(GONE);
        errorLayer.setVisibility(GONE);
        thumbnail.setVisibility(GONE);
        statusLayer.setVisibility(VISIBLE);
        noNetLayer.setVisibility(VISIBLE);
    }

    // ------------------------------------------------------------------
    // 广播
    // ------------------------------------------------------------------

    private void registerBatteryReceiver() {
        if (batteryReceiver == null) {
            batteryReceiver = new BatteryReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            // 电量广播是 sticky 的，注册时会同步返回最近一次值，立即应用避免二次 playVideo 后电池被隐藏不再刷新
            Intent sticky = context.registerReceiver(batteryReceiver, filter);
            applyBattery(sticky);
        }
    }

    private void applyBattery(Intent intent) {
        if (intent == null || batteryView == null) {
            return;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return;
        }
        batteryView.setVisibility(VISIBLE);
        batteryView.setLevel((level * 100) / scale);
    }

    private void unRegisterBatteryReceiver() {
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            batteryReceiver = null;
        }
    }

    private void registerNetReceiver() {
        if (netChangeReceiver == null) {
            netChangeReceiver = new NetChangeReceiver();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(netChangeReceiver, filter);
        }
    }

    private void unRegisterNetReceiver() {
        if (netChangeReceiver != null) {
            try {
                context.unregisterReceiver(netChangeReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            netChangeReceiver = null;
        }
    }

    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            applyBattery(intent);
        }
    }

    class NetChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (networkListener == null || !isNeedNetChangeListen) {
                return;
            }
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    networkListener.onWifi(mediaPlayer);
                } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    networkListener.onMobile(mediaPlayer);
                }
            } else {
                networkListener.onUnavailable(mediaPlayer);
            }
        }
    }

    // ------------------------------------------------------------------
    // 回调设置
    // ------------------------------------------------------------------

    public void setOnNetChangeListener(NetworkListener listener) {
        this.networkListener = listener;
    }

    public void setOnCompletionListener(CompletionListener listener) {
        this.completionListener = listener;
    }

    public void setOnScreenOrientationListener(OrientationListener listener) {
        this.orientationListener = listener;
    }
}
