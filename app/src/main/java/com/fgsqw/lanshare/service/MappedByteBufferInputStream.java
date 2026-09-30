package com.fgsqw.lanshare.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedByteBufferInputStream extends InputStream {

    private static final long DEFAULT_CHUNK_SIZE = 64 * 1024 * 1024;
    private final FileChannel fileChannel;
    private final long fileSize;
    private final long chunkSize;
    private long position;
    private MappedByteBuffer mappedByteBuffer;

    public MappedByteBufferInputStream(String filePath) throws IOException {
        this(filePath, DEFAULT_CHUNK_SIZE);
    }

    public MappedByteBufferInputStream(String filePath, long chunkSize) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r");
        this.fileChannel = randomAccessFile.getChannel();
        this.fileSize = fileChannel.size();
        this.chunkSize = chunkSize;
        this.position = 0;
        this.mappedByteBuffer = mapChunk();
    }

    private MappedByteBuffer mapChunk() throws IOException {
        long remaining = fileSize - position;
        if (remaining <= 0) {
            return null;
        }
        long size = Math.min(chunkSize, remaining);
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
    }

    @Override
    public int read() throws IOException {
        if (position >= fileSize) {
            return -1;
        }
        if (mappedByteBuffer == null || !mappedByteBuffer.hasRemaining()) {
            mappedByteBuffer = mapChunk();
            if (mappedByteBuffer == null) {
                return -1;
            }
        }
        position++;
        return mappedByteBuffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position >= fileSize) {
            return -1;
        }
        int bytesRead = 0;
        while (bytesRead < len && position < fileSize) {
            if (mappedByteBuffer == null || !mappedByteBuffer.hasRemaining()) {
                mappedByteBuffer = mapChunk();
                if (mappedByteBuffer == null) {
                    break;
                }
            }
            int bytesToRead = Math.min(len - bytesRead, mappedByteBuffer.remaining());
            mappedByteBuffer.get(b, off + bytesRead, bytesToRead);
            position += bytesToRead;
            bytesRead += bytesToRead;
        }
        return bytesRead > 0 ? bytesRead : -1;
    }

    public void seek(long newPosition) throws IOException {
        if (newPosition < 0 || newPosition >= fileSize) {
            throw new IllegalArgumentException("Seek position out of file bounds");
        }
        position = newPosition;
        mappedByteBuffer = mapChunk();
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, fileSize - position));
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
