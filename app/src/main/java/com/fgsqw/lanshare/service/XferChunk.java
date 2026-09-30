package com.fgsqw.lanshare.service;

import com.fgsqw.lanshare.utils.DataEnc;

/**
 * 传输分块缓冲区。
 *
 * 收发两端都用它做双缓冲流水线：读线程与写线程交替使用不同缓冲区，
 * 于是「读盘」与「写 socket」（发送端）、「读 socket」与「落盘」（接收端）
 * 两个阶段可以重叠执行，而不是像原来那样单线程串行等待。
 *
 * 缓冲区总量很小（2 块 × 2MB），靠 ArrayBlockingQueue 当环形缓冲复用，
 * 既限制了预读深度（不会把整个文件读进内存），又让读线程可以领先写线程一块。
 */
public class XferChunk {

    // 数据缓冲区。发送端在 DataEnc.getHeaderSize() 偏移处写数据（头+数据一次写出），
    // 接收端从 0 开始写（头单独读进 dataDec）。
    public final byte[] buf;

    // 发送端复用：每个缓冲区自带 DataEnc，保证「12 字节头 + 载荷」能一次 write 出去
    public final DataEnc dataEnc;

    // 本块有效数据长度；<= 0 表示「读线程已结束」的终止标记
    public int len;

    // 本块在文件中的起始偏移，加密流用它做 XOR 偏移量
    public long offset;

    public XferChunk(int size) {
        this.buf = new byte[size];
        this.dataEnc = new DataEnc(this.buf);
    }
}
