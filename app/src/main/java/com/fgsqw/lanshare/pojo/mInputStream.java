package com.fgsqw.lanshare.pojo;

import java.io.IOException;
import java.io.InputStream;

public class mInputStream extends InputStream {
    private boolean isClose;
    private final InputStream is;

    public mInputStream(InputStream is) {
        this.is = is;
        isClose = false;
    }

    // 逻辑取消
    public void setClose(boolean close) {
        isClose = close;
    }

    @Override
    public int read() throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        return is.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (isClose) throw new IOException("mOutputStream is close");
        return is.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
    /* @Override
    public synchronized void reset() throws IOException {
        is.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }*/


}
