package com.fgsqw.lanshare.utils;


import java.io.UnsupportedEncodingException;


//数据解析类
public class DataDec {
    public static final int HEADER_LEN = 12;
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

    public byte getByte() {
        return getByte(index++);
    }

    public byte getByte(int i) {
        if ((i + i) <= byteLen) {
            return bytes[i];
        }
        return 0;
    }

    public int getInt() {
        int data = getInt(index);
        index += 4;
        return data;
    }

    public int getInt(int i) {
        if ((i + 4) <= byteLen) {
            return ByteUtil.bytesToInt(bytes, i);
        }
        return 0;
    }

    public boolean getBool() {
        return getByte() == 1;
    }

    public long getLong() {
        long data = getLong(index);
        index += 8;
        return data;
    }

    public long getLong(int i) {
        if ((i + 8) <= byteLen) {
            return ByteUtil.bytesToLong(bytes, i);
        }
        return 0;
    }

    public float getFloat() {
        return getInt() / 1000.0f;
    }

    public double getDouble() {
        return getLong() / 1000000.0d;
    }

    public byte[] getSurplusBytes() {
        if (byteLen - index > 0) {
            int surplus = byteLen - index;
            byte[] temp = new byte[surplus];
            System.arraycopy(bytes, index, temp, 0, surplus);
            index = byteLen;
            return temp;
        }
        return null;
    }


    public byte[] getBytes() {
        // 获取byte数组长度
        int byteLen = ByteUtil.bytesToInt(bytes, index);
        index += 4;
        if (bytes.length >= index + byteLen) {
            byte[] tmp = getBytes(byteLen);
            index += byteLen;
            return tmp;
        }
        return null;
    }


    public byte[] getBytes(int length) {
        if (byteLen >= index + length) {
            byte[] temp = new byte[length];
            System.arraycopy(bytes, index, temp, 0, length);
            index += length;
            return temp;
        }
        return null;
    }


    /**
     * 获取String
     *
     * @param en
     * @return
     */
    public String getString(String en) {
        // 获取字符串长度
        int strLen = ByteUtil.bytesToInt(bytes, index);
        index += 4;
        if (byteLen >= index + strLen) {
            String str = null;
            try {
                str = new String(bytes, index, strLen, en);
                index += strLen;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return str;
        }
        return null;
    }

    public String getString() {
        return getString("UTF-8");
    }


    public long getLastLong() {
        long length = getLength();
        if (length > 0) {
            return ByteUtil.bytesToLong(bytes, HEADER_LEN - 8);
        }
        return 0;
    }

    /**
     * 设置读取偏移
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

    public int getHeaderSize() {
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
