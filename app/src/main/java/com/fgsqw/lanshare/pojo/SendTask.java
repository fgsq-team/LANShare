package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.pojo.message.MessageFileContent;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class SendTask {
    Socket socket;
    InputStream input;
    OutputStream out;
    Device device;
    List<MessageFileContent> messageFileContents;

    boolean encData;


    public SendTask() {
    }

    public SendTask(Socket socket, InputStream input, OutputStream out, Device device, List<MessageFileContent> messageFileContents, boolean encData) {
        this.socket = socket;
        this.input = input;
        this.out = out;
        this.device = device;
        this.messageFileContents = messageFileContents;
        this.encData = encData;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public List<MessageFileContent> getMessageFileContents() {
        return messageFileContents;
    }

    public void setMessageFileContents(List<MessageFileContent> messageFileContents) {
        this.messageFileContents = messageFileContents;
    }

    public boolean isEncData() {
        return encData;
    }

    public void setEncData(boolean encData) {
        this.encData = encData;
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

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }


}
