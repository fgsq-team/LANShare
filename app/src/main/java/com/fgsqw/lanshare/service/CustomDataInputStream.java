package com.fgsqw.lanshare.service;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 自定义数据输入流
 * <p>提供基础数据类型的读取功能,支持按大端序读取各种数据类型</p>
 * <p>支持的数据类型包括:boolean、byte、short、int、long、float、double、char、String 和对象</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class CustomDataInputStream extends InputStream {
    /** 底层输入流 */
    private final InputStream in;
    
    /** 缓冲区,用于读取基础数据类型 */
    private final byte[] buffer = new byte[8];

    /**
     * 构造函数
     *
     * @param in 底层输入流
     */
    public CustomDataInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * 读取一个 boolean 值
     *
     * @return 读取到的 boolean 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public boolean readBoolean() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch != 0;
    }

    /**
     * 读取一个 byte 值
     *
     * @return 读取到的 byte 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    /**
     * 读取一个无符号 byte 值
     *
     * @return 读取到的无符号 byte 值(0-255)
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /**
     * 读取一个 short 值(大端序)
     *
     * @return 读取到的 short 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + (ch2));
    }

    /**
     * 读取一个无符号 short 值(大端序)
     *
     * @return 读取到的无符号 short 值(0-65535)
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch1 << 8) + (ch2);
    }

    /**
     * 读取一个 char 值(大端序)
     *
     * @return 读取到的 char 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char) ((ch1 << 8) + (ch2));
    }

    /**
     * 读取一个 int 值(大端序)
     *
     * @return 读取到的 int 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
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

    /**
     * 读取一个 long 值(大端序)
     *
     * @return 读取到的 long 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
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

    /**
     * 读取一个 float 值(大端序)
     *
     * @return 读取到的 float 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * 读取一个 double 值(大端序)
     *
     * @return 读取到的 double 值
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * 读取一个 UTF-8 编码的字符串
     * <p>格式:先读取 4 字节长度,再读取指定长度的字节数据</p>
     *
     * @return 读取到的字符串,如果长度为 -1 则返回 null
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public String readString() throws IOException {
        int length = readInt();
        if (length == -1) {
            return null;
        }
        if (length == 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    /**
     * 读取一个 JSON 对象并转换为指定类型的 Java 对象
     *
     * @param clazz 目标类型的 Class 对象
     * @param <T>   目标类型
     * @return 转换后的 Java 对象
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public <T> T readObject(Class<T> clazz) throws IOException {
        String json = readString();
        return JSON.toJavaObject(JSON.parseObject(json), clazz);
    }

    /**
     * 读取指定长度的字节数组
     *
     * @param b 目标字节数组
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * 读取指定长度的字节数组的一部分
     *
     * @param b   目标字节数组
     * @param off 起始偏移量
     * @param len 要读取的字节数
     * @return 实际读取的字节数
     * @throws IOException 如果到达流末尾或发生 I/O 错误
     */
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

    /**
     * 跳过指定字节数
     *
     * @param n 要跳过的字节数
     * @return 实际跳过的字节数
     * @throws IOException 如果发生 I/O 错误
     */
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
