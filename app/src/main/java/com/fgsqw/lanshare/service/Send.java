package com.fgsqw.lanshare.service;

import com.fgsqw.lanshare.utils.ByteUtil;
import com.fgsqw.lanshare.utils.StringLockManager;
import com.fgsqw.utils.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

public class Send {
    static boolean flag = false;
    static int port = 5586;
    static String ip = "192.168.0.224";

    public static void test() throws IOException, InterruptedException {

        Socket socket = new Socket(ip, port);
        InputStream inputStream = socket.getInputStream();
        byte[] buffer = new byte[1024];
        inputStream.read(buffer, 0, 4);
        int port = ByteUtil.bytesToInt(buffer, 0);

        inputStream.read(buffer, 0, 8);
        long fileSize = ByteUtil.bytesToLong(buffer, 0);
        Socket ssocket = null;
        if (flag) {
            ssocket = new Socket(ip, port);
        }
        int buffSize = 1024 * 1024 * 2;

        long startTime = System.currentTimeMillis();
        new Thread(() -> {
            Lock stringLock = StringLockManager.getStringLock("1");
            stringLock.lock();
            RandomAccessFile fos = null;
            try {
                byte[] b = new byte[buffSize];
                fos = new RandomAccessFile("received_1.zip", "rw");
                int ten = 0;
                while ((ten = inputStream.read(b, 0, buffSize)) != -1) {
                    fos.write(b, 0, ten);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            IOUtil.closeIO(inputStream, fos, socket);
            stringLock.unlock();
        }).start();

        if (flag) {
            Socket finalSsocket = ssocket;
            new Thread(() -> {
                Lock stringLock = StringLockManager.getStringLock("2");
                stringLock.lock();
                RandomAccessFile fos = null;
                InputStream inputStream1 = null;
                try {
                    inputStream1 = finalSsocket.getInputStream();
                    byte[] b = new byte[buffSize];
                    fos = new RandomAccessFile("received_1.zip", "rw");
                    fos.seek(fileSize / 2);
                    int ten = 0;
                    while ((ten = inputStream1.read(b, 0, buffSize)) != -1) {
                        fos.write(b, 0, ten);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                IOUtil.closeIO(inputStream1, fos, finalSsocket);
                stringLock.unlock();
            }).start();
        }

        Thread.sleep(1000);

        Lock stringLock = StringLockManager.getStringLock("1");
        stringLock.lock();
        stringLock.unlock();
        stringLock = StringLockManager.getStringLock("2");
        stringLock.lock();
        stringLock.unlock();
        long endTime = System.currentTimeMillis();
        System.out.println("完成: " + ((endTime - startTime - 1000) / 1000F) + "ms");
    }
}
