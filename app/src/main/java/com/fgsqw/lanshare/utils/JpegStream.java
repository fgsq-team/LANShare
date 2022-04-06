/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fgsqw.lanshare.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class JpegStream {

    private final int port;
    private boolean newJpeg = false;
    private long timestamp = 0;
    private final Object bufferLock = new Object();
    private Thread worker = null;
    private volatile boolean running = false;

    // 数据缓存
    DataEnc dataEnc;

    JpegStream(final int port, final int bufferSize) {
        this.port = port;
        dataEnc = new DataEnc(bufferSize);
    }

    void start() {
        if (running) {
            throw new IllegalStateException("JpegStream is already running");
        }
        running = true;
        worker = new Thread(this::workerRun);
        worker.start();
    }

    void stop() {
        if (!running) {
            throw new IllegalStateException("JpegStream is already stopped");
        }
        running = false;
        worker.interrupt();
    }

    void streamJpeg(final byte[] jpeg, final int length, final long timestamp) {
        synchronized (bufferLock) {
            this.timestamp = timestamp;
            dataEnc.reset();
            dataEnc.putBytes(jpeg, length);
            newJpeg = true;
            bufferLock.notify();
        }
    }

    private void workerRun() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                Socket client = serverSocket.accept();
                if (client == null)
                    break;
                acceptAndStream(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }


    }

    // 完全以流的形式输出图片
    private void acceptAndStream(Socket client) {
        OutputStream output;
        try {
            output = client.getOutputStream();
            while (running) {
                // 阻塞等待Buff更新
                synchronized (bufferLock) {
                    while (!newJpeg) {
                        try {
                            bufferLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    newJpeg = false;
                }
                IOUtil.write(output, dataEnc.getData(), dataEnc.getDataLen());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // HTTP jpeg 暂时不用
    private static final String BOUNDARY = "--gc0p4Jq0M2Yt08jU534c0p--";
    private static final String BOUNDARY_LINES = "\r\n" + BOUNDARY + "\r\n";
    private static final String HTTP_HEADER =
            "HTTP/1.0 200 OK\r\n"
                    + "Server: Peepers\r\n"
                    + "Connection: close\r\n"
                    + "Max-Age: 0\r\n"
                    + "Expires: 0\r\n"
                    + "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, "
                    + "post-check=0, max-age=0\r\n"
                    + "Pragma: no-cache\r\n"
                    + "Content-Type: multipart/x-mixed-replace; "
                    + "boundary=" + BOUNDARY + "\r\n"
                    + BOUNDARY_LINES;

    // 使用Http协议输出图片
    private void acceptAndJpegHttpStream(Socket client) {
        DataOutputStream stream = null;
        try {
            stream = new DataOutputStream(client.getOutputStream());
            stream.writeBytes(HTTP_HEADER);
            stream.flush();
            while (running) {
                // 阻塞等待Buff更新
                synchronized (bufferLock) {
                    while (!newJpeg) {
                        try {
                            bufferLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    newJpeg = false;
                }
                stream.writeBytes("Content-type: image/jpeg\r\n"
                        + "Content-Length: " + dataEnc.getLength() + "\r\n"
                        + "X-Timestamp:" + timestamp + "\r\n"
                        + "\r\n"
                );
                stream.write(dataEnc.getData(), DataEnc.getHeaderSize(), dataEnc.getLength());
                stream.writeBytes(BOUNDARY_LINES);
                stream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}