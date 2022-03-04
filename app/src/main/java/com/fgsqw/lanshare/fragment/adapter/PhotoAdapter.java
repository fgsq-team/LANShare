package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
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
import com.fgsqw.lanshare.pojo.PhotoInfo;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    private Context mContext;//上下文对象
    private List<PhotoInfo> mFileUtils;   //图片列表
    private final LayoutInflater mInflater;//
    //保存选中的图片
    private OnImageSelectListener mSelectListener;
    private OnItemClickListener mItemClickListener;
    private boolean isViewImage;//是否放大查看图片标志位
    private final FragPhotoList fragPhotoList;

    /**
     * @param isViewImage 是否点击放大图片查看
     */
    //构造方法
    public PhotoAdapter(FragPhotoList fragPhotoList, boolean isViewImage) {
        this.fragPhotoList = fragPhotoList;
        mContext = fragPhotoList.getContext();
        this.mInflater = LayoutInflater.from(mContext);
        this.isViewImage = isViewImage;


    }

    /*
     *加载view
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_list_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        final PhotoInfo photoInfo = mFileUtils.get(position);
        Glide
                .with(mContext)
                .load(photoInfo.getPath())
                .centerCrop()
                .placeholder(R.drawable.ic_null)
                .into(holder.ivImage);
        setItemSelect(holder, isSelect(photoInfo));

        if (photoInfo.isVideo()) {
            holder.timeLayout.setVisibility(View.VISIBLE);
            holder.time.setText(photoInfo.getVideoTime());
        } else {
            holder.timeLayout.setVisibility(View.GONE);
            holder.ivGif.setVisibility(photoInfo.isGif() ? View.VISIBLE : View.GONE);
        }
        //点击选中/取消选中图片
        holder.ivSelectIcon.setOnClickListener(v -> checkedImage(holder, photoInfo, holder.itemView));

        holder.itemView.setOnClickListener(v -> {
            if (isViewImage) {//单击图片是否查看，true查看，false选中
                if (mItemClickListener != null) {
                    int p = holder.getAdapterPosition();
                    mItemClickListener.OnItemClick(photoInfo, p);
                }
            } else {
                checkedImage(holder, photoInfo, v);
            }
        });
        holder.itemView.setOnLongClickListener(view -> {
            int p = holder.getAdapterPosition();
            mItemClickListener.OnLongItenClick(photoInfo, p);
            return true;
        });
    }

    private boolean isSelect(PhotoInfo fs) {
        if (fragPhotoList.mSelectFile != null && fragPhotoList.mSelectFile.size() != 0) {
            for (int i = 0; i < fragPhotoList.mSelectFile.size(); i++) {
                if (fs.getPath().equals(fragPhotoList.mSelectFile.get(i).getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*选中图片效果*/
    private void checkedImage(ViewHolder holder, PhotoInfo photoInfo, View view) {

        if (isSelect(photoInfo)) {//如果图片已经选中，就取消选中
            fragPhotoList.dataCenterActivity.removeSendFile(photoInfo);
            unSelectImage(photoInfo, view);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragPhotoList.dataCenterActivity.addASendFile(photoInfo)) {
                selectImage(photoInfo, view);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }
    }

    /**
     * 选中图片
     *
     * @param fileUtils
     */
    private void selectImage(PhotoInfo fileUtils, View view) {
        fragPhotoList.mSelectFile.add(fileUtils);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileUtils, true, view);
        }
    }

    /**
     * 取消选中图片
     *
     * @param fileUtils
     */
    private void unSelectImage(PhotoInfo fileUtils, View view) {
        if (fragPhotoList.mSelectFile != null && fragPhotoList.mSelectFile.size() != 0) {
            for (int i = 0; i < fragPhotoList.mSelectFile.size(); i++) {
                if (fileUtils.getPath().equals(fragPhotoList.mSelectFile.get(i).getPath())) {
                    fragPhotoList.mSelectFile.remove(i);
                    break;
                }
            }
        }
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileUtils, false, view);
        }
    }


    @Override
    public int getItemCount() {
        return getImageCount();
    }

    private int getImageCount() {
        return mFileUtils == null ? 0 : mFileUtils.size();
    }

    public List<PhotoInfo> getData() {
        return mFileUtils;
    }

    public void refresh(List<PhotoInfo> data) {
        mFileUtils = data;
        notifyDataSetChanged();
    }

    public PhotoInfo getFirstVisibleImage(int firstVisibleItem) {
        if (mFileUtils != null && !mFileUtils.isEmpty()) {
            return mFileUtils.get(firstVisibleItem);
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
        if (mFileUtils != null && !fragPhotoList.mSelectFile.isEmpty()) {
            fragPhotoList.mSelectFile.removeAll(mFileUtils);
            fragPhotoList.dataCenterActivity.removeSendALL(mFileUtils);
            notifyItemRangeChanged(0, mFileUtils.size());
        }
    }

    public void clearAllSelect() {
        if (mFileUtils != null && fragPhotoList.mSelectFile.size() != 0) {
            fragPhotoList.dataCenterActivity.removeSendALL(fragPhotoList.mSelectFile);
            fragPhotoList.mSelectFile.clear();
            notifyItemRangeChanged(0, mFileUtils.size());
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NotifyDataSetChanged")
    public void setSelecteAll(List<PhotoInfo> selected) {
        if (selected != null) {
            for (PhotoInfo select : selected) {
                for (PhotoInfo photoInfo : mFileUtils) {
                    if (select.equals(photoInfo)) {
                        if (!fragPhotoList.mSelectFile.contains(photoInfo)) {
                            if (fragPhotoList.dataCenterActivity.addASendFile(photoInfo)) {
                                fragPhotoList.mSelectFile.add(photoInfo);
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
        return fragPhotoList.mSelectFile.containsAll(mFileUtils);
    }


    public List<PhotoInfo> getSelectImages() {
        return fragPhotoList.mSelectFile;
    }

    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public void remove(PhotoInfo img, int position) {
        if (fragPhotoList.mSelectFile.contains(img)) {
            fragPhotoList.mSelectFile.remove(img);
        }
        mFileUtils.remove(img);
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

    public interface OnImageSelectListener {
        void OnImageSelect(PhotoInfo photoInfo, boolean isSelect, View view);
    }

    public interface OnItemClickListener {
        void OnItemClick(PhotoInfo fileUtils, int position);

        void OnLongItenClick(PhotoInfo fileUtils, int position);
    }
}
