package com.fgsqw.lanshare.activity.preview.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.widget.PreViewViewPager;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * 图片预览适配器
 * <p>支持普通图片浏览和实况图长按播放功能</p>
 * <p>PhotoView 直接作为 ViewPager 子视图（保持原有测量行为），
 * TextureView 叠加在 ViewPager 上用于实况图视频播放</p>
 */
public class PreViewPagerAdapter extends PagerAdapter {

    private final Context mContext;
    private final List<PhotoView> viewList = new ArrayList<>(4);
    List<MessageMediaContent> mImgList;
    private OnItemClickListener mListener;

    // 实况图视频叠加层（由 ReviewImages 添加到 ViewPager 上）
    private TextureView mOverlayTextureView;
    private android.media.MediaPlayer mMediaPlayer;
    private SurfaceTexture mSurfaceTexture;
    private PreViewViewPager mHostViewPager;

    public PreViewPagerAdapter(Context context, List<MessageMediaContent> imgList) {
        this.mContext = context;
        createImageViews();
        mImgList = imgList;
    }

    private void createImageViews() {
        for (int i = 0; i < 4; i++) {
            PhotoView imageView = new PhotoView(mContext);
            imageView.setAdjustViewBounds(true);
            viewList.add(imageView);
        }
    }

    /**
     * 绑定宿主 ViewPager，并将 TextureView 叠加层添加到其上
     */
    public void attachToViewPager(PreViewViewPager viewPager) {
        mHostViewPager = viewPager;
        if (viewPager != null) {
            mOverlayTextureView = new TextureView(mContext);
            mOverlayTextureView.setVisibility(View.GONE);
            mOverlayTextureView.setOpaque(false);
            mOverlayTextureView.setAlpha(0f);
            // 添加到 ViewPager 顶层作为叠加层
            viewPager.addView(mOverlayTextureView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    /**
     * 从宿主 ViewPager 移除叠加层并释放资源
     */
    public void detachFromViewPager() {
        stopVideoPlayback();
        if (mOverlayTextureView != null && mHostViewPager != null) {
            mHostViewPager.removeView(mOverlayTextureView);
            mOverlayTextureView = null;
        }
        mHostViewPager = null;
    }

    @Override
    public int getCount() {
        return mImgList == null ? 0 : mImgList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final PhotoView currentView = viewList.remove(0);
        final MessageMediaContent mediaContent = mImgList.get(position);
        container.addView(currentView);
        currentView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(mContext).load(mediaContent.getPath())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(currentView);

        // 单击切换顶栏
        currentView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(position, mediaContent);
            }
        });

        // 长按播放实况图视频
        currentView.setOnLongClickListener(v -> {
            if (mediaContent.isLivePhoto()) {
                stopVideoPlayback();
                playLivePhotoVideo(mediaContent.getLiveVideoPath());
                return true;
            }
            return false;
        });

        return currentView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof PhotoView) {
            PhotoView view = (PhotoView) object;
            view.setImageDrawable(null);
            view.setOnLongClickListener(null);
            viewList.add(view);
            container.removeView(view);
        }
    }

    // ==================== 实况图视频播放 ====================

    private void playLivePhotoVideo(String videoPath) {
        if (videoPath == null || !new File(videoPath).exists()) {
            LLog.debug("Live photo video not found: " + videoPath);
            return;
        }
        if (mOverlayTextureView == null) {
            LLog.debug("Overlay TextureView not initialized");
            return;
        }

        try {
            android.media.MediaPlayer mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                LLog.error("MediaPlayer error: " + what + ", " + extra,
                        new RuntimeException("MediaPlayer error " + what));
                stopVideoPlayback();
                return true;
            });
            mediaPlayer.prepare();
            mMediaPlayer = mediaPlayer;

            SurfaceTexture surfaceTexture = mOverlayTextureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                Surface surface = new Surface(surfaceTexture);
                mediaPlayer.setSurface(surface);
                mSurfaceTexture = surfaceTexture;
                mediaPlayer.start();
                showVideoOverlay();
            } else {
                mOverlayTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                        try {
                            if (mMediaPlayer != null) {
                                mMediaPlayer.setSurface(new Surface(surface));
                                mSurfaceTexture = surface;
                                mMediaPlayer.start();
                                showVideoOverlay();
                            }
                        } catch (Exception e) {
                            LLog.error("Error starting video on surface ready", e);
                        }
                        if (mOverlayTextureView != null) {
                            mOverlayTextureView.setSurfaceTextureListener(null);
                        }
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    }
                });
            }
        } catch (Exception e) {
            LLog.error("Error playing live photo video", e);
            stopVideoPlayback();
        }
    }

    private void showVideoOverlay() {
        if (mOverlayTextureView != null) {
            mOverlayTextureView.setVisibility(View.VISIBLE);
            mOverlayTextureView.setAlpha(1f);
        }
    }

    /**
     * 停止视频播放并释放资源
     */
    public void stopVideoPlayback() {
        try {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            if (mOverlayTextureView != null) {
                mOverlayTextureView.setSurfaceTextureListener(null);
                mOverlayTextureView.setVisibility(View.GONE);
                mOverlayTextureView.setAlpha(0f);
            }
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }
        } catch (Exception e) {
            LLog.error("Error stopping video playback", e);
        }
    }

    // ==================== 回调与工具方法 ====================

    public void setOnItemClickListener(OnItemClickListener l) {
        mListener = l;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, MessageMediaContent mediaInfo);
    }

    private void setBitmap(PhotoView imageView, Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        if (bitmap != null) {
            int bw = bitmap.getWidth();
            int bh = bitmap.getHeight();
            int vw = imageView.getWidth();
            int vh = imageView.getHeight();
            if (bw != 0 && bh != 0 && vw != 0 && vh != 0) {
                if (1.0f * bh / bw > 1.0f * vh / vw) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    float offset = (1.0f * bh * vw / bw - vh) / 2;
                    adjustOffset(imageView, offset);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            }
        }
    }

    private void adjustOffset(PhotoView view, float offset) {
        PhotoViewAttacher attacher = new PhotoViewAttacher(view);
        try {
            Field field = PhotoViewAttacher.class.getDeclaredField("mBaseMatrix");
            field.setAccessible(true);
            Matrix matrix = (Matrix) field.get(attacher);
            matrix.postTranslate(0, offset);
            Method method = PhotoViewAttacher.class.getDeclaredMethod("resetMatrix");
            method.setAccessible(true);
            method.invoke(attacher);
        } catch (Exception e) {
            e.printStackTrace();
            LLog.error(e);
        }
    }

    public static Bitmap narrrowBitmap(Bitmap bm, int reqWidth, int reqHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) reqWidth) / width;
        float scaleHeight = ((float) reqHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }
}
