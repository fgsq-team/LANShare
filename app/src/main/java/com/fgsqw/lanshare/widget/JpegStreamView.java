

package com.fgsqw.lanshare.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.fgsqw.lanshare.utils.DataDec;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.utils.TypeLength;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class JpegStreamView extends AppCompatImageView {

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
                Socket socket = new Socket("240e:438:1a26:7c2:d8a1:e1ff:feb5:9fe0", 8880);
                InputStream input = socket.getInputStream();
                while (run) {
                    dataDec.reset();
                    if (IOUtil.read(input, dataDec.getData(), 0, DataDec.getHeaderSize()) != DataDec.getHeaderSize())
                        return;
                    int length = dataDec.getLength();
                    if (IOUtil.read(input, dataDec.getData(), DataDec.getHeaderSize(), length) != length)
                        return;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            dataDec.getData(),
                            TypeLength.INT_LEN + DataDec.getHeaderSize(),  // 偏移长度
                            length
                    );
                    post(() -> setImageBitmap(bitmap));
                }
            } catch (IOException e) {
                LLog.error("error", e);
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

