package com.fgsqw.lanshare.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.fgsqw.lanshare.toast.T;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CopFileTask extends AsyncTask<Integer, Integer, String> {

    private ProgressDialog progressDialog;
    String srcPath, outPath;
    boolean isBackSucess = false;

    public CopFileTask(Context context, String srcPath, String outPath) {

        this.srcPath = srcPath;
        this.outPath = outPath;

        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        progressDialog.setButton("隐藏", (p1, p2) -> {
            progressDialog.cancel();
        });
        progressDialog.setTitle("正在备份");
    }

    @Override
    protected void onPreExecute() {
        progressDialog.show();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Integer[] p1) {

        try {

            File srcFile = new File(srcPath);
            File outFile = new File(outPath);

            File parentFile = outFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (outFile.exists()) {
                for (int s = 1; s < 65535; s++) {
                    String str;
                    if (outFile.getName().contains(".")) {
                        String prefix = outFile.getName().substring(0, outFile.getName().lastIndexOf(".")) + "(" + s + ")";
                        String suffix = outFile.getName().substring(outFile.getName().lastIndexOf("."));
                        str = prefix + suffix;
                    } else {
                        str = outFile.getName() + "(" + s + ")";
                    }
                    outFile = new File(parentFile, str);
                    if (!outFile.exists()) {
                        break;
                    }
                }
            }

            InputStream input = new FileInputStream(srcPath);
            OutputStream out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024 * 1024];

            long fileSize = srcFile.length();
            long copyTotal = 0;
            int ten = 0;

            int tempPro = 0;
            while ((ten = input.read(buffer)) != -1) {
                copyTotal += ten;
                System.out.println(copyTotal);
                int progress = (int) ((copyTotal * 100) / fileSize);
                if (progress != tempPro) {
                    publishProgress(progress);
                    tempPro = progress;
                }
                out.write(buffer, 0, ten);
            }

            isBackSucess = true;
            IOUtil.closeIO(out, input);
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
            isBackSucess = false;
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressDialog.setProgress(values[0]);
        super.onProgressUpdate(values);
    }


    @Override
    protected void onPostExecute(String result) {
        progressDialog.cancel();
        if (isBackSucess) {
            T.s("备份成功");
        } else {
            T.s("备份失败");
        }
        super.onPostExecute(result);
    }

}
