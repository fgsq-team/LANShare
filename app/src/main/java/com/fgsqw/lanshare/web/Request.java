package com.fgsqw.lanshare.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * 请求信息
 * @Author: fgsqme
 */
public class Request {

    public static final String CONTENT_LENGTH = "Content-Length";

    // 请求体长度
    private long contentLength = 0;

    private final InputStream is;

    // 请求头解析
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    // 请求路径
    private String requestURL;

    // 请求路径参数
    private String requestURLParams;

    // 请求路径参数解析
    private final Map<String, String> pathParams = new HashMap<>();

    // 请求协议 POST/GET
    private String requestMethod;

    private boolean headerReady = false;

    public Request(InputStream is) {
        this.is = is;
    }


    /**
     * 获取路径参数
     */
    public Map<String, String> getPathParams() {
        return pathParams;
    }


    public String getPathParam(String key) {
        return pathParams.get(key);
    }


    public String getRequestBody() throws IOException {
        if (!headerReady) {
            return null;
        }
        if (requestMethod.equalsIgnoreCase("GET")) {
            return requestURLParams;
        } else {
            if (contentLength == 0) {
                return null;
            }
            byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();
            int size = (int) contentLength;
            while (size > 0) {
                int read = is.read(buffer, 0, Math.min(size, buffer.length));
                if (read == -1) {
                    return null;
                }
                sb.append(new String(buffer, 0, read));
                size -= read;
            }
            return sb.toString();
        }
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
        if (CONTENT_LENGTH.equalsIgnoreCase(key)) {
            contentLength = Long.parseLong(value.replaceAll("[\\s+\\t\\n\\r]", ""));
        }
    }


    public boolean isHeaderReady() {
        return headerReady;
    }

    public void setHeaderReady(boolean headerReady) {
        this.headerReady = headerReady;
    }

    public String getHeaderValue(String key) {
        return headers.get(key);
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod.toUpperCase();
    }


    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public String getRequestURLParams() {
        return requestURLParams;
    }

    public void setRequestURLParams(String requestURLParams) {
        this.requestURLParams = requestURLParams;
    }

    public String getRequestMethod() {
        return requestMethod;
    }


    public void addPathParams(String key, String value) {
        pathParams.put(key, value);
    }

}
