package com.fgsqw.lanshare.fragment.child;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.activity.preview.ReviewImages;
import com.fgsqw.lanshare.activity.video.VideoPlayer;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.db.FileSyncDBUtil;
import com.fgsqw.lanshare.dialog.DeviceSelectDialog;
import com.fgsqw.lanshare.dialog.FileInfoDialog;
import com.fgsqw.lanshare.dialog.InfoDialog;
import com.fgsqw.lanshare.fragment.adapter.MediaAdapter;
import com.fgsqw.lanshare.fragment.adapter.SortPhotoAdapter;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileSyncData;
import com.fgsqw.lanshare.pojo.file.PhotoFolder;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;

import java.io.File;
import java.util.*;

public class FragmentMediaList extends BaseFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private final String TAG = "FragMediaList";
    private View view;
    private ImageView backImg;
    private TextView sizImgTv;
    private TextView timeTv;
    private CheckBox selectAll;
    private CheckBox selectMode;
    private SwipeRefreshLayout swipe;
    private RecyclerView recyclerView;
    private RelativeLayout backLayout;
    private LinearLayout selectLayout;
    private boolean isOpenFolder;
    private boolean isShowTime;

    private int posiition;
    private MediaAdapter mMediaAdapter;
    private GridLayoutManager mLayoutManager;
    public List<PhotoFolder> mFolders;

    public final List<MessageMediaContent> mSelectList = new LinkedList<>();


    private final Handler mHideHandler = new Handler();
    private final Runnable mHide = this::hideTime;

    public DataCenterActivity dataCenterActivity;

    private List<MessageMediaContent> currentPhotoList;
    private PhotoFolder currentPhotoFolder;

    private PrefUtil prefUtil;

    private FileSyncDBUtil fileSyncDBUtil;


    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == LCmd.SERVICE_MEDIA_CHANGES) {          // 媒体变动
            loadImageForSDCard(false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_photo, container, false);
            prefUtil = App.getPrefUtil();
            fileSyncDBUtil = new FileSyncDBUtil(getContext());
            initView();
            loadImageForSDCard(false);
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        selectMode = view.findViewById(R.id.photo_check_select_mode);
        backLayout.setOnClickListener(this);
        selectAll.setOnClickListener(this);
        selectMode.setOnCheckedChangeListener(this);
        swipe.setOnRefreshListener(() -> loadImageForSDCard(true));
        selectMode.setChecked(prefUtil.getBoolean(PreConfig.MEDIA_SELECT_MODEL));
    }

    private List<Device> getDeviceList() {
        LANService instance = LANService.getInstance();
        Map<String, Device> deviceMap = null;
        if (instance != null) {
            deviceMap = LANService.getInstance().getOnLineDevices();
        }
        List<Device> deviceList;
        if (deviceMap != null && deviceMap.size() > 0) {
            deviceList = new ArrayList<>(deviceMap.values());
        } else {
            deviceList = new ArrayList<>();
        }
        return deviceList;
    }


    private void loadImageForSDCard(boolean refresh) {
        if (sizImgTv == null) {
            return;
        }
        sizImgTv.setText(R.string.loading);
        swipe.setRefreshing(true);
        ThreadUtils.runThread(() -> {
            DeviceDataScanner.scanImages(Objects.requireNonNull(getContext()), refresh);
            if (AnyData.mediaResult != null) {
                mFolders = AnyData.mediaResult.getmFolders();
            }
            ThreadUtils.threadUi(() -> {
                if (isOpenFolder) {
                    for (PhotoFolder mFolder : mFolders) {
                        if (mFolder.getFolderPath().equals(currentPhotoFolder.getFolderPath())) {
                            initPhotoList(mFolder);
                            break;
                        }
                    }
                } else {
                    if (mFolders != null && !mFolders.isEmpty()) {
                        isOpenFolder = true;
                        posiition = 0;
                        folderview(); // 初始化文件列表
                    }
                }
                swipe.setRefreshing(false); // 关闭加载进度条
            });
        });

    }

    @SuppressLint("SetTextI18n")
    public void initPhotoList(PhotoFolder photoFolder) {
        currentPhotoList = photoFolder.getImages();
        currentPhotoFolder = photoFolder;
        if (mLayoutManager == null) {
            mLayoutManager = new GridLayoutManager(getActivity(), 4);
        }
        recyclerView.setLayoutManager(mLayoutManager);
        if (mMediaAdapter == null) {
            mMediaAdapter = new MediaAdapter(this, !selectMode.isChecked());
        }
        recyclerView.setAdapter(mMediaAdapter);
        mMediaAdapter.refresh();
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

        mMediaAdapter.setOnImageSelectListener((photoInfo, isSelect, view) -> {
            selectAll.setChecked(mMediaAdapter.isSelectAll());
        });

        mMediaAdapter.setOnItemClickListener(new MediaAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(MessageMediaContent mediaInfo, int position) {
                if (mediaInfo.isVideo()) {
                    VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
                } else {
                    toPreviewActivity(currentPhotoList, position);
                }
            }

            @Override
            public void OnLongItenClick(final MessageMediaContent mediaInfo, final int position) {
                final String path = mediaInfo.getPath();
                InfoDialog dialog = new InfoDialog(getContext(), R.style.AlertDialogTheme);
                dialog.setTitle(getString(R.string.please_select_operation));
                final File f = new File(path);
                String[] items;
                if (f.canWrite()) {
                    items = new String[]{
                            getString(R.string.send),
                            getString(R.string.info),
                            getString(R.string.open),
                            getString(R.string.generate_ipv6_sharing_link),
                            getString(R.string.generate_ipv4_sharing_link),
                            getString(R.string.cancel),
                    };
                } else {
                    items = new String[]{getString(R.string.send)};
                }
                dialog.setItems(items);
                dialog.setOnItemClickListener(arg1 -> {
                    switch (arg1) {
                        case 0:
                            dataCenterActivity.sendSingleFile(mediaInfo);
                            break;
                        case 1:
                            FileInfoDialog fileInfoDialog = new FileInfoDialog(getContext(), f.getPath());
                            fileInfoDialog.show();
                            break;
                        case 2:
                            if (mediaInfo.isVideo()) {
                                VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
                            } else {
                                toPreviewActivity(Collections.singletonList(mediaInfo), 1);
                            }
                            break;
                        case 3:
                            mUtil.shareFile(false, mediaInfo, getContext());
                            break;
                        case 4:
                            mUtil.shareFile(true, mediaInfo, getContext());
                            break;
                        case 5:
                            break;
                        default:
                            break;
                    }
                });
                dialog.show();
            }
        });
        selectAll.setChecked(mMediaAdapter.isSelectAll());
        sizImgTv.setText(photoFolder.getName() + "(" + photoFolder.getImages().size() + ")");
        isOpenFolder = true;
        backImg.setVisibility(View.VISIBLE);
        selectLayout.setVisibility(View.VISIBLE);
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

            @Override
            public boolean OnLongClickListener(PhotoFolder folder) {
                InfoDialog dialog = new InfoDialog(getContext(), R.style.AlertDialogTheme);
                dialog.setTitle(getString(R.string.please_select_operation));
                String[] items = new String[]{
                        getString(R.string.media_sync),
                        getString(R.string.cancel),
                };
                dialog.setItems(items);
                dialog.setOnItemClickListener(arg1 -> {
                    if (arg1 == 0) {
                        mediaSync(folder);
                    }
                });
                dialog.show();
                return true;
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(posiition);
        sizImgTv.setText(getString(R.string.files) + "(" + mFolders.size() + ")");
        backImg.setVisibility(View.GONE);
        selectLayout.setVisibility(View.GONE);
        isOpenFolder = false;
    }


    public void mediaSync(PhotoFolder folder) {
        DeviceSelectDialog dialog = new DeviceSelectDialog(getContext());
        dialog.setTitle(getString(R.string.select_device_to_receive));
        dialog.setShowAllDevices(false);
        dialog.setOnDeviceSelect(device -> {
            String folderPath = folder.getFolderPath();
            FileSyncData fileSyncData = fileSyncDBUtil.queryFileSyncData(device.getUniqueUUid());
            if (fileSyncData == null) {
                fileSyncData = new FileSyncData();
                fileSyncData.setCreateTime(new Date().getTime());
                fileSyncData.setDeviceId(device.getUniqueUUid());
            } else {
                fileSyncDBUtil.deleteFileSyncData(device.getUniqueUUid());
                T.s(getString(R.string.media_sync_task_already_exists));
            }
            if (!Config.MEDIA_SYNC) {
                boolean mediaSync = prefUtil.getBoolean(PreConfig.MEDIA_SYNC, false);
                if (!mediaSync) {
                    LANService.getInstance().getImageObserver().registerObserver();
                    prefUtil.saveBoolean(PreConfig.MEDIA_SYNC, true);
                    Config.MEDIA_SYNC = true;
                }
            }
            fileSyncData.setDeviceName(device.getDevName());
            fileSyncData.setFolderPath(folderPath);
            fileSyncData.setName(folder.getName());
            fileSyncDBUtil.addFileSyncData(fileSyncData);
            List<MessageMediaContent> media = mUtil.deepCopyList(folder.getImages());
            LANService.getInstance().startSyncingMedias(device, media);
            T.s(getString(R.string.sync_function_set_up_successfully));
        });
        dialog.show();
    }

    /**
     * 文件列表视图
     */
    @SuppressLint("SetTextI18n")
    private void folderview() {
        if (isOpenFolder) {
            initfolderlist();
        }
    }

    /**
     * 图片列表视图
     **/
    @SuppressLint("SetTextI18n")
    private void imageview(PhotoFolder folder) {
        if (!isOpenFolder) {
            initPhotoList(folder);
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
        MessageMediaContent mediaInfo = mMediaAdapter.getFirstVisibleImage(firstVisibleItem); //获取图片列表工具类
        if (mediaInfo != null) {
            String time = DateUtils.getImageTime(mediaInfo.getTime() * 1000);
            timeTv.setText(time);
            showTime();
            mHideHandler.removeCallbacks(mHide);
            mHideHandler.postDelayed(mHide, 500);
        }
    }

    private int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();//获取屏幕第一个item 位置
    }


    private void toPreviewActivity(List<MessageMediaContent> mediaInfos, int position) {
        if (mediaInfos != null && !mediaInfos.isEmpty()) {
            ReviewImages.openActivity(getActivity(), mediaInfos,
                    mMediaAdapter.getSelectImages(), false, 0, position);
        }
    }


    @Override
    public boolean onBack() {
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
                    mMediaAdapter.setSelecteAll(currentPhotoList);
                    mSelectList.clear();
                    mSelectList.addAll(currentPhotoList);
                } else {
                    mMediaAdapter.clearThisFolderAllSelect();
                }
                break;
            }

        }
    }

    public List<MessageMediaContent> getSelectList() {
        return mSelectList;
    }

    public List<MessageMediaContent> getcurrentPhotoList() {
        return currentPhotoList;
    }

    @Override
    public void clearSelect() {
        if (!mSelectList.isEmpty() && isVisible()) {
            mSelectList.clear();
            mMediaAdapter.refresh();
            selectAll.setChecked(mMediaAdapter.isSelectAll());
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.photo_check_select_mode: {
                if (isOpenFolder) {
                    mMediaAdapter.setViewImage(!isChecked);
                }
                break;
            }

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (n == KeyEvent.KEYCODE_REFRESH || n == KeyEvent.KEYCODE_AVR_INPUT) {
            loadImageForSDCard(true);
        }
        return super.onKeyDown(n, keyEvent);
    }
}



















