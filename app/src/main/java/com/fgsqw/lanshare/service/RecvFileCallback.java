package com.fgsqw.lanshare.service;

import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public abstract class RecvFileCallback {
    Device device;
    List<MessageFileContent> fileContentList;
    Socket client;
    InputStream input;
    OutputStream out;
    boolean encData;

    public RecvFileCallback(Device device, List<MessageFileContent> fileContentList, Socket client, InputStream input, OutputStream out, boolean encData) {
        this.device = device;
        this.fileContentList = fileContentList;
        this.client = client;
        this.input = input;
        this.out = out;
        this.encData = encData;
    }

    public abstract void receviceFile(boolean isAgree);

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public List<MessageFileContent> getFileContentList() {
        return fileContentList;
    }

    public void setFileContentList(List<MessageFileContent> fileContentList) {
        this.fileContentList = fileContentList;
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
