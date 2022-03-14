package com.fgsqw.lanshare.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.NetWorkUtil;
import com.fgsqw.lanshare.utils.ViewUpdate;
import com.fgsqw.lanshare.utils.mUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpShareActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    Switch start;
    TextView info;
    ServerSocket serverSocket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_share);
        start = findViewById(R.id.http_share_start);
        info = findViewById(R.id.http_share_info);
        start.setOnCheckedChangeListener(this);
        info.setOnClickListener(this);
    }

    public void startHttpServer() {
        ViewUpdate.runThread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                Socket socket = null;
                InputStream input;
                OutputStream output;
                try {
                    socket = serverSocket.accept();
                    input = socket.getInputStream();
                    output = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (socket == null) {
                    return;
                }
                try {
                    BufferedReader bd = new BufferedReader(new InputStreamReader(input));
                    String requestHeader;
                    boolean pathvif = false;
                    while ((requestHeader = bd.readLine()) != null && !requestHeader.isEmpty()) {
                        System.out.println(requestHeader);
                        if (requestHeader.startsWith("GET")) {
                            int begin = requestHeader.indexOf("/");
                            int end = requestHeader.indexOf("HTTP/") - 1;
                            String condition = requestHeader.substring(begin, end);
                            pathvif = condition.equals("/LANShare.apk");
                        }
                    }

                    File selfapk = new File(mUtil.getSelfApkPath(this));
                    InputStream selfapkIO = new FileInputStream(selfapk);

                    StringBuilder header = new StringBuilder();
                    if (pathvif) {
                        header.append("HTTP/1.1 200 OK\r\n");
                        header.append("Content-type: application/octet-stream\r\n");
                        header.append("Content-Length: ").append(selfapk.length()).append("\r\n");
                        header.append("Connection: keep-alive\r\n\r\n");
                        // 写出响应头
                        output.write(header.toString().getBytes());
                        // 写出响应体
                        IOUtil.writeAlData(selfapkIO, output);
                        output.flush();
                    } else {
                        byte[] body = "404 Not Found".getBytes();
                        header.append("HTTP/1.1 404 Not Found\r\n");
                        header.append("Content-type: text/html\r\n");
                        header.append("Content-Length: ").append(body.length).append("\r\n");
                        header.append("Connection: keep-alive\r\n\r\n");
                        // 写出响应头
                        output.write(header.toString().getBytes());
                        // 写出响应体
                        output.write(body);
                        output.flush();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtil.closeIO(input, output, socket);
                }
            }

        });
    }

    public void stopHttpServer() {
        IOUtil.closeIO(serverSocket);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopHttpServer();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            String locAddress = NetWorkUtil.getLocAddress(this);
            info.setText("地址:http://" + locAddress + ":8080/LANShare.apk");
            startHttpServer();
        } else {
            info.setText(getString(R.string.http_share_info));
            stopHttpServer();
        }
    }

    @Override
    public void onClick(View v) {
        if (start.isChecked()) {
            String locAddress = NetWorkUtil.getLocAddress(this);
            mUtil.copy("http://" + locAddress + ":8080/LANShare.apk", this);
            T.s("已复制");
        } else {
            T.s("请先开启按钮");
        }
    }
}
