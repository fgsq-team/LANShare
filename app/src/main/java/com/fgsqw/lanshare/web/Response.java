package com.fgsqw.lanshare.web;



import com.fgsqw.lanshare.utils.ContentTypes;
import com.fgsqw.lanshare.utils.IOUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 响应信息
 * @Author: fgsqme
 */
public class Response {


    private OutputStream out;

    // 响应头
    private Map<String, String> headers = new HashMap<>();

    public static final String HTTP_VERSION = "HTTP/1.1";
    public static final String TEXT_CONTEXT_TYPE = "text/html; charset-utf-8";
    public static final String STREAM_CONTEXT_TYPE = "application/octet-stream";
    public static final String STREAM_CONTEXT_IMAGE = "image/png";
    public static final String STREAM_CONTEXT_JSON = "application/json";

    private long contentLength = 0;

    public Response(OutputStream out) {
        this.out = out;
        headers.put(HTTP_VERSION, "200 OK");
        headers.put("Content-Length", contentLength + "");
        headers.put("Content-Type", TEXT_CONTEXT_TYPE);
    }

    public void setStatus(String status) {
        headers.put(HTTP_VERSION, status);
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
        headers.put("Content-Length", contentLength + "");
    }

    private StringBuilder createHeader() {
        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<String, String>> entries = headers.entrySet();
        int lineNum = 0;
        for (Map.Entry<String, String> entry : entries) {
            sb.append(entry.getKey());
            if (lineNum == 0) {
                sb.append(" ");
            } else {
                sb.append(": ");
            }
            sb.append(entry.getValue());
            sb.append("\r\n");
            lineNum++;
        }
        sb.append("\r\n");
        return sb;
    }

    public void write404() throws IOException {
        String responseBody = "404 Not Found";
        setStatus(responseBody);
        setContentLength(responseBody.getBytes().length);
        setContentType(TEXT_CONTEXT_TYPE);
        StringBuilder sb = createHeader();
        sb.append(responseBody);
        out.write(sb.toString().getBytes());
        out.flush();
    }


    public void writeString(String responseBody) throws IOException {
        writeString(responseBody, STREAM_CONTEXT_JSON);
    }

    public void writeString(String responseBody, String contentType) throws IOException {
        setContentLength(responseBody.getBytes().length);
        setContentType(contentType);
        StringBuilder sb = createHeader();
        sb.append(responseBody);
        out.write(sb.toString().getBytes());
        out.flush();
    }

    public void writeBytes(byte[] bytes) throws IOException {
        writeBytes(bytes, STREAM_CONTEXT_TYPE);
    }

    /**
     * 返回字节数组
     * @param bytes 字节数组
     * @param contentType 响应体contentType
     */
    public void writeBytes(byte[] bytes, String contentType) throws IOException {
        setContentLength(bytes.length);
        setContentType(contentType);
        StringBuilder sb = createHeader();
        out.write(sb.toString().getBytes());
        out.write(bytes);
        out.flush();
    }

    /**
     * 返回文件，根据文件名判断响应体的contentType类型
     * @param file 文件
     */
    public void writeFile(File file) throws IOException {
        setContentLength(file.length());
        setContentType(STREAM_CONTEXT_TYPE);
        String name = file.getName();
        int i = name.lastIndexOf(".");
        if (i > 0) {
            String suffix = name.substring(i + 1);
            String contentType = ContentTypes.contentTypeMap.get(suffix);
            if (contentType != null) {
                setContentType(contentType);
            }
        }
        StringBuilder sb = createHeader();
        out.write(sb.toString().getBytes());
        InputStream is = new FileInputStream(file);
        IOUtil.transfer(is, out);
        out.flush();
        is.close();
    }


    public void writeStream(InputStream is, long length) throws IOException {
        writeStream(is, STREAM_CONTEXT_TYPE, length);
    }

    /**
     * 返回数据流
     * @param is 数据流
     * @param contentType 响应体contentType类型
     * @param length 返回数据长度
     */
    public void writeStream(InputStream is, String contentType, long length) throws IOException {
        setContentLength(length);
        setContentType(contentType);
        StringBuilder sb = createHeader();
        out.write(sb.toString().getBytes());
        IOUtil.transfer(is, out);
        out.flush();
    }

}
