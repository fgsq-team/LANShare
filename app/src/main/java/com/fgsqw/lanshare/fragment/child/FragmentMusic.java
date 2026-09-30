package com.fgsqw.lanshare.fragment.child;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLinearLayoutManager;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.dialog.FileInfoDialog;
import com.fgsqw.lanshare.fragment.adapter.MusicAdapter;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.message.MessageAudioContent;
import com.fgsqw.lanshare.service.MusicService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.FileSearchUtils;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.SeekBar;
import android.widget.TextView;
import com.fgsqw.lanshare.utils.mUtil;

public class FragmentMusic extends BaseFragment implements View.OnClickListener {

    private Uri mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private View view;

    private ImageButton play;
    private ImageButton next;
    private ImageButton previous;
    private SwipeRefreshLayout mSwipe;
    private RecyclerView mRecyclerView;

    Context context;
    private TextView start, stop, nam;
    private SeekBar seekbar;
    private int listtab = 0;

    public DataCenterActivity dataCenterActivity;
    public MusicAdapter musicAdapter;
    private MLinearLayoutManager mLayoutManager;
    private List<MessageAudioContent> musicList = new ArrayList<>();  // 当前文件所有列表
    private final List<MessageAudioContent> selectMusicList = new LinkedList<>();   // 当前文件列表

    private MusicService musicService;
    private boolean isBound = false;
    private boolean isPlaying = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            if (musicService.isPlaying()) {
                nam.setText(musicService.getName());
                stop.setText(musicService.getTotalTime());
                seekbar.setMax(musicService.getDuration());
                play.setImageResource(R.drawable.pause);
                isPlaying = true;
            }
            // 注册播放进度监听器，用于更新播放进度
            musicService.setProgressListener(new MusicService.MusicProgressListener() {

                @Override
                public void onPlay(String name, String endTime, int duration) {
                    ThreadUtils.threadUi(() -> {
                        nam.setText(name);
                        stop.setText(endTime);
                        seekbar.setMax(duration);
                        play.setImageResource(R.drawable.pause);
                    });

                }

                @Override
                public void onPause() {
                    ThreadUtils.threadUi(() -> {
                        play.setImageResource(R.drawable.play);
                    });
                }

                @Override
                public void onCompletion() {
                    next();
                }

                @Override
                public void onProgressChanged(final int progress, String time) {
                    ThreadUtils.threadUi(() -> {
                        start.setText(time);
                        seekbar.setProgress(progress);
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };


    public List<MessageAudioContent> getMusicList() {
        return musicList;
    }

    public List<MessageAudioContent> getSelectMusicList() {
        return selectMusicList;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(dataCenterActivity, MusicService.class);
        dataCenterActivity.bindService(intent, connection, 0);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            dataCenterActivity.unbindService(connection);
            isBound = false;
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dataCenterActivity = (DataCenterActivity) activity;
    }

    @Override
    public void onAttach(Context context) {
        // TODO: Implement this method
        this.context = context;
        super.onAttach(context);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == LCmd.FRAGMENT_PLAY_MUSIC) {
            MessageAudioContent musicInfo = (MessageAudioContent) message.obj;
            playFromMusicInfo(musicInfo);
        }
    }

    public void initView() {
        start = view.findViewById(R.id.fsTextView2);
        stop = view.findViewById(R.id.fsTextView3);
        nam = view.findViewById(R.id.fsTextView1);
        seekbar = view.findViewById(R.id.fsSeekBar1);
        mRecyclerView = view.findViewById(R.id.music_view_recy);
        mSwipe = view.findViewById(R.id.music_view_swip);
        play = view.findViewById(R.id.music_play);
        next = view.findViewById(R.id.music_next);
        previous = view.findViewById(R.id.music_previous);

        mSwipe.setOnRefreshListener(() -> loadImageForSDCard(true));

        play.setOnClickListener(this);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);

        mLayoutManager = new MLinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        musicAdapter = new MusicAdapter(this);
        musicAdapter.setOnClickListener(new MusicAdapter.OnClickListener() {
            @Override
            public void OnClick(int position) {//列表点击事件
                MessageAudioContent fileSource = musicList.get(position);

            }

            @Override
            public void OnLongClick(int position) {//列表长按时间
                MessageAudioContent fileInfo = musicList.get(position);

                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
                builder.setTitle(getString(R.string.please_select_operation));
                String[] items;
                if (fileInfo.getLength() == 0) {
                    T.s((R.string.file_size_is_zero));
                    return;
                }
                items = new String[]{
                        getString(R.string.send),
                        getString(R.string.open),
                        getString(R.string.info),
                        getString(R.string.play),
                        getString(R.string.generate_ipv6_sharing_link),
                        getString(R.string.generate_ipv4_sharing_link),
                        getString(R.string.cancel),
                };
                builder.setItems(items, (arg0, arg1) -> {
                    switch (arg1) {
                        case 0: {
                            dataCenterActivity.sendSingleFile(fileInfo);
                            break;
                        }
                        case 1: {
                            FileUtil.openFile((Activity) getContext(), new File(fileInfo.getPath()));
                            break;
                        }
                        case 2: {
                            FileInfoDialog fileInfoDialog = new FileInfoDialog(getContext(), fileInfo.getPath());
                            fileInfoDialog.show();
                            break;
                        }
                        case 3: {
                            Intent intent = new Intent(dataCenterActivity, MusicService.class);
                            intent.putExtra("musicFilePath", fileInfo.getPath());
                            dataCenterActivity.startService(intent);
                            break;
                        }
                        case 4:
                            mUtil.shareFile(false, fileInfo, getContext());
                            break;
                        case 5:
                            mUtil.shareFile(true, fileInfo, getContext());
                            break;
                        case 6: {
                            break;
                        }
                        default:
                            break;
                    }
                    arg0.dismiss();
                });
                builder.show();
            }

            @Override
            public void OnImageClick(int position) {
                MessageAudioContent fileSource = musicList.get(position);
                listtab = position;
                playFromMusicInfo(fileSource);
            }
        });
        mRecyclerView.setAdapter(musicAdapter);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.seek(seekBar.getProgress());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_music, container, false);
            Intent intent = new Intent(dataCenterActivity, MusicService.class);
            dataCenterActivity.startService(intent);
            initView();
            loadImageForSDCard(false);
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void next() {
        if (musicList.isEmpty()) {
            return;
        }
        listtab++;
        if (listtab > musicList.size() - 1) {
            listtab = 0;
        }
        playFromMusicInfo(musicList.get(listtab));
    }

    public void previous() {
        if (musicList.isEmpty()) {
            return;
        }
        listtab--;
        if (listtab < 0) {
            listtab = musicList.size() - 1;
        }
        playFromMusicInfo(musicList.get(listtab));
    }

    @Override
    public void clearSelect() {
        if (!selectMusicList.isEmpty() && isVisible()) {
            selectMusicList.clear();
            musicAdapter.refresh();
        }
    }

    private void loadImageForSDCard(boolean refresh) {
        mSwipe.setRefreshing(true);
        ThreadUtils.runThread(() -> {
            FileSearchUtils.loadMusicForSDCard(Objects.requireNonNull(getContext()), refresh);
            if (AnyData.musicInfoList != null) {
                musicList = AnyData.musicInfoList;
            }
            ThreadUtils.threadUi(() -> {
                musicAdapter.refresh();
                mSwipe.setRefreshing(false); // 关闭加载进度条
            });
        });
    }

    public void playFromMusicInfo(MessageAudioContent musicInfo) {
        if (musicInfo == null) {
            if (musicService.isPlaying()) {
                musicService.pause();
            } else {
                musicService.play();
            }
        } else {
            isPlaying = true;
            musicService.play(musicInfo.getPath());
        }
    }

    public void play() {
        if (!isPlaying) {
            if (!musicList.isEmpty()) {
                MessageAudioContent musicInfo = musicList.get(0);
                playFromMusicInfo(musicInfo);
            }
        } else {
            playFromMusicInfo(null);
        }
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (n == KeyEvent.KEYCODE_CHANNEL_UP) {
            next();
            return true;
        } else if (n == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            previous();
            return true;
        } else if (n == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            play();
            return true;
        } else if (n == KeyEvent.KEYCODE_REFRESH || n == KeyEvent.KEYCODE_AVR_INPUT) {
            loadImageForSDCard(true);
            return true;
        }
        return super.onKeyDown(n, keyEvent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.music_play: {
                play();
                break;
            }
            case R.id.music_previous: {
                previous();
                break;
            }
            case R.id.music_next: {
                next();
                break;
            }
        }
    }
}
