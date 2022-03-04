package com.fgsqw.lanshare.pojo;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class mSocket implements Closeable {
    Socket socket;
    mInputStream inputStream;
    mOutputStream outputStream;

    InputStream is;
    OutputStream out;

    public mSocket(InputStream is, OutputStream out) {
        this.is = is;
        this.out = out;
        inputStream = new mInputStream(is);
        outputStream = new mOutputStream(out);
    }

    public mSocket(Socket socket) throws IOException {
        this.socket = socket;
        inputStream = new mInputStream(socket.getInputStream());
        outputStream = new mOutputStream(socket.getOutputStream());
    }

    public mInputStream getInputStream() {
        return inputStream;
    }

    public mOutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getIs() {
        return is;
    }

    public OutputStream getOut() {
        return out;
    }

    public void mClose() {
        inputStream.setClose(true);
        outputStream.setClose(true);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
