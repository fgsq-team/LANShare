package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.child.FragmentSearch;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.util.List;

/**
 * @author fgsq
 * @comments 文件搜索列表适配器
 * @date 2024/7/13 10:50
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    private final FragmentSearch fragmentSearch;
    private final Context context;
    private OnClickListener mListener;
    private OnImageSelectListener mSelectListener;

    public SearchAdapter(FragmentSearch fragmentSearch) {
        this.context = fragmentSearch.getContext();
        this.fragmentSearch = fragmentSearch;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final SearchAdapter.ViewHolder holder, int position) {
        final MessageFileContent fileInfo = fragmentSearch.getSearchResiltsList().get(position);
        holder.mName.setText(mUtil.stringSize(fileInfo.getName(), 30));
//        holder.selectLayout.setVisibility(View.GONE);
        int fileType = fileInfo.getFileType();
        if (fileType == MessageFileContent.FILE_TYPE_APK) {
            Glide.with(context).load(((MessageApkContent)fileInfo).getIcon())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .into(holder.mImg);
            holder.mInfo.setText(FileUtil.computeSize(fileInfo.getLength()));
        } else if (fileType == MessageFileContent.FILE_TYPE_AUDIO) {
            Glide.with(context)
                    .load(R.drawable.ic_music)
                    .centerCrop()
                    .placeholder(R.drawable.ic_null)
                    .into(holder.mImg);
            holder.mInfo.setText(FileUtil.computeSize(fileInfo.getLength()));
        } else if (fileType == MessageFileContent.FILE_TYPE_IMAGE || fileType == MessageFileContent.FILE_TYPE_VIDEO) {
            MessageMediaContent mediaInfo = (MessageMediaContent) fileInfo;
            Glide.with(context)
                    .load(mediaInfo.getPath())
                    .centerCrop()
                    .placeholder(R.drawable.ic_null)
                    .into(holder.mImg);
            holder.mInfo.setText(FileUtil.computeSize(fileInfo.getLength()));
        } else {
            // 文件
            Glide.with(context)
                    .load(fileType == MessageFileContent.FILE_TYPE_FOLDER ? R.drawable.ic_folder : R.drawable.ic_file_file)
                    .centerCrop()
                    .placeholder(R.drawable.ic_null)
                    .into(holder.mImg);
            if (fileType == MessageFileContent.FILE_TYPE_FOLDER) {
                holder.mInfo.setText(fileInfo.getPath());
            } else {
                holder.mInfo.setText(FileUtil.computeSize(fileInfo.getLength()) + " " + fileInfo.getPath());
            }
        }

        setItemSelect(holder, isSelect(fileInfo));

        holder.itemView.setOnClickListener(v -> {
//            if (mListener != null) {
//                mListener.onClick(position);
//            }
            checkedImage(holder, fileInfo, position);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onLongClick(position);
            }
            return true;
        });

        //点击选中/取消选中图片
        holder.selectLayout.setOnClickListener(v -> {
            checkedImage(holder, fileInfo, position);
        });
    }


    /**
     * 设置软件选中和未选中的效果
     */
    private void setItemSelect(SearchAdapter.ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.mSelect.setImageResource(R.drawable.ic_select);
            holder.layout.setAlpha(0.3f);//设置imageview透明度
        } else {
            holder.mSelect.setImageResource(R.drawable.ic_image_un_select);
            holder.layout.setAlpha(1f);//设置imageview透明度
        }
    }


    /*选中图片效果*/
    private void checkedImage(SearchAdapter.ViewHolder holder, MessageFileContent fileInfo, int position) {
        if (isSelect(fileInfo)) {//如果图片已经选中，就取消选中
            fragmentSearch.dataCenterActivity.removeSendFile(fileInfo);
            unSelectImage(fileInfo, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果
        } else {//如果未选中就选中
            if (fragmentSearch.dataCenterActivity.addASendFile(fileInfo)) {
                selectImage(fileInfo, position);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }
    }

    /**
     * 选中
     *
     * @param fileSource
     */
    private void selectImage(MessageFileContent fileSource, int position) {
        fragmentSearch.getSelectFileList().add(fileSource);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileSource, true, position);
        }
    }

    /**
     * 取消选中
     *
     * @param fileSource
     */
    private void unSelectImage(MessageFileContent fileSource, int position) {
        List<MessageFileContent> selectList = fragmentSearch.getSelectFileList();
        if (!selectList.isEmpty()) {
            for (int i = 0; i < selectList.size(); i++) {
                if (fileSource.getPath().equals(selectList.get(i).getPath())) {
                    selectList.remove(i);
                    break;
                }
            }
        }
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileSource, false, position);
        }
    }

    private boolean isSelect(MessageFileContent fileSource) {
        List<MessageFileContent> selectlist = fragmentSearch.getSelectFileList();
        if (selectlist != null && !selectlist.isEmpty()) {
            for (int i = 0; i < selectlist.size(); i++) {
                if (fileSource.equals(selectlist.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        List<MessageFileContent> pathlist = fragmentSearch.getSearchResiltsList();
        return pathlist == null ? 0 : pathlist.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {      //更换列表数据
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mInfo;
        ImageView mImg;
        LinearLayout layout;
        LinearLayout selectLayout;
        ImageView mSelect;

        public ViewHolder(View v) {
            super(v);
            mName = v.findViewById(R.id.file_item_name);
            mImg = v.findViewById(R.id.file_item_img);
            layout = v.findViewById(R.id.file_item_layout);
            mInfo = v.findViewById(R.id.file_item_info);
            selectLayout = v.findViewById(R.id.file_item_select_layout);
            mSelect = v.findViewById(R.id.file_item_select);
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }


    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.mSelectListener = listener;
    }


    public interface OnImageSelectListener {
        void OnImageSelect(MessageFileContent fileSource, boolean isSelect, int position);
    }

    public interface OnClickListener {
        void onClick(int position);

        void onLongClick(int position);

        void onImageClick(int position);
    }
}
