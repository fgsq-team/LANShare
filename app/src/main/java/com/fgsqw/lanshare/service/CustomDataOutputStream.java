package com.fgsqw.lanshare.service;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CustomDataOutputStream extends OutputStream {
    private final OutputStream out;

    private final byte[] buffer = new byte[8];

    public CustomDataOutputStream(OutputStream out) {
        this.out = out;
    }

    // 写入boolean值
    public void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    // 写入byte值
    public void writeByte(int v) throws IOException {
        out.write(v);
    }

    // 写入short值
    public void writeShort(int v) throws IOException {
        buffer[0] = (byte) (v >>> 8);
        buffer[1] = (byte) v;
        out.write(buffer, 0, 2);
    }

    // 写入char值
    public void writeChar(int v) throws IOException {
        buffer[0] = (byte) (v >>> 8);
        buffer[1] = (byte) v;
        out.write(buffer, 0, 2);
    }

    // 写入int值
    public void writeInt(int v) throws IOException {
        buffer[0] = (byte) (v >>> 24);
        buffer[1] = (byte) (v >>> 16);
        buffer[2] = (byte) (v >>> 8);
        buffer[3] = (byte) v;
        out.write(buffer, 0, 4);
    }

    // 写入long值
    public void writeLong(long v) throws IOException {
        buffer[0] = (byte) (v >>> 56);
        buffer[1] = (byte) (v >>> 48);
        buffer[2] = (byte) (v >>> 40);
        buffer[3] = (byte) (v >>> 32);
        buffer[4] = (byte) (v >>> 24);
        buffer[5] = (byte) (v >>> 16);
        buffer[6] = (byte) (v >>> 8);
        buffer[7] = (byte) v;
        out.write(buffer, 0, 8);
    }

    // 写入float值
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    // 写入double值
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    // 写入字符串
    public void writeString(String str) throws IOException {
        if (str == null) {
            writeInt(-1); // null字符串用-1表示
        } else {
            byte[] bytes = str.getBytes("UTF-8");
            writeInt(bytes.length); // 先写入长度
            out.write(bytes); // 再写入字节数据
        }
    }

    // 写入完整的byte数组
    public void writeBytes(byte[] b) throws IOException {
        out.write(b, 0, b.length);
    }

    // 写入byte数组的一部分
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    // 刷新缓冲区
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
