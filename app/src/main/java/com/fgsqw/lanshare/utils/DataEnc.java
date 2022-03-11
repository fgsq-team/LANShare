package com.fgsqw.lanshare.utils;


import java.io.UnsupportedEncodingException;

//数据包装类
public class DataEnc {
    public static final int HEADER_LEN = DataDec.HEADER_LEN; // 头信息字节长度
    private byte bytes[] = null;
    private int index = HEADER_LEN;
    private int byteLen = 0;

    private int cmd;
    private int count;
    private long length;

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
        ByteUtil.intToBytes(cmd, bytes, 0);
        return this;
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
        if (i + 1 <= byteLen) {
            bytes[i] = b;
            return this;
        }
        return null;
    }


    public DataEnc putInt(int val) {
        DataEnc dataEnc = putInt(val, index);
        index += 4;
        return dataEnc;
    }

    public DataEnc putInt(int val, int i) {
        if (i + 4 <= byteLen) {
            ByteUtil.intToBytes(val, bytes, i);
            return this;
        }
        return null;
    }

    public DataEnc putLong(long val) {
        DataEnc dataEnc = putLong(val, index);
        index += 8;
        return dataEnc;
    }

    public DataEnc putLong(long val, int i) {
        if (i + 8 <= byteLen) {
            ByteUtil.longToBytes(val, bytes, i);
            return this;
        }
        return null;
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
        DataEnc dataEnc = putBytes(bs, bs.length, index);
        index += length;
        return dataEnc;
    }

    public DataEnc putBytes(byte[] bs, int length) {
        DataEnc dataEnc = putBytes(bs, length, index);
        index += length;
        return dataEnc;
    }

    public DataEnc putBytes(byte[] bs, int length, int i) {
        if (i + length <= byteLen) {
            System.arraycopy(bs, 0, bytes, i, length);
            return this;
        }
        return null;
    }

    public DataEnc putString(String val) {
        try {
            byte[] bs = val.getBytes("UTF-8");
            if (index + bs.length <= byteLen) {
                putInt(bs.length);
                putBytes(bs);
                index += bs.length;
                return this;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public int getCmd() {
        return cmd;
    }

    public int getCount() {
        return count;
    }

    public long getLength() {
        return length;
    }

    /**
     * 设置偏移
     *
     * @param skip
     */
    public void skip(int skip) {
        index = HEADER_LEN + skip;
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
