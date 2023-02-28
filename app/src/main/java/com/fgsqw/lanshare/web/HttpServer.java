package com.fgsqw.lanshare.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * 轻量HttpServer服务
 */
public class HttpServer {

    private int port = 8080;
    private boolean running = false;
    private ServerSocket serverSocket;
    private final Map<String, HttpHandler> handlerMap = new HashMap<>();

    public HttpServer(int port) {
        this.port = port;
    }

    public HttpServer() {
    }

    public void addPath(String path, HttpHandler handler) {
        handlerMap.put(path, handler);
    }

    /**
     * 新的webHttp连接
     */
    public void newClient(Socket socket, byte[] magicBytes, InputStream inputStream, OutputStream outputStream) {
        try {
            Request request = new Request(inputStream);
            String line = null;
            int lineNum = 0;
            while ((line = IOUtil.readHttpLine(inputStream)) != null) {
                if (StringUtils.isEmpty(line)) {
                    break;
                }
                if (lineNum == 0 && magicBytes != null) {
                    String http = new String(magicBytes);
                    line = http + line;
                }
                int index = line.indexOf(" ");
                // 请求头数据
                String key = line.substring(0, index);
                String value = line.substring(index + 1);
                // 首行HTTP版本
                if (lineNum == 0) {
                    String[] s = value.split(" ");
                    String url = s[0];
                    url = URLDecoder.decode(url, "UTF-8");
                    int i = url.indexOf("?");
                    if (i > 0) {
                        String requestURL = url.substring(0, i);
                        request.setRequestURL(requestURL);
                        request.setRequestURLParams(url.substring(i + 1));
                        if (!StringUtils.isEmpty(request.getRequestURLParams())) {
                            if (!StringUtils.isEmpty(request.getRequestURLParams())) {
                                String[] split = request.getRequestURLParams().split("&");
                                for (String p : split) {
                                    if (p.contains("=")) {
                                        String[] split1 = p.split("=");
                                        request.addPathParams(split1[0], split1[1]);
                                    }
                                }
                            }
                        }
                    } else {
                        request.setRequestURL(url);
                    }
                    request.setRequestMethod(key);
                    request.addHeader(key, value);
                } else {
                    int i = key.indexOf(":");
                    if (i > 0) {
                        key = key.substring(0, i);
                    }
                    request.addHeader(key, value);
                }
                lineNum++;
            }
            request.setHeaderReady(true);
            Set<String> strings = handlerMap.keySet();
            boolean isMatch = false;
            for (String path : strings) {
                isMatch = request.getRequestURL().startsWith(path);
                if (isMatch) {
                    HttpHandler httpHandler = handlerMap.get(path);
                    if (httpHandler != null) {
                        httpHandler.handle(request, new Response(outputStream));
                    }
                    break;
                }
            }
            if (!isMatch) {
                Response response = new Response(outputStream);
                response.write404();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws IOException {
        running = true;
        serverSocket = new ServerSocket(port);
        ViewUpdate.runThread(() -> {
            try {
                while (running) {
                    Socket socket = serverSocket.accept();
                    newClient(socket, null, socket.getInputStream(), socket.getOutputStream());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        if (running) {
            running = false;
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
