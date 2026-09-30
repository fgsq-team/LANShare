package com.fgsqw.lanshare.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedByteBufferOutputStream extends OutputStream {
    private static final long DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024; // 4MB for Android
    private final FileChannel fileChannel;
    private final long chunkSize;
    private long position;
    private final long fileSize;
    private MappedByteBuffer mappedByteBuffer;

    public MappedByteBufferOutputStream(String filePath, long fileSize) throws IOException {
        this(filePath, fileSize, DEFAULT_CHUNK_SIZE);
    }

    public MappedByteBufferOutputStream(String filePath, long fileSize, long chunkSize) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
        randomAccessFile.setLength(fileSize);
        this.fileChannel = randomAccessFile.getChannel();
        this.chunkSize = chunkSize;
        this.position = 0;
        this.fileSize = fileSize;
        this.mappedByteBuffer = mapChunk();
    }

    private MappedByteBuffer mapChunk() throws IOException {
        long remaining = fileSize - position;
        long size = Math.min(chunkSize, remaining);
        return fileChannel.map(FileChannel.MapMode.READ_WRITE, position, size);
    }

    @Override
    public void write(int b) throws IOException {
        if (!mappedByteBuffer.hasRemaining()) {
            mapNextChunk();
        }
        mappedByteBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            if (!mappedByteBuffer.hasRemaining()) {
                mapNextChunk();
            }
            int bytesToWrite = Math.min(len, mappedByteBuffer.remaining());
            mappedByteBuffer.put(b, off, bytesToWrite);
            off += bytesToWrite;
            len -= bytesToWrite;
        }
    }

    private void mapNextChunk() throws IOException {
        position += mappedByteBuffer.position();
        mappedByteBuffer = mapChunk();
    }

    public void seek(long newPosition) throws IOException {
        if (newPosition < 0 || newPosition >= fileSize) {
            throw new IllegalArgumentException("Seek position out of file bounds");
        }
        position = newPosition;
        mappedByteBuffer = mapChunk();
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
