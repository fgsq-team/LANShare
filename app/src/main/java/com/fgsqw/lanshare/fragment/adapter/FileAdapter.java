package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
import com.fgsqw.lanshare.fragment.child.FragFileList;
import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.pojo.FileSource;
import com.fgsqw.lanshare.pojo.MediaInfo;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.text.SimpleDateFormat;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    private final LayoutInflater mInflater;
    private final FragFileList fragFileList;
    private final Context context;
    private OnClickListener mListener;
    private OnImageSelectListener mSelectListener;

    public FileAdapter(FragFileList fragFileList) {
        this.context = fragFileList.getContext();
        this.fragFileList = fragFileList;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final FileAdapter.ViewHolder holder, int position) {
        final FileSource fileSource = fragFileList.getFileList().get(position);
        holder.mName.setText(mUtil.StringSize(fileSource.getName(), 30));
        if (position != 0) {
            holder.mInfo.setText(format.format(fileSource.getTime()) + " "
                    + FileUtil.computeSize(fileSource.getLength()));
            holder.selectLayout.setVisibility(View.VISIBLE);
        } else {
            holder.mInfo.setText("");
            holder.selectLayout.setVisibility(View.GONE);
        }

        if (fileSource.isPreView()) {
            Glide.with(context).load(fileSource.getPath())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.mImg);
        } else {
            Glide.with(context).load(fileSource.getPreView())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.mImg);
        }

        setItemSelect(holder, isSelect(fileSource));

        holder.layout.setOnClickListener(v -> mListener.OnClick(position));
        holder.layout.setOnLongClickListener(v -> {
            mListener.OnLongClick(position);
            return true;
        });

        //点击选中/取消选中图片
        holder.selectLayout.setOnClickListener(v -> {
            checkedImage(holder, fileSource, position);
        });
    }


    /**
     * 设置软件选中和未选中的效果
     */
    private void setItemSelect(FileAdapter.ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.mSelect.setImageResource(R.drawable.ic_select);
            holder.layout.setAlpha(0.3f);//设置imageview透明度
        } else {
            holder.mSelect.setImageResource(R.drawable.ic_image_un_select);
            holder.layout.setAlpha(1f);//设置imageview透明度
        }
    }

    /*选中图片效果*/
    private void checkedImage(FileAdapter.ViewHolder holder, FileSource fileSource, int position) {

        if (isSelect(fileSource)) {//如果图片已经选中，就取消选中
            fragFileList.dataCenterActivity.removeSendFile(fileSource);
            unSelectImage(fileSource, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragFileList.dataCenterActivity.addASendFile(fileSource)) {
                selectImage(fileSource, position);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }
    }

    /**
     * 选中
     *
     * @param fileSource
     */
    private void selectImage(FileSource fileSource, int position) {
        fragFileList.getSelectFileList().add(fileSource);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileSource, true, position);
        }
    }

    /**
     * 取消选中
     *
     * @param fileSource
     */
    private void unSelectImage(FileSource fileSource, int position) {
        List<FileSource> selectList = fragFileList.getSelectFileList();
        if (selectList.size() != 0) {
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

    private boolean isSelect(FileSource fileSource) {
        List<FileSource> selectlist = fragFileList.getSelectFileList();
        if (selectlist != null && selectlist.size() != 0) {
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
        List<FileSource> pathlist = fragFileList.getFileList();
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
        void OnImageSelect(FileSource fileSource, boolean isSelect, int position);
    }

    public interface OnClickListener {
        void OnClick(int position);

        void OnLongClick(int position);
    }
}
