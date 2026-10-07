package com.fgsqw.lanshare.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InfoDialog extends BaseDialog implements View.OnClickListener {

    // 弹窗模式
    public static final int MODE_CONFIRM = 0;       // 确认弹窗（标题+消息+按钮）
    public static final int MODE_LIST = 1;           // 列表弹窗（标题+列表项）
    public static final int MODE_SINGLE_CHOICE = 2;  // 单选弹窗（标题+单选列表+按钮）
    public static final int MODE_MULTI_CHOICE = 3;   // 多选弹窗（标题+多选列表+按钮）
    public static final int MODE_CUSTOM_VIEW = 4;    // 自定义视图弹窗

    private LinearLayout infoLayoutLeft;
    private LinearLayout infoLayoutRight;
    private TextView infoTextLeft;
    private TextView infoTextRight;
    private TextView infoText;
    private TextView infoTips;
    private RecyclerView infoList;
    private LinearLayout buttonArea;

    private String text;
    private String title;
    private String leftButtonText;
    private String rightButtonText;
    private String[] items;
    private int mode = MODE_CONFIRM;
    private int selectedPosition = -1;
    private boolean[] checkedItems;
    private View customView;

    // 回调监听器
    private OnClickListener onClickListener;
    private OnItemClickListener onItemClickListener;
    private OnSingleChoiceListener onSingleChoiceListener;
    private OnMultiChoiceListener onMultiChoiceListener;

    public InfoDialog(@NonNull Context context) {
        super(context, R.style.AlertDialogTheme);
    }

    public InfoDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected InfoDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    // ========== 设置器 ==========

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String text) {
        this.title = text;
    }

    public void setLeftButtonText(String text) {
        leftButtonText = text;
    }

    public void setRightButtonText(String text) {
        rightButtonText = text;
    }

    public void setItems(String[] items) {
        this.items = items;
        this.mode = MODE_LIST;
    }

    public void setSingleChoiceItems(String[] items, int selectedPosition) {
        this.items = items;
        this.selectedPosition = selectedPosition;
        this.mode = MODE_SINGLE_CHOICE;
    }

    public void setMultiChoiceItems(String[] items, boolean[] checkedItems) {
        this.items = items;
        this.checkedItems = checkedItems;
        this.mode = MODE_MULTI_CHOICE;
    }

    public void setCustomView(View view) {
        this.customView = view;
        this.mode = MODE_CUSTOM_VIEW;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    // ========== 监听器设置 ==========

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnSingleChoiceListener(OnSingleChoiceListener onSingleChoiceListener) {
        this.onSingleChoiceListener = onSingleChoiceListener;
    }

    public void setOnMultiChoiceListener(OnMultiChoiceListener onMultiChoiceListener) {
        this.onMultiChoiceListener = onMultiChoiceListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_dialog_enhanced);

        // 设置弹窗宽度为屏幕宽度的 85%，避免弹窗过宽
        Window window = getWindow();
        if (window != null) {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.85);
            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        infoLayoutLeft = findViewById(R.id.info_layout_left);
        infoLayoutRight = findViewById(R.id.info_layout_right);
        infoTextLeft = findViewById(R.id.info_text_left);
        infoTextRight = findViewById(R.id.info_text_right);
        infoText = findViewById(R.id.info_text);
        infoTips = findViewById(R.id.info_tips);
        infoList = findViewById(R.id.info_list);
        buttonArea = findViewById(R.id.info_button_area);

        // 设置标题
        if (!StringUtils.isEmpty(title)) {
            infoTips.setText(title);
        }

        // 默认按钮文字
        if (StringUtils.isEmpty(rightButtonText)) {
            rightButtonText = getContext().getString(R.string.confirm);
        }
        if (StringUtils.isEmpty(leftButtonText)) {
            leftButtonText = getContext().getString(R.string.cancel);
        }

        switch (mode) {
            case MODE_CONFIRM:
                setupConfirmMode();
                break;
            case MODE_LIST:
                setupListMode();
                break;
            case MODE_SINGLE_CHOICE:
                setupSingleChoiceMode();
                break;
            case MODE_MULTI_CHOICE:
                setupMultiChoiceMode();
                break;
            case MODE_CUSTOM_VIEW:
                setupCustomViewMode();
                break;
        }
    }

    private void setupConfirmMode() {
        // 显示消息文本
        if (!StringUtils.isEmpty(text)) {
            infoText.setText(text);
            infoText.setVisibility(View.VISIBLE);
        }

        // 显示按钮区域
        buttonArea.setVisibility(View.VISIBLE);
        infoTextRight.setText(rightButtonText);

        // 如果左按钮文字为空，则隐藏左按钮
        if (StringUtils.isEmpty(leftButtonText)) {
            infoLayoutLeft.setVisibility(View.GONE);
            // 右按钮占满宽度
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            infoLayoutRight.setLayoutParams(params);
        } else {
            infoTextLeft.setText(leftButtonText);
            infoLayoutLeft.setOnClickListener(this);
        }

        infoLayoutRight.setOnClickListener(this);
        infoLayoutRight.requestFocus();
    }

    private void setupListMode() {
        // 隐藏消息文本
        infoText.setVisibility(View.GONE);

        // 设置列表
        infoList.setVisibility(View.VISIBLE);
        infoList.setLayoutManager(new LinearLayoutManager(getContext()));
        infoList.setAdapter(new ListItemAdapter(items, MODE_LIST, -1, null));

        // 列表模式不显示按钮区域
        buttonArea.setVisibility(View.GONE);
    }

    private void setupSingleChoiceMode() {
        // 隐藏消息文本
        infoText.setVisibility(View.GONE);

        // 设置单选列表
        infoList.setVisibility(View.VISIBLE);
        infoList.setLayoutManager(new LinearLayoutManager(getContext()));
        final SingleChoiceAdapter adapter = new SingleChoiceAdapter(items, selectedPosition);
        infoList.setAdapter(adapter);

        // 显示按钮区域
        buttonArea.setVisibility(View.VISIBLE);
        infoTextRight.setText(rightButtonText);
        infoTextLeft.setText(leftButtonText);

        infoLayoutLeft.setOnClickListener(v -> {
            if (onSingleChoiceListener != null) {
                onSingleChoiceListener.onCancel();
            }
            dismiss();
        });

        infoLayoutRight.setOnClickListener(v -> {
            if (onSingleChoiceListener != null) {
                onSingleChoiceListener.onConfirm(adapter.getSelectedPosition());
            }
            dismiss();
        });
    }

    private void setupMultiChoiceMode() {
        // 隐藏消息文本
        infoText.setVisibility(View.GONE);

        // 设置多选列表
        infoList.setVisibility(View.VISIBLE);
        infoList.setLayoutManager(new LinearLayoutManager(getContext()));
        final MultiChoiceAdapter adapter = new MultiChoiceAdapter(items, checkedItems);
        infoList.setAdapter(adapter);

        // 显示按钮区域
        buttonArea.setVisibility(View.VISIBLE);
        infoTextRight.setText(rightButtonText);
        infoTextLeft.setText(leftButtonText);

        infoLayoutLeft.setOnClickListener(v -> {
            if (onMultiChoiceListener != null) {
                onMultiChoiceListener.onCancel();
            }
            dismiss();
        });

        infoLayoutRight.setOnClickListener(v -> {
            if (onMultiChoiceListener != null) {
                onMultiChoiceListener.onConfirm(adapter.getCheckedItems());
            }
            dismiss();
        });
    }

    private void setupCustomViewMode() {
        // 隐藏消息文本和列表
        infoText.setVisibility(View.GONE);
        infoList.setVisibility(View.GONE);

        // 添加自定义视图
        if (customView != null) {
            LinearLayout contentArea = (LinearLayout) infoText.getParent();
            int index = contentArea.indexOfChild(infoList);
            contentArea.addView(customView, index);
        }

        // 自定义视图模式不显示按钮区域（由外部自行处理）
        buttonArea.setVisibility(View.GONE);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.info_layout_left: {
                if (onClickListener != null) {
                    onClickListener.onClick(false);
                }
                dismiss();
                break;
            }
            case R.id.info_layout_right: {
                if (onClickListener != null) {
                    onClickListener.onClick(true);
                }
                dismiss();
                break;
            }
            default:
                break;
        }
    }

    // ========== 列表适配器 ==========

    /**
     * 普通列表适配器（无按钮模式）
     */
    private class ListItemAdapter extends RecyclerView.Adapter<ListItemViewHolder> {
        private final String[] items;
        private final int listMode;
        private final int selectedPos;
        private final boolean[] checked;

        ListItemAdapter(String[] items, int listMode, int selectedPos, boolean[] checked) {
            this.items = items;
            this.listMode = listMode;
            this.selectedPos = selectedPos;
            this.checked = checked;
        }

        @NonNull
        @Override
        public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.info_dialog_list_item, parent, false);
            return new ListItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ListItemViewHolder holder, int position) {
            holder.textView.setText(items[position]);
            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position);
                }
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.length : 0;
        }
    }

    /**
     * 单选列表适配器
     */
    private class SingleChoiceAdapter extends RecyclerView.Adapter<ListItemViewHolder> {
        private final String[] items;
        private int selectedPosition;

        SingleChoiceAdapter(String[] items, int selectedPosition) {
            this.items = items;
            this.selectedPosition = selectedPosition;
        }

        int getSelectedPosition() {
            return selectedPosition;
        }

        @NonNull
        @Override
        public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.info_dialog_list_item, parent, false);
            return new ListItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ListItemViewHolder holder, int position) {
            holder.textView.setText(items[position]);
            holder.radioButton.setVisibility(View.VISIBLE);
            holder.radioButton.setChecked(position == selectedPosition);
            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                if (oldPos >= 0) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.length : 0;
        }
    }

    /**
     * 多选列表适配器
     */
    private class MultiChoiceAdapter extends RecyclerView.Adapter<ListItemViewHolder> {
        private final String[] items;
        private final boolean[] checked;

        MultiChoiceAdapter(String[] items, boolean[] checked) {
            this.items = items;
            this.checked = checked;
        }

        boolean[] getCheckedItems() {
            return checked;
        }

        @NonNull
        @Override
        public ListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.info_dialog_list_item, parent, false);
            return new ListItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ListItemViewHolder holder, int position) {
            holder.textView.setText(items[position]);
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(checked[position]);
            holder.itemView.setOnClickListener(v -> {
                checked[position] = !checked[position];
                holder.checkBox.setChecked(checked[position]);
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.length : 0;
        }
    }

    /**
     * 通用列表ViewHolder
     */
    private static class ListItemViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        RadioButton radioButton;
        CheckBox checkBox;

        ListItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.list_item_text);
            radioButton = itemView.findViewById(R.id.list_item_radio);
            checkBox = itemView.findViewById(R.id.list_item_check);
        }
    }

    // ========== 回调接口 ==========

    /**
     * 确认弹窗回调（MODE_CONFIRM）
     */
    public interface OnClickListener {
        void onClick(boolean agree);
    }

    /**
     * 列表项点击回调（MODE_LIST）
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * 单选弹窗回调（MODE_SINGLE_CHOICE）
     */
    public interface OnSingleChoiceListener {
        void onConfirm(int selectedPosition);
        void onCancel();
    }

    /**
     * 多选弹窗回调（MODE_MULTI_CHOICE）
     */
    public interface OnMultiChoiceListener {
        void onConfirm(boolean[] checkedItems);
        void onCancel();
    }
}
