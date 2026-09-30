package com.fgsqw.lanshare.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseDialog;
import com.fgsqw.lanshare.utils.FileUtil;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileInfoDialog extends BaseDialog {

    private String filePath;

    public FileInfoDialog(Context context, String filePath) {
        super(context, R.style.AlertDialogTheme);
        this.filePath = filePath;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_file_info);
        TextView textViewTitle = findViewById(R.id.info_tips);
        TextView textViewFileNameLabel = findViewById(R.id.textViewFileNameLabel);
        TextView textViewFileName = findViewById(R.id.textViewFileName);
        TextView textViewFilePathLabel = findViewById(R.id.textViewFilePathLabel);
        TextView textViewFilePath = findViewById(R.id.textViewFilePath);
        TextView textViewFileSizeLabel = findViewById(R.id.textViewFileSizeLabel);
        TextView textViewFileSize = findViewById(R.id.textViewFileSize);
        TextView textViewLastModifiedLabel = findViewById(R.id.textViewLastModifiedLabel);
        TextView textViewLastModified = findViewById(R.id.textViewLastModified);
        LinearLayout infoLayoutOk = findViewById(R.id.info_layout_ok);
        infoLayoutOk.setOnClickListener(v -> dismiss());

        File file = new File(filePath);
        String fileName = file.getName();
        long fileSize = file.length();
        long lastModified = file.lastModified();

        textViewTitle.setText("文件信息");
        textViewFileNameLabel.setText("文件名：");
        textViewFileName.setText(fileName);
        textViewFilePathLabel.setText("路径：");
        textViewFilePath.setText(filePath);
        textViewFileSizeLabel.setText("大小：");
        textViewFileSize.setText(FileUtil.computeSize(fileSize));
        textViewLastModifiedLabel.setText("修改时间：");
        textViewLastModified.setText(formatDate(lastModified));
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date(timestamp));
    }
}
