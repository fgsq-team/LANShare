package com.fgsqw.lanshare.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.db.TokenDBUtil;
import com.fgsqw.lanshare.dialog.EditTextDialog;
import com.fgsqw.lanshare.pojo.Token;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.NetWorkUtil;
import com.fgsqw.lanshare.utils.StringUtils;

import java.util.List;

public class WebClientManagerActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView recyclerView;
    private List<Token> itemList;
    private ItemAdapter itemAdapter;
    private TokenDBUtil tokenDBUtil;
    private ImageView webManagerExitImg;
    private Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_manager);
        tokenDBUtil = new TokenDBUtil(this);
        recyclerView = findViewById(R.id.web_manager_recy);
        webManagerExitImg = findViewById(R.id.web_manager_exit_img);
        addButton = findViewById(R.id.web_manager_add_button);
        itemList = generateItemList();
        itemAdapter = new ItemAdapter(itemList);
        webManagerExitImg.setOnClickListener(this);
        addButton.setOnClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemAdapter);
    }

    private List<Token> generateItemList() {
        return tokenDBUtil.queryList();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.web_manager_exit_img) {
            finish();
        } else if (v.getId() == R.id.web_manager_add_button) {
            EditTextDialog editTextDialog = new EditTextDialog(this, false, getString(R.string.please_enter_an_ip_address), "");
            editTextDialog.setOnClickListener((ok, ip) -> {
                if (!NetWorkUtil.isIPv4Address(ip)) {
                    T.s((R.string.please_enter_the_correct_ip_address));
                    return;
                }
                tokenDBUtil.addToken(StringUtils.getUUID(), true, 1, ip, "");
                Token token = new Token();
                token.setToken(StringUtils.getUUID());
                token.setCustom(true);
                token.setIp(ip);
                itemList.add(token);
                itemAdapter.refresh();
            });
            editTextDialog.setMaxLen(16).show();
        }
    }


    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

        private List<Token> itemList;

        ItemAdapter(List<Token> itemList) {
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
            Token item = itemList.get(position);
            holder.textView.setText(item.getIp());
            holder.buttonDelete.setOnClickListener(v -> deleteItem(item));
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        private void deleteItem(Token token) {
            tokenDBUtil.delToken(token.getToken());
            itemList.remove(token);
            refresh();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void refresh() {
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            Button buttonDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
            }
        }
    }
}
