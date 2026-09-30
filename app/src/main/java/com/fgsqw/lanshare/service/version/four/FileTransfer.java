package com.fgsqw.lanshare.service.version.four;



import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;

import java.util.List;

public class FileTransfer {
    private Device fromDevice;
    private int type;
    private String groupId;
    private List<MessageFileContent> files;

    public Device getFromDevice() {
        return fromDevice;
    }

    public void setFromDevice(Device fromDevice) {
        this.fromDevice = fromDevice;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<MessageFileContent> getFiles() {
        return files;
    }

    public void setFiles(List<MessageFileContent> files) {
        this.files = files;
    }
}
