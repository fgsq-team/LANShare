package com.fgsqw.lanshare.utils;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

//数据包装类
public class DataEnc {
    private static final int HEADER_LEN = DataDec.getHeaderSize(); // 头信息字节长度
    private byte[] bytes = null;
    private int index = HEADER_LEN;
    private int byteLen = 0;

    private int cmd;
    private int count;
    private int length;

    public DataEnc() {
        byteLen = HEADER_LEN;
        bytes = new byte[byteLen]; // 加上头字节大小
    }

    public DataEnc(int size) {
        byteLen = size + HEADER_LEN;
        bytes = new byte[byteLen]; // 加上头字节大小
    }

    public DataEnc(byte[] bs) {
        setData(bs, bs.length);
    }

    public DataEnc(byte[] bs, int length) {
        setData(bs, length);
    }

    public void setData(byte[] bs, int length) {
        reset(); // 重置读取下标
        this.bytes = bs;
        byteLen = bs.length;
    }

    public void packData(int cmd) {
        index = HEADER_LEN;
        setCmd(cmd);
        setCount(0);
        setLength(0);
    }

    public DataEnc setCmd(int cmd) {
        this.cmd = cmd;
        return putInt(cmd, 0);
    }

    public DataEnc setByteCmd(byte cmd) {
        this.cmd = cmd;
        return putByte(cmd, 0);
    }

    public DataEnc setCount(int count) {
        this.count = count;
        return putInt(count, 4);
    }


    public DataEnc setLength(int length) {
        this.length = length;
        return putInt(length, 8);
    }

    public DataEnc putByte(byte b) {
        return putByte(b, index++);
    }

    public DataEnc putByte(byte b, int i) {
        bytes[i] = b;
        return this;
    }

    public DataEnc putInt(int val) {
        putInt(val, index);
        index += TypeLength.INT_LEN;
        return this;
    }

    public DataEnc putInt(int val, int i) {
        ByteUtil.intToBytes(val, bytes, i);
        return this;

    }

    public DataEnc putLong(long val) {
        putLong(val, index);
        index += TypeLength.LONG_LEN;
        return this;
    }

    public DataEnc putLong(long val, int i) {
        ByteUtil.longToBytes(val, bytes, i);
        return this;

    }

    public DataEnc putBool(boolean val) {
        return putByte((byte) (val ? 1 : 0));
    }

    public DataEnc putFloat(float val) {
        return putInt((int) (val * 1000));
    }

    public DataEnc putDouble(double val) {
        return putLong((long) (val * 1000000));
    }

    public DataEnc putBytes(byte[] bs) {
        putBytes(bs, bs.length);
        return this;
    }

    public DataEnc putBytes(byte[] bs, int length) {
        return putBytes(bs, 0, length);
    }

    public DataEnc putBytes(byte[] bs, int off, int length) {
        putInt(length);
        putBytes(bs, off, length, index);
        index += length;
        return this;
    }

    public DataEnc putBytes(byte[] bs, int off, int length, int i) {
        System.arraycopy(bs, off, bytes, i, length);
        return this;
    }

    public DataEnc putString(String val) {
        return putString(val, FileUtil.UTF_8);
    }


    public DataEnc putString(String val, Charset en) {
        putBytes(val.getBytes(en));
        return this;
    }

    public int getCmd() {
        return cmd;
    }

    public int getCount() {
        return count;
    }

    public int getLength() {
        return length;
    }

    /**
     * 设置读取偏移
     **/
    public void seek(int seek) {
        index = HEADER_LEN + seek;
    }

    /**
     * 跳过指定长度偏移
     *
     * @param skip
     */
    public void skip(int skip) {
        index += skip;
    }


    /**
     * 重置读取下标
     */
    public void reset() {
        setDataIndex(0);
    }

    public int getDataLen() {
        return index;
    }

    public static int getHeaderSize() {
        return HEADER_LEN;
    }

    // 获取数据下标
    public int getDataIndex() {
        return index - HEADER_LEN;
    }


    // 设置数据下标
    public void setDataIndex(int index) {
        this.index = index + HEADER_LEN;
    }

    public byte[] getData() {
        setLength(index - HEADER_LEN);
        return bytes;
    }
}
