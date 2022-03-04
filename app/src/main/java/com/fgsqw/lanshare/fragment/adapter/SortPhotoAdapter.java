package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.PhotoFolder;
import com.fgsqw.lanshare.pojo.PhotoInfo;

import java.io.File;
import java.util.List;

public class SortPhotoAdapter extends RecyclerView.Adapter<SortPhotoAdapter.ViewHolder> {

    private Context mContext;
    private List<PhotoFolder> mFolders;
    private LayoutInflater mInflater;

    private OnFolderSelectListener mListener;

    public SortPhotoAdapter(Context context, List<PhotoFolder> folders) {
        mContext = context;
        mFolders = folders;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_folder_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PhotoFolder folder = mFolders.get(position);
        List<PhotoInfo> fileUtils = folder.getImages();
        holder.tvFolderName.setText(folder.getName());
        if (fileUtils != null && !fileUtils.isEmpty()) {
            holder.tvFolderSize.setText(fileUtils.size() + "");
            Glide.with(mContext).load(new File(fileUtils.get(0).getPath()))
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .into(holder.ivImage);
        } else {
            holder.tvFolderSize.setText("0");
            holder.ivImage.setImageBitmap(null);
        }

        holder.itemView.setOnClickListener(v -> {
            notifyDataSetChanged();
            if (mListener != null) {
                mListener.OnFolderSelect(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFolders == null ? 0 : mFolders.size();
    }

    public void setOnFolderSelectListener(OnFolderSelectListener listener) {
        this.mListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
        TextView tvFolderName;
        TextView tvFolderSize;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.photo_folder_item_img);
            tvFolderName = itemView.findViewById(R.id.photo_folder_item_img_tv_name);
            tvFolderSize = itemView.findViewById(R.id.photo_folder_item_img_tv_size);
        }
    }

    public interface OnFolderSelectListener {
        void OnFolderSelect(PhotoFolder folder);

        void OnImageFoderSelect(PhotoFolder folder);
    }

}
