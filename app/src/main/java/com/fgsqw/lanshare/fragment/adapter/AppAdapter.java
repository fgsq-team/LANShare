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
    private OnItemClickListener mItemClickListener;


    //构造方法
    public AppAdapter(FragAppList fragAppList) {
        this.fragAppList = fragAppList;

        mContext = fragAppList.getContext();
        mInflater = LayoutInflater.from(mContext);

    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.app_list_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final ApkInfo apkInfo = fragAppList.getApkFileList().get(position);
        Glide.with(mContext).load(apkInfo.getIcon())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .into(holder.mIcon);
        setItemSelect(holder, isSelect(apkInfo));
        holder.mName.setText(mUtil.StringSize(apkInfo.getName().replace(".apk", ""), 6));
        holder.mSize.setText(FileUtil.computeSize(apkInfo.getLength()));
        holder.itemView.setOnClickListener(v -> checkedImage(holder, apkInfo, position));
        holder.itemView.setOnLongClickListener(view -> {
            int p = holder.getAdapterPosition();
            mItemClickListener.OnLongItenClick(apkInfo, p);
            return true;
        });

    }

    private boolean isSelect(ApkInfo apkInfo) {
        List<ApkInfo> selectlist = fragAppList.getSelectlist();
        if (selectlist != null && selectlist.size() != 0) {
            for (int i = 0; i < selectlist.size(); i++) {
                if (apkInfo.getPath().equals(selectlist.get(i).getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*选中图片效果*/
    private void checkedImage(ViewHolder holder, ApkInfo apkInfo, int position) {
        if (isSelect(apkInfo)) {//如果图片已经选中，就取消选中
            fragAppList.dataCenterActivity.removeSendFile(apkInfo);
            unSelectImage(apkInfo, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragAppList.dataCenterActivity.addASendFile(apkInfo)) {
                selectImage(apkInfo, position);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }

    }

    /**
     * 选中软件
     */
    private void selectImage(ApkInfo apkInfo, int position) {
        fragAppList.getSelectlist().add(apkInfo);
        if (mItemClickListener != null) {
            mItemClickListener.OnItemClick(apkInfo, true, position);
        }

    }

    /**
     * 取消选中软件
     */
    private void unSelectImage(ApkInfo apkInfo, int position) {
        if (fragAppList.getSelectlist() != null && fragAppList.getSelectlist().size() != 0) {
            for (int i = 0; i < fragAppList.getSelectlist().size(); i++) {
                if (apkInfo.getPath().equals(fragAppList.getSelectlist().get(i).getPath())) {
                    fragAppList.getSelectlist().remove(i);
                    break;
                }
            }
        }
        // mSelectFileUtils.remove(fileUtils);
        if (mItemClickListener != null) {
            mItemClickListener.OnItemClick(apkInfo, false, position);
        }
    }

    @Override
    public int getItemCount() {
        return getImageCount();
    }

    private int getImageCount() {
        return fragAppList.getApkFileList() == null ? 0 : fragAppList.getApkFileList().size();
    }


    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        notifyDataSetChanged();
    }

    public ApkInfo getFirstVisibleImage(int firstVisibleItem) {
        if (fragAppList.getApkFileList() != null && !fragAppList.getApkFileList().isEmpty()) {
            return fragAppList.getApkFileList().get(firstVisibleItem);
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
        if (fragAppList.getSelectlist().size() != 0) {
            fragAppList.dataCenterActivity.removeSendALL(fragAppList.getSelectlist());
            fragAppList.getSelectlist().clear();
            notifyItemRangeChanged(0, fragAppList.getApkFileList().size());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NotifyDataSetChanged")
    public void setSelecteByApkinfo(List<ApkInfo> selected) {
        if (selected != null) {
            for (ApkInfo select : selected) {
                for (ApkInfo apkInfo : fragAppList.getApkFileList()) {
                    if (select.equals(apkInfo)) {
                        if (!fragAppList.getSelectlist().contains(apkInfo)) {
                            if (fragAppList.dataCenterActivity.addASendFile(apkInfo)) {
                                fragAppList.getSelectlist().add(apkInfo);
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
        fragAppList.getSelectlist().remove(apkInfo);
        fragAppList.getApkFileList().remove(apkInfo);
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
        void OnLongItenClick(ApkInfo apkInfo, int position);

        void OnItemClick(ApkInfo apkInfo, boolean isSelect, int position);//选择取消选择软件
    }
}
