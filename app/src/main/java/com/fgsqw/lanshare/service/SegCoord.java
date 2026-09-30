package com.fgsqw.lanshare.service;

import com.fgsqw.lanshare.pojo.message.MessageFileContent;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一次分段并行传输的共享状态。
 *
 * 4 条连接各自独立跑，但它们指向同一个文件，必须协调三件事：
 * 1. 落盘路径只算一次：4 次 avoidDuplication 会得到 4 个不同文件名，必须由首段决定，其余段复用
 * 2. 预分配全长：并发的 RandomAccessFile 各自 setLength 会互相截断，必须在写之前由首段定好长度
 * 3. 整体进度与收尾：进度是 4 段字节数之和，收尾只由「凑满 segCount 的那一段」做一次
 */
final class SegCoord {

    private static final ConcurrentHashMap<String, SegCoord> MAP = new ConcurrentHashMap<>();
    private static final Object JOIN_LOCK = new Object();

    final String segId;
    final long fileSize;
    final int segCount;
    final MessageFileContent fileContent;

    private final AtomicInteger arrived = new AtomicInteger();
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong lastTpMs = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastTpSent = new AtomicLong();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicBoolean presented = new AtomicBoolean();
    private final Thread creator;
    private final File file;
    private volatile boolean broken;

    private SegCoord(String segId, long fileSize, int segCount, MessageFileContent fileContent, File file) {
        this.segId = segId;
        this.fileSize = fileSize;
        this.segCount = segCount;
        this.fileContent = fileContent;
        this.file = file;
        this.creator = Thread.currentThread();
    }

    /**
     * 计算落盘路径的动作。只在竞争胜出的那个线程里执行一次。
     */
    interface Resolver {
        File resolve();
    }

    /**
     * 加入某个 segId 的分段传输，返回该 segId 唯一的协调器。
     *
     * 落盘路径的确定必须是「算完再发布」，而且所有段都必须用协调器里那一个路径：
     * avoidDuplication 每调一次就会得到一个不同的文件名，如果 4 段各自算一次，
     * 就会把一个文件拆成 4 个文件写出去。所以这里先在锁内算好路径再放进 MAP，
     * 输掉竞争的线程随后一定拿得到同一个路径，不存在「自己算了自己的」这种情况。
     *
     * @return 协调器；解析不出路径时返回 null
     */
    static SegCoord join(String segId, long fileSize, int segCount,
                         MessageFileContent fileContent, Resolver resolver) {
        SegCoord c = MAP.get(segId);
        if (c != null) {
            return c;
        }
        synchronized (JOIN_LOCK) {
            c = MAP.get(segId);
            if (c != null) {
                return c;
            }
            File f = resolver.resolve();
            if (f == null) {
                return null;
            }
            SegCoord n = new SegCoord(segId, fileSize, segCount, fileContent, f);
            MAP.put(segId, n);
            return n;
        }
    }

    /**
     * 本段是否是「第一个到达」的段（也就是唯一有资格定名、弹确认框、预分配长度的那一段）。
     * 由协调器自己判定，而不是各段各自去猜，避免两个线程同时认为自己是第一个。
     */
    boolean isFirst() {
        return creator == Thread.currentThread();
    }

    static void remove(String segId) {
        MAP.remove(segId);
    }

    static SegCoord peek(String segId) {
        return MAP.get(segId);
    }

    File file() {
        return file;
    }

    long addReceived(long n) {
        long sum = received.addAndGet(n);
        // 每秒一行接收侧聚合吞吐：与发送端 SEND-TP 对照
        long now = System.currentTimeMillis();
        long lm = lastTpMs.get();
        if (now - lm >= 1000 && lastTpMs.compareAndSet(lm, now)) {
            long ls = lastTpSent.getAndSet(sum);
            long ds = sum - ls;
            long ms = now - lm;
            long aggX10 = ds * 10000L / ms / 1048576L;
            String line = "RECV-TP agg=" + aggX10 / 10 + "." + aggX10 % 10 + "MB/s delta="
                    + (ds >> 20) + "MB done=" + (sum >> 20) + "/" + (fileSize >> 20)
                    + "MB active~" + (segCount - arrived.get());
            android.util.Log.e("XFER", line);
        }
        return sum;
    }

    long received() {
        return received.get();
    }

    int arrived() {
        return arrived.get();
    }

    void markBroken() {
        broken = true;
    }

    boolean broken() {
        return broken;
    }

    /**
     * 「本段的传输已经结束」才允许调用一次，且必须在 recvSegFile 返回值之后调用。
     * 返回 true 表示所有段都结束了，这一条负责做整体收尾。
     * 这样保证收尾时其它段的数据都已经落盘并计入 received，不会误删。
     */
    boolean arrive() {
        return arrived.incrementAndGet() >= segCount;
    }

    /**
     * 整体收尾只允许执行一次：谁先抢到谁做，防止「拒绝接收」被多次触发时重复收尾/重复删文件。
     */
    boolean finishOnce() {
        return finished.compareAndSet(false, true);
    }

    /**
     * 只允许一个线程把「接收中」进度行加进列表，4 条连接 + 弹窗回调都走这里，
     * 否则 UI 会冒出多条重复进度行。
     */
    boolean presentOnce() {
        return presented.compareAndSet(false, true);
    }
}
