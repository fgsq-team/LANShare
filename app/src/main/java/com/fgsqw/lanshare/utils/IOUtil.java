package com.fgsqw.lanshare.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class IOUtil {

    public static void closeIO(Closeable... io) {

        if (io != null && io.length > 0) {
            for (Closeable closeable : io) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static long writeAllData(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[2048];
        int ten = 0;
        long total = 0;
        while ((ten = input.read(buffer)) != -1) {
            output.write(buffer, 0, ten);
            total += ten;
        }
        return total;
    }


    public static String readInputTxt(InputStream inputStream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取Assets下的txt文件
     */
    public static String readAssetsTxt(Context context, String fileName) {
        try {
            return readInputTxt(context.getAssets().open(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 阻塞获取数据包直到达到len时跳出
     *
     * @param is     流
     * @param buf    缓冲区
     * @param offset 写入buff偏移
     * @param len    读取大小
     * @return 总读取大小
     */
    public static int read(InputStream is, byte[] buf, int offset, int len) throws IOException {
        int totalRecv = 0;
        int off = offset;
        int size = len;

        while (size > 0) {
            int i = is.read(buf, off, size);
            if (i <= 0) {
                return i;
            }
            off += i;
            totalRecv += i;
            size -= i;
        }

        return totalRecv;
    }

    public static boolean readHeader(InputStream is, DataDec dataDec) throws IOException {
        return read(is, dataDec.getData(), 0, DataEnc.getHeaderSize()) == DataEnc.getHeaderSize();
    }


    public static boolean read(InputStream is, DataDec dataDec) throws IOException {
        if (read(is, dataDec.getData(), 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            return false;
        int length = dataDec.getLength();
        if (read(is, dataDec.getData(), DataEnc.getHeaderSize(), length) != length)
            return false;
        return true;
    }

    public static String readHttpLine(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = is.read();
            if (read == -1) {
                return null;
            }
            char c = (char) read;
            if ((c == '\r')) {
                is.read();
                break;
            }
            sb.append((char) read);
        }
        return sb.toString();
    }


    public static void write(OutputStream out, DataEnc dataEnc) throws IOException {
        write(out, dataEnc.getData(), dataEnc.getDataLen());
    }

    public static void write(OutputStream out, byte[] buf) throws IOException {
        write(out, buf, buf.length);
    }

    public static void write(OutputStream out, byte[] buf, int len) throws IOException {
        write(out, buf, 0, len);
    }

    public static void write(OutputStream out, byte[] buf, int offset, int len) throws IOException {
        out.write(buf, offset, len);
        out.flush();
    }


    public static byte[] readBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ten = 0;
        while ((ten = is.read(buffer)) != -1) {
            out.write(buffer, 0, ten);
        }
        return out.toByteArray();
    }

    public static void transfer(InputStream is, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int ten = 0;
        while ((ten = is.read(buffer)) != -1) {
            out.write(buffer, 0, ten);
        }
    }


}
