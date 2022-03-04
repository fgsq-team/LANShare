package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.fgsqw.lanshare.pojo.FileSource;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.text.SimpleDateFormat;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    private LayoutInflater mInflater;
    private Context context;
    List<FileSource> path;//列表数据
    OnClickListener mListener;

    public FileAdapter(Context context, List<FileSource> path) {
        this.path = path;
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final FileAdapter.ViewHolder holder, int position) {
        final FileSource fileSource = path.get(position);
        holder.mName.setText(mUtil.StringSize(fileSource.getName(), 30));
        if (position != 0) {
            holder.mInfo.setText(format.format(fileSource.getTime()) + " "
                    + FileUtil.computeSize(fileSource.getLength()));
        } else {
            holder.mInfo.setText("");
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

        holder.layout.setOnClickListener(v -> mListener.OnClick(position));
        holder.layout.setOnLongClickListener(v -> {
            mListener.OnLongClick(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return path == null ? 0 : path.size();
    }

    public void refresh(List<FileSource> path) {      //更换列表数据
        this.path = path;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mInfo;
        ImageView mImg;
        LinearLayout layout;

        public ViewHolder(View v) {
            super(v);
            mName = v.findViewById(R.id.file_item_name);
            mImg = v.findViewById(R.id.file_item_img);
            layout = v.findViewById(R.id.file_item_layout);
            mInfo = v.findViewById(R.id.file_item_info);
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void OnClick(int position);

        void OnLongClick(int position);
    }
}
