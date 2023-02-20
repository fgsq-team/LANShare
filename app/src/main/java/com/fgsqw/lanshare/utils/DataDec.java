package com.fgsqw.lanshare.utils;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

//数据解析类
public class DataDec {
    private static final int HEADER_LEN = 12;
    private byte[] bytes = null;
    private int byteLen = 0;
    private int index = HEADER_LEN;

    public DataDec() {
        byteLen = HEADER_LEN;
        bytes = new byte[byteLen];
    }

    public DataDec(int size) {
        byteLen = size + HEADER_LEN;
        bytes = new byte[byteLen]; // 加上头字节大小
    }


    public DataDec(byte[] bs) {
        setData(bs, bs.length);
    }

    public DataDec(byte[] bs, int byteLen) {
        setData(bs, byteLen);
    }

    public void setData(byte[] bs, int byteLen) {
        reset();                 // 重置读取下标
        this.byteLen = byteLen;
        this.bytes = bs;
    }


    public int getCmd() {
        return getInt(0);
    }

    public byte getByteCmd() {
        return getByte(0);
    }

    public int getCount() {
        return getInt(4);
    }

    public int getLength() {
        return getInt(8);
    }


    public byte getByte() {
        return getByte(index++);
    }

    public byte getByte(int i) {
        return bytes[i];
    }

    public int getInt() {
        int data = getInt(index);
        index += TypeLength.INT_LEN;
        return data;
    }

    public int getInt(int i) {
        return ByteUtil.bytesToInt(bytes, i);
    }

    public boolean getBool() {
        return getByte() == 1;
    }

    public long getLong() {
        long data = getLong(index);
        index += TypeLength.LONG_LEN;
        return data;
    }

    public long getLong(int i) {
        return ByteUtil.bytesToLong(bytes, i);
    }

    public float getFloat() {
        return getInt() / 1000.0f;
    }

    public double getDouble() {
        return getLong() / 1000000.0d;
    }

    /**
     * 从数据包中获取剩下的byte数据
     */
    public byte[] getSurplusBytes() {
        int surplus = byteLen - index;
        byte[] temp = new byte[surplus];
        System.arraycopy(bytes, index, temp, 0, surplus);
        index = byteLen;
        return temp;
    }

    /**
     * 从数据包中获取剩下的byte数据
     *
     * @param buff 缓存
     */
    public void getSurplusBytes(byte[] buff) {
        getSurplusBytes(buff, 0);
    }


    /**
     * 从数据包中获取剩下的byte数据
     *
     * @param buff 缓存
     * @param off  缓存偏移
     */
    public void getSurplusBytes(byte[] buff, int off) {
        int surplus = byteLen - index;
        System.arraycopy(bytes, index, buff, off, surplus);
        index = byteLen;
    }


    /**
     * 从数据包中获取byte数组
     */
    public byte[] getBytes() {
        // 获取byte数组长度
        int byteLen = getInt();
        byte[] temp = new byte[byteLen];
        System.arraycopy(bytes, index, temp, 0, byteLen);
        index += byteLen;
        return temp;
    }


    /**
     * 从数据包中获取一段数据
     *
     * @param index 数据中的包偏移
     * @param buff  缓存
     * @param off   缓存偏移
     * @param len   获取长度
     */
    public void getBytes(int index, byte[] buff, int off, int len) {
        System.arraycopy(bytes, index + HEADER_LEN, buff, off, len);
    }


    /**
     * 获取String
     *
     * @param en
     * @return
     */
    public String getString(Charset en) {
        byte[] bytes = getBytes();
        return new String(bytes, en);
    }

    public String getString() {
        return getString(FileUtil.UTF_8);
    }

    public long getLastLong() {
        return getLong(index - TypeLength.LONG_LEN);
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


    public static int getHeaderSize() {
        return HEADER_LEN;
    }

    public int getByteLen() {
        return byteLen;
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
        return bytes;
    }
}
