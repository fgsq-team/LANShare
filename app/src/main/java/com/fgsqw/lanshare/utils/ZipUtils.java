package com.fgsqw.lanshare.utils;

import com.fgsqw.lanshare.toast.T;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void compressFiles(File file, String fileName, ZipOutputStream zipOutputSteam) {
        try {
            if (zipOutputSteam == null)
                return;
            ZipEntry zipEntry = new ZipEntry(fileName);
            FileInputStream inputStream = new FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[4096];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputSteam.write(buffer, 0, len);
            }
            zipOutputSteam.closeEntry();
        } catch (Exception e) {
            T.s("压缩文件失败");
        }
    }

}
