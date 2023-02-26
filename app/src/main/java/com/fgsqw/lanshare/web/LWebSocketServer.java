package com.fgsqw.lanshare.web;


import com.fgsqw.lanshare.utils.LLog;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * webSocket服务
 * 主要用于web端聊天服务
 */
public class LWebSocketServer extends WebSocketServer {

    public LWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    public LWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("连接成功");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
//        if (conn != null) {
//        }
    }

    @Override
    public void onStart() {
        LLog.info("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    public static void main(String[] args) throws UnknownHostException {
        LWebSocketServer socketServer = new LWebSocketServer(6666);
        // 启动自己会开启一个线程
        socketServer.start();
    }

}

