package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.service.version.four.FileTransfer;

import java.net.Socket;

public class SendTask {
    Socket socket;
    FileTransfer fileTransfer;
    boolean encData;

    public SendTask() {
    }

    public SendTask(Socket socket, FileTransfer fileTransfer, boolean encData) {
        this.socket = socket;
        this.fileTransfer = fileTransfer;
        this.encData = encData;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isEncData() {
        return encData;
    }

    public void setEncData(boolean encData) {
        this.encData = encData;
    }

    public FileTransfer getFileTransfer() {
        return fileTransfer;
    }
}
