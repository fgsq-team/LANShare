package com.fgsqw.lanshare.service;

import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.service.version.four.FileTransfer;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public abstract class RecvFileCallback {
    FileTransfer fileTransfer;
    Socket client;
    InputStream input;
    OutputStream out;
    boolean encData;

    public RecvFileCallback(FileTransfer fileTransfer, List<MessageFileContent> fileContentList, Socket client, InputStream input, OutputStream out, boolean encData) {
        this.fileTransfer = fileTransfer;
        this.client = client;
        this.input = input;
        this.out = out;
        this.encData = encData;
    }

    public abstract void receviceFile(boolean isAgree);

    public FileTransfer getFileTransfer() {
        return fileTransfer;
    }

    public void setFileTransfer(FileTransfer fileTransfer) {
        this.fileTransfer = fileTransfer;
    }

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public OutputStream getOut() {
        return out;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public boolean isEncData() {
        return encData;
    }

    public void setEncData(boolean encData) {
        this.encData = encData;
    }
}
