package com.fgsqw.lanshare.service;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 自定义数据输出流
 * <p>提供基础数据类型的写入功能,支持按大端序写入各种数据类型</p>
 * <p>支持的数据类型包括:boolean、byte、short、int、long、float、double、char、String 和字节数组</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class CustomDataOutputStream extends OutputStream {
    /** 底层输出流 */
    private final OutputStream out;

    /** 缓冲区,用于写入基础数据类型 */
    private final byte[] buffer = new byte[8];

    /**
     * 构造函数
     *
     * @param out 底层输出流
     */
    public CustomDataOutputStream(OutputStream out) {
        this.out = out;
    }

    /**
     * 写入一个 boolean 值
     *
     * @param v 要写入的 boolean 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    /**
     * 写入一个 byte 值
     *
     * @param v 要写入的 byte 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeByte(int v) throws IOException {
        out.write(v);
    }

    /**
     * 写入一个 short 值(大端序)
     *
     * @param v 要写入的 short 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeShort(int v) throws IOException {
        buffer[0] = (byte) (v >>> 8);
        buffer[1] = (byte) v;
        out.write(buffer, 0, 2);
    }

    /**
     * 写入一个 char 值(大端序)
     *
     * @param v 要写入的 char 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeChar(int v) throws IOException {
        buffer[0] = (byte) (v >>> 8);
        buffer[1] = (byte) v;
        out.write(buffer, 0, 2);
    }

    /**
     * 写入一个 int 值(大端序)
     *
     * @param v 要写入的 int 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeInt(int v) throws IOException {
        buffer[0] = (byte) (v >>> 24);
        buffer[1] = (byte) (v >>> 16);
        buffer[2] = (byte) (v >>> 8);
        buffer[3] = (byte) v;
        out.write(buffer, 0, 4);
    }

    /**
     * 写入一个 long 值(大端序)
     *
     * @param v 要写入的 long 值
     * @throws IOException 如果发生 I/O 错误
     */
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

    /**
     * 写入一个 float 值(大端序)
     *
     * @param v 要写入的 float 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /**
     * 写入一个 double 值(大端序)
     *
     * @param v 要写入的 double 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /**
     * 写入一个 UTF-8 编码的字符串
     * <p>格式:先写入 4 字节长度,再写入字节数据</p>
     *
     * @param str 要写入的字符串,如果为 null 则写入长度 -1
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeString(String str) throws IOException {
        if (str == null) {
            writeInt(-1);
        } else {
            byte[] bytes = str.getBytes("UTF-8");
            writeInt(bytes.length);
            out.write(bytes);
        }
    }

    /**
     * 写入完整的字节数组
     *
     * @param b 要写入的字节数组
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeBytes(byte[] b) throws IOException {
        out.write(b, 0, b.length);
    }

    /**
     * 写入字节数组的一部分
     *
     * @param b   字节数组
     * @param off 起始偏移量
     * @param len 要写入的字节数
     * @throws IOException 如果发生 I/O 错误
     */
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

    /**
     * 刷新缓冲区,强制写出所有缓冲的输出字节
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
