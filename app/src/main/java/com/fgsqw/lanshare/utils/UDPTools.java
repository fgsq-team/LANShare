package com.fgsqw.lanshare.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPTools {

    public static synchronized void sendData(DatagramSocket ds, DataEnc dataEnc, String IP, int port) {
        sendData(ds, dataEnc.getData(), dataEnc.getDataLen(), IP, port);
    }

    public static synchronized void sendData(DatagramSocket ds, byte[] buf, int length, String IP, int port) {
        try {
            ds.send(new DatagramPacket(buf, length, InetAddress.getByName(IP), port));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ds.close();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
