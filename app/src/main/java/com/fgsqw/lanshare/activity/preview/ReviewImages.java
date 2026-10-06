package com.fgsqw.lanshare.activity.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.preview.adapter.PreViewPagerAdapter;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.widget.PreViewViewPager;
import com.fgsqw.lanshare.utils.mUtil;

import java.util.List;


public class ReviewImages extends BaseActivity {
    public static final String MAX_SELECT_COUNT = "max_select_count";
    public static final String POSITION = "position";
    public static final int RESULT_CODE = 0x00000012;

    private PreViewViewPager vpImage;
    private TextView tvIndicator;
    private TextView tvLiveBadge;
    private RelativeLayout rlTopBar;

    //tempImages和tempSelectImages用于图片列表数据的页面传输。
    //之所以不要Intent传输这两个图片列表，因为要保证两位页面操作的是同一个列表数据，同时可以避免数据量大时，
    // 用Intent传输发生的错误问题。
    private static List<MessageMediaContent> tempFileUtils;
    private static List<MessageMediaContent> tempSelectFileUtils;

    private List<MessageMediaContent> mFileUtils;
    private List<MessageMediaContent> mSelectFileUtils;
    private boolean isShowBar = true;
//    private boolean isConfirm = false;
    // private boolean isSingle;
    private int mMaxCount;

    private BitmapDrawable mSelectDrawable;
    private BitmapDrawable mUnSelectDrawable;
    private PreViewPagerAdapter mPagerAdapter;

    public static void openActivity(Activity activity, List<MessageMediaContent> fileUtils,
                                    List<MessageMediaContent> selectFileUtils, boolean isSingle,
                                    int maxSelectCount, int position) {
        tempFileUtils = fileUtils;
        tempSelectFileUtils = selectFileUtils;
        Intent intent = new Intent(activity, ReviewImages.class);
        intent.putExtra(MAX_SELECT_COUNT, maxSelectCount);
        // intent.putExtra(ImageSelector.IS_SINGLE, isSingle);
        intent.putExtra(POSITION, position);
        activity.startActivityForResult(intent, RESULT_CODE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_photo);

        setStatusBarVisible(true);
        mFileUtils = tempFileUtils;
        tempFileUtils = null;
        mSelectFileUtils = tempSelectFileUtils;
        tempSelectFileUtils = null;

        Intent intent = getIntent();
        mMaxCount = intent.getIntExtra(MAX_SELECT_COUNT, 0);
        //  isSingle = intent.getBooleanExtra(ImageSelector.IS_SINGLE, false);

        Resources resources = getResources();
        mSelectDrawable = drawableToBitmapDrawable(resources, ContextCompat.getDrawable(this, R.drawable.ic_image_select));
        mUnSelectDrawable = drawableToBitmapDrawable(resources, ContextCompat.getDrawable(this, R.drawable.ic_image_un_select));

        setStatusBarColor();
        initView();
        initListener();
        initViewPager();

        tvIndicator.setText(mUtil.addString(1, "/", mFileUtils.size()));
        int initialPosition = intent.getIntExtra(POSITION, 0);
        vpImage.setCurrentItem(initialPosition);
        // 初始化实况图标识
        if (initialPosition < mFileUtils.size() && mFileUtils.get(initialPosition).isLivePhoto()) {
            tvLiveBadge.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 将 Drawable 转换为 BitmapDrawable（兼容矢量图）
     */
    private BitmapDrawable drawableToBitmapDrawable(Resources resources, Drawable drawable) {
        if (drawable == null) return null;
        int size = (int) getResources().getDimension(android.R.dimen.app_icon_size);
        if (size <= 0) size = 48;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
        bitmapDrawable.setBounds(0, 0, size, size);
        return bitmapDrawable;
    }

    private void initView() {
        vpImage = findViewById(R.id.preview_image_vp);
        tvIndicator = findViewById(R.id.preview_indicator_tv);
        tvLiveBadge = findViewById(R.id.preview_live_badge);
        rlTopBar = findViewById(R.id.preview_top_bar);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rlTopBar.getLayoutParams();
        lp.topMargin = getStatusBarHeight(this);
        rlTopBar.setLayoutParams(lp);
    }

    private void initListener() {
        findViewById(R.id.preview_btn_back).setOnClickListener(v -> finish());
    }

    /**
     * 初始化ViewPager
     */
    private void initViewPager() {
        mPagerAdapter = new PreViewPagerAdapter(this, mFileUtils);
        vpImage.setAdapter(mPagerAdapter);
        // 将 TextureView 叠加层添加到 ViewPager 上
        mPagerAdapter.attachToViewPager(vpImage);
        mPagerAdapter.setOnItemClickListener((position, photoInfo) -> {
            if (isShowBar) {
                hideBar();
            } else {
                showBar();
            }
        });
        vpImage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onPageSelected(int position) {
                tvIndicator.setText(position + 1 + "/" + mFileUtils.size());
                // 滑动时停止视频播放并更新实况图标识
                mPagerAdapter.stopVideoPlayback();
                if (position < mFileUtils.size() && mFileUtils.get(position).isLivePhoto()) {
                    tvLiveBadge.setVisibility(View.VISIBLE);
                } else {
                    tvLiveBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    /**
     * 修改状态栏颜色
     */
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#373c3d"));
        }
    }

    /**
     * 获取状态栏高度
     */
    @SuppressLint("InternalInsetResource")
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 显示和隐藏状态栏
     */
    private void setStatusBarVisible(boolean show) {
        if (show) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /**
     * 显示头部和尾部栏
     */
    private void showBar() {
        isShowBar = true;
        setStatusBarVisible(true);
        //添加延时，保证StatusBar完全显示后再进行动画。
        rlTopBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rlTopBar != null) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(rlTopBar, "translationY",
                            rlTopBar.getTranslationY(), 0).setDuration(300);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @SuppressLint("WrongConstant")
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            if (rlTopBar != null) {
                                rlTopBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    animator.start();


                }
            }
        }, 100);
    }

    /**
     * 隐藏头部和尾部栏
     */
    private void hideBar() {
        isShowBar = false;
        ObjectAnimator animator = ObjectAnimator.ofFloat(rlTopBar, "translationY",
                0, -rlTopBar.getHeight()).setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @SuppressLint("WrongConstant")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (rlTopBar != null) {
                    rlTopBar.setVisibility(View.GONE);
                    //添加延时，保证rlTopBar完全隐藏后再隐藏StatusBar。
                    rlTopBar.postDelayed(() -> setStatusBarVisible(false), 5);
                }
            }
        });
        animator.start();

    }

    @Override
    protected void onDestroy() {
        if (mPagerAdapter != null) {
            mPagerAdapter.detachFromViewPager();
        }
        super.onDestroy();
    }

}
