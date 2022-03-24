package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.child.FragPhotoList;
import com.fgsqw.lanshare.pojo.MediaInfo;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    private final Context mContext;//上下文对象
    //    private List<MediaInfo> mFileUtils;   //图片列表
    private final LayoutInflater mInflater;//
    //保存选中的图片
    private OnImageSelectListener mSelectListener;
    private OnItemClickListener mItemClickListener;
    private boolean isViewImage;   // 是否预览媒体
    private final FragPhotoList fragPhotoList;

    /**
     * @param isViewImage 是否点击放大图片查看
     */
    //构造方法
    public PhotoAdapter(FragPhotoList fragPhotoList, boolean isViewImage) {
        this.fragPhotoList = fragPhotoList;
        this.isViewImage = isViewImage;
        mContext = fragPhotoList.getContext();
        this.mInflater = LayoutInflater.from(mContext);
    }

    /*
     *加载view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_list_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        final MediaInfo mediaInfo = fragPhotoList.getCruuentPhotoList().get(position);

        Glide
                .with(mContext)
                .load(mediaInfo.getPath())
                .centerCrop()
                .placeholder(R.drawable.ic_null)
                .into(holder.ivImage);


        setItemSelect(holder, isSelect(mediaInfo));

        if (mediaInfo.isVideo()) {  // 视频
            holder.timeLayout.setVisibility(View.VISIBLE);
            holder.time.setText(mediaInfo.getVideoTime());
        } else {         // 图片
            holder.timeLayout.setVisibility(View.GONE);
            holder.ivGif.setVisibility(mediaInfo.isGif() ? View.VISIBLE : View.GONE);
        }

        //点击选中/取消选中图片
        holder.ivSelectIcon.setOnClickListener(v -> {
            checkedImage(holder, mediaInfo, position);
        });

        holder.itemView.setOnClickListener(v -> {
            if (isViewImage) {//单击图片是否查看，true查看，false选中
                if (mItemClickListener != null) {
                    int p = holder.getAdapterPosition();
                    mItemClickListener.OnItemClick(mediaInfo, p);
                }
            } else {
                checkedImage(holder, mediaInfo, position);
            }
        });
        holder.itemView.setOnLongClickListener(view -> {
            int p = holder.getAdapterPosition();
            mItemClickListener.OnLongItenClick(mediaInfo, p);
            return true;
        });
    }

    private boolean isSelect(MediaInfo fs) {
        List<MediaInfo> selectList = fragPhotoList.getSelectList();
        if (selectList.size() != 0) {
            for (int i = 0; i < selectList.size(); i++) {
                if (fs.getPath().equals(selectList.get(i).getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*选中图片效果*/
    private void checkedImage(ViewHolder holder, MediaInfo mediaInfo, int position) {

        if (isSelect(mediaInfo)) {//如果图片已经选中，就取消选中
            fragPhotoList.dataCenterActivity.removeSendFile(mediaInfo);
            unSelectImage(mediaInfo, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragPhotoList.dataCenterActivity.addASendFile(mediaInfo)) {
                selectImage(mediaInfo, position);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }
    }

    /**
     * 选中图片
     *
     * @param mediaInfo
     */
    private void selectImage(MediaInfo mediaInfo, int position) {
        fragPhotoList.getSelectList().add(mediaInfo);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(mediaInfo, true, position);
        }
    }

    /**
     * 取消选中图片
     *
     * @param mediaInfo
     */
    private void unSelectImage(MediaInfo mediaInfo, int position) {
        List<MediaInfo> selectList = fragPhotoList.getSelectList();
        if (selectList.size() != 0) {
            for (int i = 0; i < selectList.size(); i++) {
                if (mediaInfo.getPath().equals(selectList.get(i).getPath())) {
                    selectList.remove(i);
                    break;
                }
            }
        }
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(mediaInfo, false, position);
        }
    }


    @Override
    public int getItemCount() {
        return getImageCount();
    }

    private int getImageCount() {
        List<MediaInfo> cruuentPhotoList = fragPhotoList.getCruuentPhotoList();
        return cruuentPhotoList == null ? 0 : cruuentPhotoList.size();
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    public MediaInfo getFirstVisibleImage(int firstVisibleItem) {
        List<MediaInfo> cruuentPhotoList = fragPhotoList.getCruuentPhotoList();
        if (cruuentPhotoList != null && !cruuentPhotoList.isEmpty()) {
            return cruuentPhotoList.get(firstVisibleItem);
        }
        return null;
    }

    /**
     * 设置图片选中和未选中的效果
     */
    private void setItemSelect(ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.ivSelectIcon.setImageResource(R.drawable.ic_image_select);
            holder.ivMasking.setAlpha(0.5f);//设置imageview透明度
        } else {
            holder.ivSelectIcon.setImageResource(R.drawable.ic_image_un_select);
            holder.ivMasking.setAlpha(0.2f);//设置imageview透明度
        }
    }

    public void clearThisFolderAllSelect() {
        List<MediaInfo> cruuentPhotoList = fragPhotoList.getCruuentPhotoList();

        if (cruuentPhotoList != null && !fragPhotoList.getSelectList().isEmpty()) {
            fragPhotoList.mSelectList.removeAll(cruuentPhotoList);
            fragPhotoList.dataCenterActivity.removeSendALL(cruuentPhotoList);
            notifyItemRangeChanged(0, cruuentPhotoList.size());
        }

    }

    public void clearAllSelect() {
        List<MediaInfo> cruuentPhotoList = fragPhotoList.getCruuentPhotoList();
        if (cruuentPhotoList != null && fragPhotoList.mSelectList.size() != 0) {
            fragPhotoList.dataCenterActivity.removeSendALL(fragPhotoList.mSelectList);
            fragPhotoList.mSelectList.clear();
            notifyItemRangeChanged(0, cruuentPhotoList.size());
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NotifyDataSetChanged")
    public void setSelecteAll(List<MediaInfo> selected) {
        if (selected != null) {
            for (MediaInfo select : selected) {
                for (MediaInfo mediaInfo : fragPhotoList.getCruuentPhotoList()) {
                    if (select.equals(mediaInfo)) {
                        if (!fragPhotoList.mSelectList.contains(mediaInfo)) {
                            if (fragPhotoList.dataCenterActivity.addASendFile(mediaInfo)) {
                                fragPhotoList.mSelectList.add(mediaInfo);
                            }
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public boolean isSelectAll() {
        return fragPhotoList.mSelectList.containsAll(fragPhotoList.getCruuentPhotoList());
    }


    public List<MediaInfo> getSelectImages() {
        return fragPhotoList.mSelectList;
    }

    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public void remove(MediaInfo img, int position) {
        fragPhotoList.mSelectList.remove(img);
        fragPhotoList.getCruuentPhotoList().remove(img);
        notifyItemRemoved(position); // 提醒item删除指定数据，这里有RecyclerView的动画效果
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;//选中情况imageview
        ImageView ivSelectIcon;
        ImageView ivMasking;
        ImageView ivGif;
        RelativeLayout timeLayout;
        TextView time;


        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.photo_item_img);
            ivSelectIcon = itemView.findViewById(R.id.photo_item_select);
            ivMasking = itemView.findViewById(R.id.photo_item_masking);
            ivGif = itemView.findViewById(R.id.photo_item_gif);
            timeLayout = itemView.findViewById(R.id.photo_item_time_layout);
            time = itemView.findViewById(R.id.photo_item_time);
        }
    }

    public void setViewImage(boolean viewImage) {
        isViewImage = viewImage;
    }

    public interface OnImageSelectListener {
        void OnImageSelect(MediaInfo mediaInfo, boolean isSelect, int position);
    }

    public interface OnItemClickListener {
        void OnItemClick(MediaInfo mediaInfo, int position);

        void OnLongItenClick(MediaInfo mediaInfo, int position);
    }
}
