package com.fgsqw.lanshare.fragment.child;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.base.view.MLinearLayoutManager;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.dialog.InfoDialog;
import com.fgsqw.lanshare.fragment.adapter.SearchAdapter;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.pojo.message.MessageAudioContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageFolderContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.pojo.network.MediaResult;
import com.fgsqw.lanshare.service.MusicService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;

import java.io.File;
import java.util.*;

/**
 * @author fgsq
 * @comments 文件搜索界面
 * @date 2024/7/13 10:51
 */
public class FragmentSearch extends BaseFragment implements View.OnClickListener, View.OnLongClickListener {


    public static final String TAG = "FragSearch";
    private View view;
    private EditText searchEdit;
    private final List<MessageFileContent> searchResiltsList = new ArrayList<>();
    private final List<MessageFileContent> selectFileList = new LinkedList<>();   // 当前文件列表
    private SearchAdapter searchAdapter;

    public List<MessageFileContent> getSearchResiltsList() {
        return searchResiltsList;
    }

    public DataCenterActivity dataCenterActivity;
    public boolean extend = false;


    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate:" + this.hashCode());
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView:" + (view == null));
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_search, container, false);
            initView();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;
    }

    public void initView() {
        searchEdit = view.findViewById(R.id.search_edit);
        Button searchButton = view.findViewById(R.id.search_button);
        RecyclerView mRecyclerView = view.findViewById(R.id.search_recy);
        searchButton.setOnClickListener(this);
        searchButton.setOnLongClickListener(this);
        searchEdit.addTextChangedListener(editListener);
        mRecyclerView.setLayoutManager(new MLinearLayoutManager(getContext()));
        searchAdapter = new SearchAdapter(this);
        searchAdapter.setOnClickListener(new SearchAdapter.OnClickListener() {
            @Override
            public void onClick(int position) {
            }

            @Override
            public void onLongClick(int position) {
                dialog(position);
            }

            @Override
            public void onImageClick(int position) {

            }
        });
        mRecyclerView.setAdapter(searchAdapter);

    }

    private final TextWatcher editListener = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            Editable text = searchEdit.getText();
            if (text != null) {
                String str = text.toString();
                if (search != null) {
                    search.next = false;
                }
                search(str, true);
            }
        }
    };


    @Override
    public void clearSelect() {
        if (!selectFileList.isEmpty() && isVisible()) {
            selectFileList.clear();
            searchAdapter.refresh();
        }
    }

    private void dialog(final int position) {
        MessageFileContent fileInfo = searchResiltsList.get(position);
        File file = new File(fileInfo.getPath());
        if (fileInfo.getLength() == 0) {
            T.s((R.string.file_size_is_zero));
            return;
        }
        if (!file.canRead()) {
            T.s((R.string.file_cannot_be_read));
            return;
        }

        InfoDialog dialog = new InfoDialog(getContext(), R.style.AlertDialogTheme);
        dialog.setTitle(getString(R.string.please_select_operation));

        if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_APK) {
            String[] items = new String[]{
                    getString(R.string.send),
                    getString(R.string.backup),
                    getString(R.string.open),
                    getString(R.string.uninstall),
                    getString(R.string.generate_ipv6_sharing_link),
                    getString(R.string.generate_ipv4_sharing_link),
                    getString(R.string.cancel),
            };
            dialog.setItems(items);
            dialog.setOnItemClickListener(arg1 -> {
                switch (arg1) {
                    case 0:
                        dataCenterActivity.sendSingleFile(fileInfo);
                        break;
                    case 1:
                        new CopFileTask(getContext(), fileInfo.getPath(), Config.FILE_SAVE_PATH + "备份/" + fileInfo.getName()).execute(0);
                        break;
                    case 2:
                        FileUtil.startApp(getContext(), ((MessageApkContent) fileInfo).getPackageName());
                        break;
                    case 3:
                        FileUtil.uninstallApp(getContext(), ((MessageApkContent) fileInfo).getPackageName());
                        break;
                    case 4:
                        mUtil.shareFile(false, fileInfo, getContext());
                        break;
                    case 5:
                        mUtil.shareFile(true, fileInfo, getContext());
                        break;
                    default:
                        break;
                }
            });
        } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_VIDEO) {
            String[] items = new String[]{
                    getString(R.string.send),
                    getString(R.string.open),
                    getString(R.string.play),
                    getString(R.string.generate_ipv6_sharing_link),
                    getString(R.string.generate_ipv4_sharing_link),
                    getString(R.string.cancel),
            };
            dialog.setItems(items);
            dialog.setOnItemClickListener(arg1 -> {
                switch (arg1) {
                    case 0:
                        dataCenterActivity.sendSingleFile(fileInfo);
                        break;
                    case 1:
                        FileUtil.openFile((Activity) getContext(), file);
                        break;
                    case 2:
                        Intent intent = new Intent(dataCenterActivity, MusicService.class);
                        intent.putExtra("musicFilePath", fileInfo.getPath());
                        dataCenterActivity.startService(intent);
                        break;
                    case 3:
                        mUtil.shareFile(false, fileInfo, getContext());
                        break;
                    case 4:
                        mUtil.shareFile(true, fileInfo, getContext());
                        break;
                    default:
                        break;
                }
            });
        } else {
            String[] items = new String[]{
                    getString(R.string.send),
                    getString(R.string.open),
                    getString(R.string.generate_ipv6_sharing_link),
                    getString(R.string.generate_ipv4_sharing_link),
                    getString(R.string.cancel),
            };
            dialog.setItems(items);
            dialog.setOnItemClickListener(arg1 -> {
                switch (arg1) {
                    case 0:
                        dataCenterActivity.sendSingleFile(fileInfo);
                        break;
                    case 1:
                        FileUtil.openFile((Activity) getContext(), file);
                        break;
                    case 2:
                        mUtil.shareFile(false, fileInfo, getContext());
                        break;
                    case 3:
                        mUtil.shareFile(true, fileInfo, getContext());
                        break;
                    default:
                        break;
                }
            });
        }
        dialog.show();
    }

    FileUtil.Search search = null;

    public void searchFile(boolean extend, File folder, String str) {
        if (extend) {
            searchResiltsList.clear();
            selectFileList.clear();
            searchEdit.removeTextChangedListener(editListener);
            searchEdit.setText(str);
            searchEdit.setSelection(searchEdit.getText().length());
            searchEdit.addTextChangedListener(editListener);
            this.extend = true;
        }
        if (search != null) {
            search.next = false;
        }
        search = new FileUtil.Search(str) {
            @Override
            public void onSearchChange(File file) {
                MessageFileContent fileInfo;
                if (file.isFile()) {
                    fileInfo = new MessageFileContent();
                } else {
                    fileInfo = new MessageFolderContent();
                }
                fileInfo.setLength(file.length());
                fileInfo.setPath(file.getPath());
                fileInfo.setName(file.getName());
                Log.d(TAG, file.getAbsolutePath());
                if (searchResiltsList.size() < 100) {
                    searchResiltsList.add(fileInfo);
                    Collections.sort(searchResiltsList, (o1, o2) -> {
                        int diff1 = Math.abs(o1.getName().length() - str.length());
                        int diff2 = Math.abs(o2.getName().length() - str.length());
                        return Integer.compare(diff1, diff2);
                    });
                    ThreadUtils.threadUi(() -> searchAdapter.refresh());
                } else {
                    next = false;
                }
            }
        };
        ThreadUtils.runThread(() -> FileUtil.searchFiles(folder, search));
    }

    public void search(String str, boolean change) {
        searchResiltsList.clear();
        selectFileList.clear();
        if (!StringUtils.isEmpty(str)) {
            str = str.toLowerCase();
            String finalStr = str;
            if (extend || (Config.SEARCH_FLAG[3] && !change)) {
                searchFile(false, Environment.getExternalStorageDirectory(), str);
            }
            if (!extend && Config.SEARCH_FLAG[0]) {
                List<MessageApkContent> apkFileList = AnyData.apkFileList;
                if (apkFileList != null && !apkFileList.isEmpty()) {
                    for (MessageApkContent apkInfo : apkFileList) {
                        if (apkInfo.getName().toLowerCase().contains(str)) {
                            if (searchResiltsList.size() < 100) {
                                searchResiltsList.add(apkInfo);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            if (!extend && Config.SEARCH_FLAG[1]) {
                MediaResult mediaResult = AnyData.mediaResult;
                if (mediaResult != null) {
                    List<MessageMediaContent> mediaInfos = mediaResult.getAllMedia();
                    if (mediaInfos != null && !mediaInfos.isEmpty()) {
                        for (MessageMediaContent image : mediaInfos) {
                            if (image.getName().contains(str)) {
                                if (searchResiltsList.size() < 100) {
                                    searchResiltsList.add(image);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (!extend && Config.SEARCH_FLAG[2]) {
                List<MessageAudioContent> musicInfoList = AnyData.musicInfoList;
                if (musicInfoList != null && !musicInfoList.isEmpty()) {
                    for (MessageAudioContent musicInfo : musicInfoList) {
                        if (musicInfo.getName().toLowerCase().contains(str)) {
                            if (searchResiltsList.size() < 100) {
                                searchResiltsList.add(musicInfo);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            // 把结果长度与搜索值长度最相近的结果
            Collections.sort(searchResiltsList, (o1, o2) -> {
                int diff1 = Math.abs(o1.getName().length() - finalStr.length());
                int diff2 = Math.abs(o2.getName().length() - finalStr.length());
                return Integer.compare(diff1, diff2);
            });
        } else {
            extend = false;
        }
        searchAdapter.refresh();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search_button) {
            Editable text = searchEdit.getText();
            if (text != null) {
                String str = text.toString();
                search(str, false);
            }
        }
    }

    public List<MessageFileContent> getSelectFileList() {
        return selectFileList;
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.search_button) {
            searchEdit.setText("");
        }
        return true;
    }
}
