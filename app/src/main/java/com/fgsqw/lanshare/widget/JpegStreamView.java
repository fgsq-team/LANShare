/*
 * IPCamImageView - IPCamView.java
 * Created by Marcos Calvo García on 14/12/18 16:14.
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 14/12/18 16:14.
 */

package com.fgsqw.lanshare.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

import com.fgsqw.lanshare.utils.DataDec;
import com.fgsqw.lanshare.utils.DataEnc;
import com.fgsqw.lanshare.utils.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class JpegStreamView extends android.support.v7.widget.AppCompatImageView {

    private boolean run = false;
    private Thread streamThread;

    public JpegStreamView(Context context) {
        super(context);
    }

    public JpegStreamView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public JpegStreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void start() {
        if (run)
            return;
        run = true;
        streamThread = new Thread(() -> {
            try {
                DataDec dataDec = new DataDec(1024 * 1024 * 2);
                Socket socket = new Socket("192.168.31.228", 8080);
                InputStream input = socket.getInputStream();
                while (run) {
                    dataDec.reset();
                    if (IOUtil.read(input, dataDec.getData(), 0, DataDec.getHeaderSize()) != DataDec.getHeaderSize())
                        return;
                    int length = dataDec.getLength();
                    if (IOUtil.read(input, dataDec.getData(), DataDec.getHeaderSize(), length) != length)
                        return;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(dataDec.getData(), DataDec.getHeaderSize(), length);
                    post(() -> setImageBitmap(bitmap));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        streamThread.start();
    }


    public void stop() {
        if (run) {
            run = false;
            streamThread.interrupt();
        }
    }


}

