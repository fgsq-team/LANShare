package com.fgsqw.lanshare.listener;


import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class LFileObserver {

    private static final String TAG = "LFileObserver";

    private String rootPath;
    private int fileObserverMask;
    private FileObserver rootObserver;
    private List<FileObserver> subObservers;
    private int ISDIR = 0x40000000;

    public LFileObserver(String path, int mask) {
        rootPath = path;
        fileObserverMask = mask;
        subObservers = new ArrayList<>();
        rootObserver = createFileObserver(rootPath);
    }

    public void startWatching() {
        if (rootObserver != null) {
            rootObserver.startWatching();
            for (FileObserver subObserver : subObservers) {
                subObserver.startWatching();
            }
        }
    }

    public void stopWatching() {
        if (rootObserver != null) {
            rootObserver.stopWatching();
            for (FileObserver subObserver : subObservers) {
                subObserver.stopWatching();
            }
        }
    }

    private FileObserver createFileObserver(String apath) {
        FileObserver observer = new FileObserver(apath, fileObserverMask) {
            @Override
            public void onEvent(int event, String path) {
                if ((event & ISDIR) != 0) {
                    // 处理文件夹或文件变化事件
                    switch (event & 0xFFFF) {
                        case FileObserver.CREATE:
                            Log.d(TAG, "File created folder: " + apath + "/" + path);
                            FileObserver fileObserver = createFileObserver(apath + "/" + path);
                            fileObserver.startWatching();
                            // 处理创建事件
                            break;
                        case FileObserver.DELETE:
                            Log.d(TAG, "File deleted: " + path);
                            // 处理删除事件
                            break;
//                        case FileObserver.MODIFY:
//                            Log.d(TAG, "File modified: " + path);
//                            // 处理修改事件
//                            break;
                        // 其他事件类型可以根据需要进行处理
                    }
                } else {
                    // 处理文件夹或文件变化事件
                    switch (event) {
                        /*case FileObserver.CREATE:
                            Log.d(TAG, "File created: " + apath + "/" + path);
                            // 处理创建事件
                            break;*/
                        case FileObserver.DELETE:
                            Log.d(TAG, "File deleted: " + path);
                            // 处理删除事件
                            break;
//                        case FileObserver.MODIFY:
//                            Log.d(TAG, "File modified: " + path);
//                            // 处理修改事件
//                            break;
                        case FileObserver.CLOSE_WRITE:
                            Log.d(TAG, "File close write: " + path);
                            // 处理修改事件
                            break;
                        // 其他事件类型可以根据需要进行处理
                    }
                }
            }
        };

        // 获取当前目录下的所有子目录
        File[] subdirectories = new File(apath).listFiles(File::isDirectory);

        // 递归为每个子目录创建观察者
        if (subdirectories != null) {
            for (File subdirectory : subdirectories) {
                String subdirectoryPath = subdirectory.getAbsolutePath();
                FileObserver subdirectoryObserver = createFileObserver(subdirectoryPath);
                subObservers.add(subdirectoryObserver);
            }
        }

        return observer;
    }


/*    public static ArrayList<String> scanFolder(File directory) {
        ArrayList<String> md5List = new ArrayList<>();
        scanDirectory(directory, md5List);
        return md5List;
    }

    private static void scanDirectory(File directory, ArrayList<String> md5List) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            String md5 = calculateMD5(file);
                            md5List.add(md5);
                        } catch (IOException | NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    } else if (file.isDirectory()) {
                        scanDirectory(file, md5List);
                    }
                }
            }
        }
    }

    private static String calculateMD5(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = fis.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        byte[] md5sum = digest.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : md5sum) {
            builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }*/
}
