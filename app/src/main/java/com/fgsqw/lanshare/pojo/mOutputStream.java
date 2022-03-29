package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.utils.DataEnc;

import java.io.IOException;
import java.io.OutputStream;

public class mOutputStream extends OutputStream {
    private boolean isClose;
    private final OutputStream os;

    // 逻辑取消
    public void setClose(boolean close) {
        isClose = close;
    }

    public mOutputStream(OutputStream os) {
        this.os = os;
        isClose = false;
    }

    public void write(DataEnc dataEnc) throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        os.write(dataEnc.getData(), 0, dataEnc.getDataLen());
    }


    @Override
    public void write(int b) throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        os.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        os.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        os.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}
