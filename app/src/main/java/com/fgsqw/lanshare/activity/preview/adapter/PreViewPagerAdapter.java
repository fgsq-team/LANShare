package com.fgsqw.lanshare.activity.preview.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.pojo.MediaInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PreViewPagerAdapter extends PagerAdapter {

    private final Context mContext;
    private final List<PhotoView> viewList = new ArrayList<>(4);
    List<MediaInfo> mImgList;
    private OnItemClickListener mListener;

    public PreViewPagerAdapter(Context context, List<MediaInfo> imgList) {
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

    @Override
    public int getCount() {
        return mImgList == null ? 0 : mImgList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof PhotoView) {
            PhotoView view = (PhotoView) object;
            view.setImageDrawable(null);
            viewList.add(view);
            container.removeView(view);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final PhotoView currentView = viewList.remove(0);
        final MediaInfo fileUtils = mImgList.get(position);
        container.addView(currentView);
        currentView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(mContext).load(fileUtils.getPath())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(currentView);



           /* Glide.with(mContext).asBitmap()
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .load(new File(fileUtils.getPath())).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    int bw = resource.getWidth();
                    int bh = resource.getHeight();
                    if (bw > 8192 || bh > 8192) {
                        Bitmap bitmap = narrrowBitmap(resource, 8192, 8192);
                        setBitmap(currentView, bitmap);
                    } else {
                        setBitmap(currentView, resource);
                    }
                }
            });*/

        currentView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(position, fileUtils);
            }
        });
        return currentView;
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

    public void setOnItemClickListener(OnItemClickListener l) {
        mListener = l;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, MediaInfo mediaInfo);
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
        }
    }

    public static Bitmap narrrowBitmap(Bitmap bm, int reqWidth, int reqHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) reqWidth) / width;
        float scaleHeight = ((float) reqHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        return newbm;
    }

}
