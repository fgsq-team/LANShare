package com.fgsqw.lanshare.utils;

import com.fgsqw.httpserver.utils.ByteUtil;
import com.fgsqw.lanshare.config.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class UDPTools {

    /**
     * @author fgsq
     * @comments 装包数据通过UDP发送
     * @date 2024/4/28 17:59
     */
    public static synchronized void sendData(DatagramSocket ds, DataEnc dataEnc, String ip, int port) throws IOException {
        byte[] data = dataEnc.encData();
        int dataLen = dataEnc.getDataLen();
        byte[] buff = new byte[data.length + 4];
        ByteUtil.intToBytes(Config.MAGIC_NUM, buff, 0);
        System.arraycopy(data, 0, buff, 4, dataLen);
        ds.send(new DatagramPacket(buff, dataLen + 4, InetAddress.getByName(ip), port));
        ds.close();
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }


}
