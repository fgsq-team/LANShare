package com.fgsqw.lanshare.web;


import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;

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
 *
 * @Author: fgsqme
 */
public class HttpServer {

    private int port = 8080;
    private boolean running = false;
    private ServerSocket serverSocket;
    // 请求路径列表
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
     *
     * @param magicStr 已经读取的字符
     */
    public void newClient(Socket socket, String magicStr, InputStream inputStream, OutputStream outputStream) {
        try {
            Request request = new Request(inputStream);
            String line = null;
            int lineNum = 0;
            while ((line = IOUtil.readHttpLine(inputStream)) != null) {
                if (StringUtils.isEmpty(line)) {
                    break;
                }
                // 已经读取的字符需要再拼接回来
                if (lineNum == 0 && magicStr != null) {
                    line = magicStr + line;
                }
                int index = line.indexOf(" ");
                // 请求头数据
                String key = line.substring(0, index);
                String value = line.substring(index + 1);
                // 首行HTTP版本
                if (lineNum == 0) {
                    String[] s = value.split(" ");
                    String url = s[0];
                    // 中文字符转义兼容
                    url = URLDecoder.decode(url, "UTF-8");
                    int i = url.indexOf("?");
                    // 如果有路径请求参数
                    if (i > 0) {
                        String requestURL = url.substring(0, i);
                        // 请求路径
                        request.setRequestURL(requestURL);
                        // 路径请求参数截取
                        request.setRequestURLParams(url.substring(i + 1));
                        if (!StringUtils.isEmpty(request.getRequestURLParams())) {
                            // 路径请求参数解析成Map
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
                        // 没有请求参数
                        // 请求路径
                        request.setRequestURL(url);
                    }
                    // 请求协议 POST/GET
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
            // 请求头解析完毕
            request.setHeaderReady(true);
            Set<String> strings = handlerMap.keySet();
            boolean isMatch = false;
            Response response = new Response(outputStream);
            // 请求路径匹配
            for (String path : strings) {
                isMatch = request.getRequestURL().startsWith(path);
                if (isMatch) {
                    HttpHandler httpHandler = handlerMap.get(path);
                    if (httpHandler != null) {
                        try {
                            httpHandler.handle(request, response);
                        } catch (Exception e) {
                            e.printStackTrace();
                            response.write404();
                        }
                    }
                    break;
                }
            }
            // 请求路径找不到直接返回404
            if (!isMatch) {
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

    /**
     * 启动服务
     *
     * @throws IOException
     */
    public void start() throws IOException {
        running = true;
        serverSocket = new ServerSocket(port);
        ThreadUtils.runThread(() -> {
            try {
                while (running) {
                    Socket socket = serverSocket.accept();
                    ThreadUtils.runThread(() -> {
                        try {
                            newClient(socket, null, socket.getInputStream(), socket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 停止服务
     */
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
