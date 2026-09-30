package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.child.FragmentAppList;
import com.fgsqw.lanshare.pojo.message.MessageApkContent;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final FragmentAppList fragmentAppList;
    private OnItemClickListener mItemClickListener;

    //构造方法
    public AppAdapter(FragmentAppList fragmentAppList) {
        this.fragmentAppList = fragmentAppList;
        mContext = fragmentAppList.getContext();
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
        final MessageApkContent apkInfo = fragmentAppList.getApkFileList().get(position);
        Glide.with(mContext)
                .load(apkInfo.getIcon())
                .centerCrop()
                .placeholder(R.drawable.ic_null)
                .into(holder.mIcon);
        setItemSelect(holder, isSelect(apkInfo));
        holder.mName.setText(mUtil.stringSize(apkInfo.getName().replace(".apk", ""), 6));
        holder.mSize.setText(FileUtil.computeSize(apkInfo.getLength()));
        holder.itemView.setOnClickListener(v -> checkedImage(holder, apkInfo, position));
        holder.itemView.setOnLongClickListener(view -> {
            int p = holder.getAdapterPosition();
            mItemClickListener.OnLongItenClick(apkInfo, p);
            return true;
        });

    }

    private boolean isSelect(MessageApkContent apkInfo) {
        List<MessageApkContent> selectlist = fragmentAppList.getSelectlist();
        if (selectlist != null && !selectlist.isEmpty()) {
            for (int i = 0; i < selectlist.size(); i++) {
                if (apkInfo.getPath().equals(selectlist.get(i).getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*选中图片效果*/
    private void checkedImage(ViewHolder holder, MessageApkContent apkInfo, int position) {
        if (isSelect(apkInfo)) {//如果图片已经选中，就取消选中
            fragmentAppList.dataCenterActivity.removeSendFile(apkInfo);
            unSelectImage(apkInfo, position);//取消选中图片
            setItemSelect(holder, false);//设置图片选中效果

        } else {//如果未选中就选中
            if (fragmentAppList.dataCenterActivity.addASendFile(apkInfo)) {
                selectImage(apkInfo, position);//选中图片
                setItemSelect(holder, true);//设置图片选中效果
            }
        }

    }

    /**
     * 选中软件
     */
    private void selectImage(MessageApkContent apkInfo, int position) {
        fragmentAppList.getSelectlist().add(apkInfo);
        if (mItemClickListener != null) {
            mItemClickListener.OnItemClick(apkInfo, true, position);
        }

    }

    /**
     * 取消选中软件
     */
    private void unSelectImage(MessageApkContent apkInfo, int position) {
        if (fragmentAppList.getSelectlist() != null && !fragmentAppList.getSelectlist().isEmpty()) {
            for (int i = 0; i < fragmentAppList.getSelectlist().size(); i++) {
                if (apkInfo.getPath().equals(fragmentAppList.getSelectlist().get(i).getPath())) {
                    fragmentAppList.getSelectlist().remove(i);
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
        return fragmentAppList.getApkFileList() == null ? 0 : fragmentAppList.getApkFileList().size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        notifyDataSetChanged();
    }

    public MessageApkContent getFirstVisibleImage(int firstVisibleItem) {
        if (fragmentAppList.getApkFileList() != null && !fragmentAppList.getApkFileList().isEmpty()) {
            return fragmentAppList.getApkFileList().get(firstVisibleItem);
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
        if (!fragmentAppList.getSelectlist().isEmpty()) {
            fragmentAppList.dataCenterActivity.removeSendALL(fragmentAppList.getSelectlist());
            fragmentAppList.getSelectlist().clear();
            notifyItemRangeChanged(0, fragmentAppList.getApkFileList().size());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NotifyDataSetChanged")
    public void setSelecteByApkinfo(List<MessageApkContent> selected) {
        if (selected != null) {
            for (MessageApkContent select : selected) {
                for (MessageApkContent apkInfo : fragmentAppList.getApkFileList()) {
                    if (select.equals(apkInfo)) {
                        if (!fragmentAppList.getSelectlist().contains(apkInfo)) {
                            if (fragmentAppList.dataCenterActivity.addASendFile(apkInfo)) {
                                fragmentAppList.getSelectlist().add(apkInfo);
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

    public void remove(MessageApkContent apkInfo, int position) {
        fragmentAppList.getSelectlist().remove(apkInfo);
        fragmentAppList.getApkFileList().remove(apkInfo);
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
        void OnLongItenClick(MessageApkContent apkInfo, int position);
        void OnItemClick(MessageApkContent apkInfo, boolean isSelect, int position);//选择取消选择软件
    }
}
