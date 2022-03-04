package com.fgsqw.lanshare.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

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

    public static int read(InputStream is, byte[] buf, int index, int len) throws IOException {
        int totalRecv = 0;
        int off = index;
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


}
