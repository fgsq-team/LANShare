package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.CardView;
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
import com.fgsqw.lanshare.fragment.child.FragAppList;
import com.fgsqw.lanshare.pojo.ApkInfo;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private final Context mContext;
    private final LayoutInflater mInflater;

    FragAppList fragAppList;
    //保存选中的图片
    private List<ApkInfo> mFileUtils;
    private List<ApkInfo> mSelectlist;

    private OnItemClickListener mItemClickListener;


    /**
     * @param isViewImage 是否点击放大图片查看
     */
    //构造方法
    public AppAdapter(FragAppList fragAppList, boolean isViewImage) {
        this.fragAppList = fragAppList;

        mContext = fragAppList.getContext();
        mInflater = LayoutInflater.from(mContext);

        if (fragAppList.mSelectlist != null) {
            if (fragAppList.mSelectlist.size() != 0) {
                this.mSelectlist = fragAppList.mSelectlist;
            }
        } else {
            mSelectlist = new ArrayList<>();
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.app_list_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final ApkInfo apkInfo = mFileUtils.get(position);
        Glide.with(mContext).load(apkInfo.getIcon())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .into(holder.mIcon);
        setItemSelect(holder, isSelect(apkInfo));
        holder.mName.setText(mUtil.StringSize(apkInfo.getName().replace(".apk", ""), 6));
        holder.mSize.setText(FileUtil.computeSize(apkInfo.getLength()));
        holder.itemView.setOnClickListener(v -> checkedImage(holder, apkInfo, v));
        holder.itemView.setOnLongClickListener(view -> {
            int p = holder.getAdapterPosition();
            mItemClickListener.OnLongItenClick(apkInfo, p);
            return true;
        });

    }

    private boolean isSelect(ApkInfo apkInfo) {
        if (mSelectlist != null && mSelectlist.size() != 0) {
            for (int i = 0; i < mSelectlist.size(); i++) {
                if (apkInfo.getPath().equals(mSelectlist.get(i).getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*选中图片效果*/
    private void checkedImage(ViewHolder holder, ApkInfo apkInfo, View view) {
        if (isSelect(apkInfo)) {//如果图片已经选中，就取消选中
            fragAppList.dataCenterActivity.removeSendFile(apkInfo);
            unSelectImage(apkInfo, view);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragAppList.dataCenterActivity.addASendFile(apkInfo)) {
                selectImage(apkInfo, view);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }

    }

    /**
     * 选中软件
     */
    private void selectImage(ApkInfo apkInfo, View view) {
        mSelectlist.add(apkInfo);
        if (mItemClickListener != null) {
            mItemClickListener.OnItemClick(apkInfo, true, mSelectlist, view);
        }

    }

    /**
     * 取消选中软件
     */
    private void unSelectImage(ApkInfo apkInfo, View view) {
        if (mSelectlist != null && mSelectlist.size() != 0) {
            for (int i = 0; i < mSelectlist.size(); i++) {
                if (apkInfo.getPath().equals(mSelectlist.get(i).getPath())) {
                    mSelectlist.remove(i);
                    break;
                }
            }
        }
        // mSelectFileUtils.remove(fileUtils);
        if (mItemClickListener != null) {
            mItemClickListener.OnItemClick(apkInfo, false, mSelectlist, view);
        }
    }

    @Override
    public int getItemCount() {
        return getImageCount();
    }

    private int getImageCount() {
        return mFileUtils == null ? 0 : mFileUtils.size();
    }

    /*
     *获取列表所有内容
     */
    public List<ApkInfo> getData() {
        return mFileUtils;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<ApkInfo> data) {
        mFileUtils = data;
        notifyDataSetChanged();
    }

    public ApkInfo getFirstVisibleImage(int firstVisibleItem) {
        if (mFileUtils != null && !mFileUtils.isEmpty()) {
            return mFileUtils.get(firstVisibleItem);
        }
        return null;
    }

    /**
     * 设置软件选中和未选中的效果
     */
    private void setItemSelect(ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.mSelect.setImageResource(R.drawable.ic_select);
            holder.mIcon.setAlpha(0.3f);//设置imageview透明度
        } else {
            holder.mSelect.setImageResource(R.drawable.ic_null);
            holder.mIcon.setAlpha(1f);//设置imageview透明度
        }
    }

    public void clearImageSelect() {
        if (mSelectlist.size() != 0) {
            fragAppList.dataCenterActivity.removeSendALL(mSelectlist);
            mSelectlist.clear();
            notifyItemRangeChanged(0, mFileUtils.size());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NotifyDataSetChanged")
    public void setSelecteByApkinfo(List<ApkInfo> selected) {
        if (selected != null) {
            for (ApkInfo select : selected) {
                for (ApkInfo apkInfo : mFileUtils) {
                    if (select.equals(apkInfo)) {
                        if (!mSelectlist.contains(apkInfo)) {
                            if (fragAppList.dataCenterActivity.addASendFile(apkInfo)) {
                                mSelectlist.add(apkInfo);
                            }
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }


    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public void remove(ApkInfo apkInfo, int position) {
        mSelectlist.remove(apkInfo);
        mFileUtils.remove(apkInfo);
        notifyItemRemoved(position); // 提醒item删除指定数据，这里有RecyclerView的动画效果
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mSize;
        ImageView mSelect;
        TextView mName;
        ImageView mIcon;
        CardView mClick;

        public ViewHolder(View v) {
            super(v);
            mName = v.findViewById(R.id.app_item_tv_name);
            mIcon = v.findViewById(R.id.app_item_img_icon);
            mSelect = v.findViewById(R.id.app_item_img_select);
            mClick = v.findViewById(R.id.app_item_card);
            mSize = v.findViewById(R.id.app_item_tv_size);
        }
    }


    public interface OnItemClickListener {
        void OnLongItenClick(ApkInfo fileUtils, int position);

        void OnItemClick(ApkInfo fileUtils, boolean isSelect, List<ApkInfo> mSelectFileUtils, View view);//选择取消选择软件
    }
}
