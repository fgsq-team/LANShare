package com.fgsqw.lanshare.dialog.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

public class ApkUpdateDialogAdapter extends RecyclerView.Adapter<ApkUpdateDialogAdapter.ViewHolder> {
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    private final LayoutInflater mInflater;
    private List<MessageApkContent> fileList;
    private OnClickListener mListener;
    private final Context context;
    private OnImageSelectListener mSelectListener;
    public List<MessageApkContent> fileSelects = new LinkedList<>();

    public ApkUpdateDialogAdapter(Context context, List<MessageApkContent> fileList) {
        this.fileList = fileList;
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ApkUpdateDialogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final ApkUpdateDialogAdapter.ViewHolder holder, int position) {
        final MessageApkContent fileSource = fileList.get(position);
        holder.mName.setText(mUtil.stringSize(fileSource.getName(), 30));
        holder.mInfo.setText(FileUtil.computeSize(fileSource.getLength()) + " V" + fileSource.getVersionName());
        holder.selectLayout.setVisibility(View.VISIBLE);
        Glide.with(context).load(fileSource.getIcon())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(holder.mImg);
        setItemSelect(holder, isSelect(fileSource));
        holder.itemView.setOnClickListener(v -> mListener.OnClick(position));
        holder.itemView.setOnLongClickListener(v -> {
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
    private void setItemSelect(ApkUpdateDialogAdapter.ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.mSelect.setImageResource(R.drawable.ic_select);
            holder.layout.setAlpha(0.3f);//设置imageview透明度
        } else {
            holder.mSelect.setImageResource(R.drawable.ic_image_un_select);
            holder.layout.setAlpha(1f);//设置imageview透明度
        }
    }

    /*选中图片效果*/
    private void checkedImage(ApkUpdateDialogAdapter.ViewHolder holder, MessageApkContent fileSource, int position) {
        if (isSelect(fileSource)) {//如果图片已经选中，就取消选中
            fileSelects.remove(fileSource);
            unSelectImage(fileSource, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果
        } else {
            fileSelects.add(fileSource);
            selectImage(fileSource, position);//选中图片
            setItemSelect(holder, true);//设置图片选中效果

        }
    }

    /**
     * @author fgsq
     * @comments 选中
     * @date 2024/5/21 9:59
     */
    private void selectImage(MessageApkContent fileSource, int position) {
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileSource, true, position);
        }
    }

    /**
     * @author fgsq
     * @comments 取消选中
     * @date 2024/5/21 9:59
     */
    private void unSelectImage(MessageApkContent fileSource, int position) {
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileSource, false, position);
        }
    }

    private boolean isSelect(MessageApkContent fileSource) {
        return fileSelects.contains(fileSource);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void selectAll() {
        fileSelects.clear();
        fileSelects.addAll(fileList);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void unSelectAll() {
        fileSelects.clear();
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public List<MessageApkContent> getFileSelects() {
        return fileSelects;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<MessageApkContent> fileList) {      //更换列表数据
        this.fileList = fileList;
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
        void OnImageSelect(MessageApkContent fileSource, boolean isSelect, int position);
    }

    public interface OnClickListener {
        void OnClick(int position);

        void OnLongClick(int position);
    }
}
