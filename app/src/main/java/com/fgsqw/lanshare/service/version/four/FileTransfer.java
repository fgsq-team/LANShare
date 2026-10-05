package com.fgsqw.lanshare.service.version.four;



import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;

import java.util.List;

/**
 * 文件传输对象
 * <p>封装文件传输的元数据信息,包括发送设备、文件列表等</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class FileTransfer {
    /** 发送设备信息 */
    private Device fromDevice;
    
    /** 传输类型 */
    private int type;
    
    /** 组ID,用于标识同一批次的传输 */
    private String groupId;
    
    /** 待传输的文件列表 */
    private List<MessageFileContent> files;

    /**
     * 获取发送设备信息
     *
     * @return 发送设备
     */
    public Device getFromDevice() {
        return fromDevice;
    }

    /**
     * 设置发送设备信息
     *
     * @param fromDevice 发送设备
     */
    public void setFromDevice(Device fromDevice) {
        this.fromDevice = fromDevice;
    }

    /**
     * 获取传输类型
     *
     * @return 传输类型
     */
    public int getType() {
        return type;
    }

    /**
     * 设置传输类型
     *
     * @param type 传输类型
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * 获取组ID
     *
     * @return 组ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * 设置组ID
     *
     * @param groupId 组ID
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * 获取文件列表
     *
     * @return 文件列表
     */
    public List<MessageFileContent> getFiles() {
        return files;
    }

    /**
     * 设置文件列表
     *
     * @param files 文件列表
     */
    public void setFiles(List<MessageFileContent> files) {
        this.files = files;
    }
}
