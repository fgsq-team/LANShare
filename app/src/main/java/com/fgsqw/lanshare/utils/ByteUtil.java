package com.fgsqw.lanshare.utils;

public class ByteUtil {

    public static byte[] interToBytes(int[] nums) {
        byte[] b = new byte[nums.length * 4];
        for (int i = 0; i < nums.length; i++) {
            b[4 * i] = (byte) ((nums[i] >> 24) & 255);
            b[4 * i + 1] = (byte) ((nums[i] >> 16) & 255);
            b[4 * i + 2] = (byte) ((nums[i] >> 8) & 255);
            b[4 * i + 3] = (byte) (nums[i] & 255);
        }
        return b;
    }

    public static byte[] intToBytes(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) ((i >> 24) & 255);
        b[1] = (byte) ((i >> 16) & 255);
        b[2] = (byte) ((i >> 8) & 255);
        b[3] = (byte) (i & 255);
        return b;
    }

    public static byte[] intToBytes(int i, byte[] b, int off) {
        b[off] = (byte) ((i >> 24) & 255);
        b[1 + off] = (byte) ((i >> 16) & 255);
        b[2 + off] = (byte) ((i >> 8) & 255);
        b[3 + off] = (byte) (i & 255);
        return b;
    }

    public static byte[] longToBytes(long i) {
        byte[] b = new byte[8];
        b[0] = (byte) ((i >> 56) & 255);
        b[1] = (byte) ((i >> 48) & 255);
        b[2] = (byte) ((i >> 40) & 255);
        b[3] = (byte) ((i >> 32) & 255);
        b[4] = (byte) ((i >> 24) & 255);
        b[5] = (byte) ((i >> 16) & 255);
        b[6] = (byte) ((i >> 8) & 255);
        b[7] = (byte) (i & 255);
        return b;
    }

    public static void longToBytes(long i, byte[] b, int off) {
        b[off] = (byte) ((i >> 56) & 255);
        b[1 + off] = (byte) ((i >> 48) & 255);
        b[2 + off] = (byte) ((i >> 40) & 255);
        b[3 + off] = (byte) ((i >> 32) & 255);
        b[4 + off] = (byte) ((i >> 24) & 255);
        b[5 + off] = (byte) ((i >> 16) & 255);
        b[6 + off] = (byte) ((i >> 8) & 255);
        b[7 + off] = (byte) (i & 255);
    }

    public static int bytesToInt(byte[] buf, int offset) {
        int i = 0;
        i = i | ((buf[offset] & 255) << 24);
        i = i | ((buf[offset + 1] & 255) << 16);
        i = i | ((buf[offset + 2] & 255) << 8);
        i = i | (buf[offset + 3] & 255);
        return i;
    }


    public static int[] bytesToInter(byte[] buf, int offset, int len) {
        int[] s = new int[(len - offset) / 4];
        for (int i = offset; i < len; i += 4) {
            int v = s[i / 4];

            v = v | ((buf[i] & 255) << 24);
            v = v | ((buf[i + 1] & 255) << 16);
            v = v | ((buf[i + 2] & 255) << 8);
            v = v | (buf[i + 3] & 255);

            s[i / 4] = v;
        }

        return s;
    }

    public static long bytesToLong(byte[] buf, int offset) {
        long i = 0;
        i = i | (((long) buf[offset] & 255) << 56)
                | (((long) buf[offset + 1] & 255) << 48)
                | (((long) buf[offset + 2] & 255) << 40)
                | (((long) buf[offset + 3] & 255) << 32)
                | (((long) buf[offset + 4] & 255) << 24)
                | (((long) buf[offset + 5] & 255) << 16)
                | (((long) buf[offset + 6] & 255) << 8)
                | ((long) buf[offset + 7] & 255);
        return i;
    }


    public static void main(String[] args) {
        float[] f = {0.35f, 0.4654f, 4156.554635574f};
        byte[] floatsbytes = floatsbytes(f);
        float[] bytesfloats = bytesfloats(floatsbytes, 0);
        for (float bytesfloat : bytesfloats) {
            System.out.println("bytesfloat = " + bytesfloat);
        }

    }

    /**
     * 浮点转换为字节
     *
     * @param f
     * @return
     */
    public static byte[] float2byte(float f) {

        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < 2; ++i) {
            temp = b[i];
            b[i] = b[4 - i - 1];
            b[4 - i - 1] = temp;
        }

        return b;

    }

    public static byte[] floatsbytes(float[] f) {

        byte[] b = new byte[f.length * 4];
        for (int i = 0; i < f.length; i++) {
            int fbit = Float.floatToIntBits(f[i]);

            for (int s = 0; s < 4; s++) {
                b[i * 4 + s] = (byte) (fbit >> (24 - s * 8));
            }

            // 翻转数组
            byte temp;
            // 将顺位第i个与倒数第i个交换
            for (int s = 0; s < 2; ++s) {
                temp = b[i * 4 + s];
                b[i * 4 + s] = b[i * 4 + 4 - s - 1];
                b[i * 4 + 4 - s - 1] = temp;
            }

        }

        return b;

    }

    /**
     * 字节转换为浮点
     *
     * @param b      字节（至少4个字节）
     * @param offset 开始位置
     * @return
     */
    public static float[] bytesfloats(byte[] b, int offset) {
        float fs[] = new float[b.length / 4];
        for (int i = 0; i < fs.length; i++) {

            int l;
            l = b[offset + i * 4];
            l &= 0xff;
            l |= ((long) b[offset + (i * 4) + 1] << 8);
            l &= 0xffff;
            l |= ((long) b[offset + (i * 4) + 2] << 16);
            l &= 0xffffff;
            l |= ((long) b[offset + (i * 4) + 3] << 24);

            fs[i] = Float.intBitsToFloat(l);
        }
        return fs;
    }

    /**
     * 字节转换为浮点
     *
     * @param b     字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    public static float bytesfloat(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

}
