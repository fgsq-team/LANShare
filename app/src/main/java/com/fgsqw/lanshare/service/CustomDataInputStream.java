package com.fgsqw.lanshare.service;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CustomDataInputStream extends InputStream {
    private final InputStream in;
    private final byte[] buffer = new byte[8];

    public CustomDataInputStream(InputStream in) {
        this.in = in;
    }

    // 读取boolean值
    public boolean readBoolean() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch != 0;
    }

    // 读取byte值
    public byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    // 读取unsigned byte值
    public int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    // 读取short值
    public short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + (ch2));
    }

    // 读取unsigned short值
    public int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 << 8) + (ch2);
    }

    // 读取char值
    public char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char) ((ch1 << 8) + (ch2));
    }

    // 读取int值
    public int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    // 读取long值
    public long readLong() throws IOException {
        readFully(buffer, 0, 8);
        return (((long) buffer[0] << 56) +
                ((long) (buffer[1] & 255) << 48) +
                ((long) (buffer[2] & 255) << 40) +
                ((long) (buffer[3] & 255) << 32) +
                ((long) (buffer[4] & 255) << 24) +
                ((buffer[5] & 255) << 16) +
                ((buffer[6] & 255) << 8) +
                ((buffer[7] & 255)));
    }

    // 读取float值
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    // 读取double值
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    // 读取字符串
    public String readString() throws IOException {
        int length = readInt(); // 先读取长度
        if (length == -1) {
            return null; // -1表示null字符串
        }
        if (length == 0) {
            return ""; // 0表示空字符串
        }
        byte[] bytes = new byte[length];
        readFully(bytes); // 读取指定长度的字节数据
        return new String(bytes, "UTF-8");
    }

    // 读取一个对象
    public <T> T readObject(Class<T> clazz) throws IOException {
        String json = readString();
        return JSON.toJavaObject(JSON.parseObject(json), clazz);
    }

    // 读取指定长度的字节数组
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    // 读取指定长度的字节数组的一部分
    public int readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
        return len;
    }

    // 跳过指定字节数
    public int skipBytes(int n) throws IOException {
        int total = 0;
        int cur = 0;

        while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
            total += cur;
        }
        return total;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
