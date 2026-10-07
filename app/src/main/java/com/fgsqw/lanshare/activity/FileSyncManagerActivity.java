package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.db.FileSyncDBUtil;
import com.fgsqw.lanshare.pojo.FileSyncData;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FileSyncManagerActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView recyclerView;
    private List<FileSyncData> itemList;
    private ItemAdapter itemAdapter;
    private FileSyncDBUtil fileSyncDBUtil;
    private ImageView webManagerExitImg;
    private TextView webManagerTitle;
    private MaterialButton addButton;
    private LinearLayout emptyLayout;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_manager);
        fileSyncDBUtil = new FileSyncDBUtil(this);
        recyclerView = bind(R.id.web_manager_recy);
        webManagerExitImg = bind(R.id.web_manager_exit_img);
        addButton = bind(R.id.web_manager_add_button);
        webManagerTitle = bind(R.id.web_manager_title);
        emptyLayout = bind(R.id.web_manager_empty_layout);
        emptyText = bind(R.id.web_manager_empty_text);
        itemList = generateItemList();
        itemAdapter = new ItemAdapter(itemList);
        webManagerExitImg.setOnClickListener(this);
        addButton.setOnClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemAdapter);
        addButton.setVisibility(View.GONE);
        webManagerTitle.setText(R.string.file_sync_device_list);
        updateEmptyState();
    }

    private List<FileSyncData> generateItemList() {
        return fileSyncDBUtil.queryList();
    }

    private void updateEmptyState() {
        if (itemList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            emptyText.setText(R.string.no_sync_devices);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.web_manager_exit_img) {
            finish();
        }
    }


    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

        private List<FileSyncData> itemList;

        ItemAdapter(List<FileSyncData> itemList) {
            this.itemList = itemList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.web_client_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FileSyncData item = itemList.get(position);
            holder.textView.setText(item.getDeviceName());
            holder.tvName.setText(item.getName());
            holder.layoutName.setVisibility(View.VISIBLE);
            holder.buttonDelete.setOnClickListener(v -> deleteItem(item));
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        private void deleteItem(FileSyncData token) {
            fileSyncDBUtil.deleteFileSyncData(token.getDeviceId());
            itemList.remove(token);
            refresh();
            updateEmptyState();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void refresh() {
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            TextView tvName;
            LinearLayout layoutName;
            MaterialButton buttonDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
                tvName = itemView.findViewById(R.id.tv_name);
                layoutName = itemView.findViewById(R.id.layout_name);
            }
        }
    }
}
