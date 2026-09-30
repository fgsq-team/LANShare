package com.fgsqw.lanshare.fragment.adapter;

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
import com.fgsqw.lanshare.fragment.child.FragmentMusic;
import com.fgsqw.lanshare.pojo.file.FileInfo;
import com.fgsqw.lanshare.pojo.file.MusicInfo;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.text.SimpleDateFormat;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    private final LayoutInflater mInflater;
    private final FragmentMusic fragmentFileList;
    private final Context context;
    private OnClickListener mListener;
    private OnImageSelectListener mSelectListener;

    public MusicAdapter(FragmentMusic fragmentFileList) {
        this.context = fragmentFileList.getContext();
        this.fragmentFileList = fragmentFileList;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MusicAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final MusicAdapter.ViewHolder holder, int position) {
        final MusicInfo fileSource = fragmentFileList.getMusicList().get(position);
        holder.mName.setText(mUtil.stringSize(fileSource.getName(), 30));
        holder.mInfo.setText(format.format(fileSource.getTime()) + " "
                + FileUtil.computeSize(fileSource.getLength()));
        holder.selectLayout.setVisibility(View.VISIBLE);

/*
        if (fileSource.isPreView()) {
            Glide.with(context).load(fileSource.getPath())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.mImg);
        } else {
            Glide.with(context).load(fileSource.getPreView())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.mImg);
        }
*/

        Glide.with(context).load(R.drawable.ic_music)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(holder.mImg);

        setItemSelect(holder, isSelect(fileSource));

        holder.itemView.setOnClickListener(v -> checkedImage(holder, fileSource, position));
        holder.itemView.setOnLongClickListener(v -> {
            mListener.OnLongClick(position);
            return true;
        });

        //点击选中/取消选中图片
        holder.selectLayout.setOnClickListener(v -> {
            checkedImage(holder, fileSource, position);
        });
        holder.mImg.setOnClickListener(v -> mListener.OnImageClick(position));
    }


    /**
     * 设置软件选中和未选中的效果
     */
    private void setItemSelect(MusicAdapter.ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.mSelect.setImageResource(R.drawable.ic_select);
            holder.layout.setAlpha(0.3f);//设置imageview透明度
        } else {
            holder.mSelect.setImageResource(R.drawable.ic_image_un_select);
            holder.layout.setAlpha(1f);//设置imageview透明度
        }
    }

    /*选中图片效果*/
    private void checkedImage(MusicAdapter.ViewHolder holder, MusicInfo fileSource, int position) {

        if (isSelect(fileSource)) {//如果图片已经选中，就取消选中
            fragmentFileList.dataCenterActivity.removeSendFile(fileSource);
            unSelectImage(fileSource, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragmentFileList.dataCenterActivity.addASendFile(fileSource)) {
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
    private void selectImage(MusicInfo fileSource, int position) {
        fragmentFileList.getSelectMusicList().add(fileSource);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileSource, true, position);
        }
    }

    /**
     * 取消选中
     *
     * @param fileSource
     */
    private void unSelectImage(MusicInfo fileSource, int position) {
        List<MusicInfo> selectList = fragmentFileList.getSelectMusicList();
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

    private boolean isSelect(MusicInfo fileSource) {
        List<MusicInfo> selectlist = fragmentFileList.getSelectMusicList();
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
        List<MusicInfo> pathlist = fragmentFileList.getMusicList();
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
        void OnImageSelect(FileInfo fileSource, boolean isSelect, int position);
    }

    public interface OnClickListener {
        void OnClick(int position);

        void OnLongClick(int position);

        void OnImageClick(int position);
    }
}
