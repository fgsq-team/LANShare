package com.fgsqw.lanshare.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPTools {
    public static void sendData(DatagramSocket ds, byte[] buf, int length, String IP, int port) {
        try {
            ds.send(new DatagramPacket(buf, length, InetAddress.getByName(IP), port));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ds.close();
    }
}
