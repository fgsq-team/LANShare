package com.fgsqw.lanshare.fragment.child;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.preview.ReviewImages;
import com.fgsqw.lanshare.activity.video.VideoPlayer;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.fragment.adapter.PhotoAdapter;
import com.fgsqw.lanshare.fragment.adapter.SortPhotoAdapter;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.PhotoFolder;
import com.fgsqw.lanshare.pojo.PhotoInfo;
import com.fgsqw.lanshare.utils.DateUtils;
import com.fgsqw.lanshare.utils.FIleSerachUtils;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FragPhotoList extends BaseFragment implements View.OnClickListener {

    private ViewPager vp;
    private View view;
    private ImageView backImg;
    private TextView sizImgTv;
    private TextView timeTv;
    private CheckBox selectAll;
    private SwipeRefreshLayout swipe;
    private RecyclerView recyclerView;
    private RelativeLayout backLayout;
    private LinearLayout selectLayout;
    private boolean isOpenFolder;
    private boolean isShowTime;
    private boolean isViewImage = true;

    private int posiition;
    private PhotoAdapter mPhotoAdapter;
    private GridLayoutManager mLayoutManager;
    private List<PhotoFolder> mFolders;
    public List<PhotoInfo> mSelectFile = new ArrayList<>();

    private final Handler mHideHandler = new Handler();
    private final Runnable mHide = this::hideTime;

    public DataCenterActivity dataCenterActivity;


    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_photo, container, false);
            initView();
            loadImageForSDCard();
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void initView() {
        selectLayout = view.findViewById(R.id.photo_select_layout);
        backImg = view.findViewById(R.id.photo_back_img);
        sizImgTv = view.findViewById(R.id.photo_size_img_tv);
        recyclerView = view.findViewById(R.id.photo_recy);
        timeTv = view.findViewById(R.id.photo_time_tv);
        swipe = view.findViewById(R.id.photo_swipe);
        backLayout = view.findViewById(R.id.photo_back_layout);
        selectAll = view.findViewById(R.id.photo_check_select_all);
        backLayout.setOnClickListener(this);
        selectAll.setOnClickListener(this);
        swipe.setOnRefreshListener(this::loadImageForSDCard);
    }

    private void loadImageForSDCard() {
        ViewUpdate.runThread(() -> {
            ViewUpdate.threadUi(() -> {
                sizImgTv.setText("加载中");
                swipe.setRefreshing(true);
            });
            mFolders = FIleSerachUtils.loadImageForSDCard(getContext());
            ViewUpdate.threadUi(() -> {
                if (mFolders != null && !mFolders.isEmpty()) {
                    isOpenFolder = true;
                    posiition = 0;
                    folderview();//初始化文件列表
                    swipe.setRefreshing(false);//关闭加载进度条
                }
            });
        });

    }

    public void initPhotoList(PhotoFolder photoFolder) {
        if (mLayoutManager == null) {
            mLayoutManager = new GridLayoutManager(getActivity(), 4);
        }


        if (mPhotoAdapter == null) {
            mPhotoAdapter = new PhotoAdapter(this, isViewImage);

        }
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mPhotoAdapter);
        mPhotoAdapter.refresh(photoFolder.getImages());


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isOpenFolder) {
                    changeTime();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //if(isOpenFolder);
                //changeTime();
            }
        });

        mPhotoAdapter.setOnImageSelectListener((photoInfo, isSelect, view) -> {
            selectAll.setChecked(mPhotoAdapter.isSelectAll());
        });

        mPhotoAdapter.setOnItemClickListener(new PhotoAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(PhotoInfo photoInfo, int position) {
                if (photoInfo.isVideo()) {
                    toPreviewVideoActivity(photoInfo);
                } else {
                    toPreviewActivity(mPhotoAdapter.getData(), position);

                }
            }

            @Override
            public void OnLongItenClick(final PhotoInfo photoInfo, final int position) {
                final String path = photoInfo.getPath();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("请选择操作");
                String[] items;
                // 如果能写入(修改/删
                final File f = new File(path);
                if (f.canWrite()) {
                    items = new String[]{"发送", "信息", "打开", "取消",};
                } else {
                    items = new String[]{"发送"};
                }
                // 绑定选项和点击事件
                builder.setItems(items, (arg0, arg1) -> {
                    switch (arg1) {
                        case 0:
                            // 发送
                            dataCenterActivity.sendFiles(Arrays.asList(photoInfo));
                            break;
                        case 1:
                            Toast.makeText(getContext(), f.getPath(), Toast.LENGTH_LONG).show();
                            break;
                        case 2:
                            //打开
                            if (photoInfo.isVideo()) {
                                toPreviewVideoActivity(photoInfo);
                            } else {
                                toPreviewActivity(Arrays.asList(photoInfo), 1);
                            }
                            break;
                        case 4:
                            //取消
                            break;
                    }
                }).show();
            }
        });
    }

    private void initfolderlist() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SortPhotoAdapter adapter = new SortPhotoAdapter(getContext(), mFolders);
        adapter.setOnFolderSelectListener(new SortPhotoAdapter.OnFolderSelectListener() {
            @Override
            public void OnImageFoderSelect(PhotoFolder folder) {


            }

            @Override
            public void OnFolderSelect(PhotoFolder folder) {
                posiition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                //获取当前列表显示的第一个item
                imageview(folder);

            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(posiition);
    }


    /**
     * 文件列表视图
     */
    @SuppressLint("SetTextI18n")
    private void folderview() {
        if (isOpenFolder) {
            initfolderlist();
            sizImgTv.setText("文件" + "(" + mFolders.size() + ")");
            backImg.setVisibility(View.GONE);
            selectLayout.setVisibility(View.GONE);
            isOpenFolder = false;
        }
    }

    /**
     * 图片列表视图
     **/
    @SuppressLint("SetTextI18n")
    private void imageview(PhotoFolder folder) {
        if (!isOpenFolder) {
            initPhotoList(folder);
            selectAll.setChecked(mPhotoAdapter.isSelectAll());
            sizImgTv.setText(folder.getName() + "(" + folder.getImages().size() + ")");
            isOpenFolder = true;
            backImg.setVisibility(View.VISIBLE);
            selectLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 隐藏时间条
     */
    private void hideTime() {
        if (isShowTime) {
            ObjectAnimator.ofFloat(timeTv, "alpha", 1, 0).setDuration(300).start();
            isShowTime = false;
        }
    }

    /**
     * 显示时间条
     */
    private void showTime() {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(timeTv, "alpha", 0, 1).setDuration(300).start();
            isShowTime = true;
        }
    }

    /**
     * 改变时间条显示的时间（显示图片列表中的第一个可见图片的时间）
     */
    private void changeTime() {
        int firstVisibleItem = getFirstVisibleItem();    //获取屏幕第一个item 位置
        PhotoInfo photoInfo = mPhotoAdapter.getFirstVisibleImage(firstVisibleItem); //获取图片列表工具类
        if (photoInfo != null) {
            String time = DateUtils.getImageTime(photoInfo.getTime() * 1000);
            timeTv.setText(time);
            showTime();
            mHideHandler.removeCallbacks(mHide);
            mHideHandler.postDelayed(mHide, 500);
        }
    }

    private int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();//获取屏幕第一个item 位置
    }


    private void toPreviewActivity(List<PhotoInfo> photoInfos, int position) {
        if (photoInfos != null && !photoInfos.isEmpty()) {
            ReviewImages.openActivity(getActivity(), photoInfos,
                    mPhotoAdapter.getSelectImages(), false, 0, position);
        }
    }

    private void toPreviewVideoActivity(FileInfo images) {
        if (images != null) {
            File name = new File(images.getPath());
            Intent intent = new Intent(getContext(), VideoPlayer.class);
            intent.putExtra("path", name.getPath());
            dataCenterActivity.startActivity(intent);
        }
    }


    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (isOpenFolder) {
            folderview();
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.photo_back_layout: {
                folderview();
                break;
            }
            case R.id.photo_check_select_all: {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    mPhotoAdapter.setSelecteAll(mPhotoAdapter.getData());
                    mSelectFile.clear();
                    mSelectFile.addAll(mPhotoAdapter.getData());
                } else {
                    mPhotoAdapter.clearThisFolderAllSelect();
                }
                break;
            }

        }
    }

}



















