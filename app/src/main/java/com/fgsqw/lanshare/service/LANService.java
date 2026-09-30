package com.fgsqw.lanshare.service;


import android.annotation.SuppressLint;
//import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.*;
//import android.telephony.PhoneStateListener;
//import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DeviceQrCodeActivity;
import com.fgsqw.lanshare.base.BaseService;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.LVersion;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.FileSyncDBUtil;
import com.fgsqw.lanshare.db.MediaIdPathDBUtil;
import com.fgsqw.lanshare.db.MesssageDButil;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.listener.ImageObserver;
//import com.fgsqw.lanshare.listener.LFileObserver;
//import com.fgsqw.lanshare.listener.LPhoneStateListener;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileSyncData;
import com.fgsqw.lanshare.pojo.SendTask;
import com.fgsqw.lanshare.pojo.file.*;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.pojo.network.NetInfo;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.receiver.LANShareReceiver;
import com.fgsqw.lanshare.receiver.NetWorkReceiver;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.web.LHttpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

public class LANService extends BaseService {
    public static final String TAG = "LANService";
    public static LANService instance;
    // TCP服务监听
    private ServerSocket fileReceive;
    // 在线设备列表
    private final Map<String, Device> onLineDevices = new ConcurrentHashMap<>();
    public final Map<String, Device> onLineWebDevices = new ConcurrentHashMap<>();
    private Messenger mMessenger;
    // 本机IP列表
    public Set<Device> localDevices = Collections.synchronizedSet(new HashSet<>());
    public List<NetInfo> ipv6NetInfoList = new ArrayList<>();
    private NetWorkReceiver netWorkReceiver;
    // 来电监听
//    private LPhoneStateListener phoneStateListener;
    private boolean running = true;
    // 系统音量保存
    int systemVolume = 0;
    // http服务
    private LHttpServer httpServer;
    public MediaIdPathDBUtil mediaIdPathDBUtil;
    private String currentIp = "";
    // 媒体变动监听
    private ImageObserver imageObserver;
    //    private LFileObserver lFileObserver;
    private FileSyncDBUtil fileSyncDBUtil;

    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    private MesssageDButil messsageDButil;

    private void getIpv6() {
        ThreadUtils.runThread(() -> {
            Lock lock = StringLockManager.getStringLock("getIpv6");
            if (!lock.tryLock()) {
                return;
            }
            try {
                List<NetInfo> openIpv6 = NetWorkUtil.getOpenIpv6();
                ipv6NetInfoList.clear();
                ipv6NetInfoList.addAll(openIpv6);
            } finally {
                lock.unlock();
            }
        });
    }

    public void addDevice(Device device) {
        String address = device.getDevIP() + ":" + device.getDevPort();
        onLineDevices.put(address, device);
        LHttpServer.sendDeviceList();
    }

    public void removeDevice(String address) {
        onLineDevices.remove(address);
        LHttpServer.sendDeviceList();
    }

    private void encData(byte[] buffer, int len, int off, long index) {
        int j = 0;
        for (int i = off; i < len + off; i++) {
            int v = (buffer[i] - 1) ^ (int) ((index + j) & 0xFF);
            buffer[i] = (byte) v;
            j++;
        }
    }

    private void decData(byte[] buffer, int len, int off, long index) {
        int j = 0;
        for (int i = off; i < len + off; i++) {
            int v = (buffer[i] ^ (int) ((index + j) & 0xFF)) + 1;
            buffer[i] = (byte) v;
            j++;
        }
    }

    public Map<String, Device> getOnLineDevices() {
        return onLineDevices;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Object messenger = Objects.requireNonNull(intent.getExtras()).get("messenger");
            if (messenger != null) {
                mMessenger = (Messenger) messenger;
            }
            currentIp = "";
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static LANService getInstance() {
        return instance;
    }

    WifiManager.MulticastLock multicastLock;
    // 传输期间必须持有高性能 WifiLock，否则系统省电策略会让 Wi-Fi 进低功耗缓冲模式，
    // 多流并行的吞吐会被压在几十 MB/s；用计数器保证并发传输时最后一个结束才释放
    WifiManager.WifiLock wifiLock;
    private final AtomicInteger xferLockCount = new AtomicInteger();

    /** 传输开始：计数 +1，第一个拿到时锁住 Wi-Fi 高性能模式 */
    void acquireWifiLock() {
        int n = xferLockCount.incrementAndGet();
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
        xfer("WIFI-LOCK acquire count=" + n + (wifiLock != null && wifiLock.isHeld() ? " held=true" : " held=false"));
    }

    /** 每个文件/整批收尾时调用，计数归零立即释放，避免常驻耗电 */
    void releaseWifiLock() {
        int n = xferLockCount.decrementAndGet();
        if (n <= 0 && wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        xfer("WIFI-LOCK release count=" + n + " held=" + (wifiLock != null && wifiLock.isHeld()));
    }

    /** 日志用：这条段连接的对端 ip:port，方便和发送端日志对时间线 */
    static String safePeer(Socket s) {
        try {
            return s.getRemoteSocketAddress() == null ? "?" : s.getRemoteSocketAddress().toString();
        } catch (Exception e) {
            return "?";
        }
    }

    public void startWebServer() {
        httpServer = new LHttpServer(this);
        try {
            httpServer.startBlendingModeHttpServer();
        } catch (IOException e) {
            LLog.error("error", e);
        }
    }

    @Override
    public void onCreate() {
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = mWifiManager.createMulticastLock("multicastLock");
        wifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "lanShareWifiLock");
        wifiLock.setReferenceCounted(false);
        int batteryLevel = BatteryUtil.getBatteryLevel(this)[0];
        LLog.debug("batteryLevel: " + batteryLevel);
//        multicastLock.setReferenceCounted(false);
        super.onCreate();
        messsageDButil = new MesssageDButil(this);
        mediaIdPathDBUtil = new MediaIdPathDBUtil(this);
        instance = this;
        startWebServer();
        // Service保活
        NotificationUtils.showBackendNotification(this, getString(R.string.app_name), getString(R.string.service_is_running));
        // 初始化数据
        initData();
        // 监听网络
        networkReceiver();
        // UDP广播监听
        udpServer();
        // 文件接收监听
        tcpServer();
        // 广播局域网所有设备我已上线
        ThreadUtils.runThread(() -> {
            for (Device device : localDevices) {
                noticeDeviceStateByIp(device, true, true);
            }
        });
        // UDP 广播扫描设备
        scanDevices();
        // 来电状态监听
//        callStateListen();
        initMediaListener();
//        if (Config.SYNC_NOTIFICATION) {
//            // 通知监听
//            initNotifiService();
//        }
    }

    private void initMediaListener() {
        fileSyncDBUtil = new FileSyncDBUtil(this);
        imageObserver = new ImageObserver(this, new Handler());
//        lFileObserver = new LFileObserver("/sdcard/LANShare", FileObserver.CREATE | FileObserver.DELETE | FileObserver.MODIFY | FileObserver.CLOSE_WRITE);
//        lFileObserver.startWatching();
        if (Config.MEDIA_SYNC) {
            imageObserver.registerObserver();
        }
    }

 /*   public void initNotifiService() {
        Intent intent = null;//启动服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(this, LNotifiService.class);
            startService(intent);//启动服务
        }
    }*/

    /**
     * 来电状态监听
     */
  /*  public void callStateListen() {
        phoneStateListener = new LPhoneStateListener();
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (SecurityException e) {
              LLog.error(e);
        }

    }*/

    /**
     * @author fgsq
     * @comments 初始化设备IP地址列表
     * @date 2024/5/22 11:02
     */
    public void initData() {
        getIpv6();
    }

    /**
     * @author fgsq
     * @comments 网络状态监听，用来刷新设备IP
     * @date 2024/5/22 11:03
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void networkReceiver() {
        netWorkReceiver = new NetWorkReceiver(this);
        IntentFilter filter = new IntentFilter();
        // 监听网络状态
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // 监听AP状态
        filter.addAction(NetWorkUtil.WIFI_AP_STATE_CHANGED_ACTION);
        registerReceiver(netWorkReceiver, filter);
    }

    /**
     * @author fgsq
     * @comments 发送ui交互消息
     * @date 2024/5/22 11:03
     */
    public void messageSend(Message message) {
        if (mMessenger == null) return;
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            LLog.error("error", e);
        }
    }

    /**
     * @author fgsq
     * @comments cmd-新增设备
     * @date 2024/5/21 15:23
     */
    private void fsAddDevice(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        boolean isIPV6 = dataDec.getBool();
//            if (device.isIPv4()) {
//                String hostAddress = client.getInetAddress().getHostAddress();
//                device.setDevIP(hostAddress);
//            } else {
//                String hostAddress = client.getInetAddress().getHostAddress();
//                device.setDevIP(hostAddress);
//            }
        device.setIPv4(!isIPV6);
        device.setCanRemove(false);
        addDevice(device);
        Device newDevice = null;
        if (isIPV6) {
            newDevice = makeIPv6Device();
        } else {
            for (Device localDevice : localDevices) {
                if (NetWorkUtil.subNet(localDevice.getDevIP(), device.getDevIP(), localDevice.getDevNetMask())) {
                    newDevice = localDevice;
                    break;
                }
            }
        }
        if (newDevice == null) {
            T.s((R.string.add_device_failed_devices_not_on_same_local_network));
            IOUtil.closeIO(out, input, client);
            return;
        }
        byte[] buffer = new byte[1024 * 1024];
        DataEnc dataEnc = makeDataEnc(newDevice, buffer, buffer.length);
        dataEnc.setCmd(LCmd.FS_ADD_DEVICE);
        dataEnc.putBool(isIPV6);
        IOUtil.write(out, dataEnc);
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException ignored) {
        }
        T.s(String.format(getString(R.string.add_device_successful), device.getDevName()));
        IOUtil.closeIO(input, out, client);
        DeviceQrCodeActivity.exitFlag = true;
    }

    /**
     * @author fgsq
     * @comments cmd-接收文件
     * @date 2024/5/21 15:23
     */
    private void fsShareFile(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        byte[] buffer = new byte[1024 * 1024];
        // 接收文件
        // 文件数量
        int count = dataDec.getCount();
        boolean encData = dataDec.getBool();
        dataDec = new DataDec(buffer);
        // 数据包大小
        // 获取设备信息
        List<MessageFileContent> fileContentList = new ArrayList<>();
        Message mMessage;
        for (int i = 0; i < count; i++) {
            // 读取头数据
            if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                throw new RuntimeException("read error");
            // 从头数据中获取数据包大小
            int length = dataDec.getLength();
            // 接收数据包
            if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
                throw new RuntimeException("read error");
            dataDec.setData(buffer, buffer.length);
            // 文件大小
            long fileSize = dataDec.getLong();
            // 文件名称
            String fileName = dataDec.getString();
            int fileType = dataDec.getInt();
            String videoTime = dataDec.getString();
            Log.d(TAG, "filename:" + fileName + " fileSize:" + fileSize);
            if (fileType == LCmd.FILE_IMAGE || fileType == LCmd.FILE_VIEDO) {
                long mediaId = dataDec.getLongDefault(-1);
                MessageMediaContent mediaContent = new MessageMediaContent();
                mediaContent.setId(StringUtils.getUUID());
                mediaContent.setDataVersion(device.getDataVersion());
                mediaContent.setStatus(MessageContent.IN);
                mediaContent.setContent(fileName);
                mediaContent.setLength(fileSize);
                mediaContent.setIndex(i);
                mediaContent.setLeft(true);
                mediaContent.setUserName(device.getDevName());
                mediaContent.setVideo(fileType == LCmd.FILE_VIEDO);
                mediaContent.setVideoTime(videoTime);
                mediaContent.setMediaId(mediaId);
                mediaContent.setDevMode(device.getDevMode());
                fileContentList.add(mediaContent);
            } else if (fileType == LCmd.FILE_FOLDER) {
                // 获取文件数量
                int fileCount = dataDec.getInt();
                MessageFolderContent folderContent = new MessageFolderContent();
                folderContent.setId(StringUtils.getUUID());
                folderContent.setStatus(MessageContent.IN);
                folderContent.setContent(fileName);
                folderContent.setLength(fileSize);
                folderContent.setIndex(i);
                folderContent.setLeft(true);
                folderContent.setUserName(device.getDevName());
                folderContent.setFileCount(fileCount);
                folderContent.setDataVersion(device.getDataVersion());
                folderContent.setDevMode(device.getDevMode());
                fileContentList.add(folderContent);
            } else {
                MessageFileContent fileContent = new MessageFileContent();
                fileContent.setId(StringUtils.getUUID());
                fileContent.setStatus(MessageContent.IN);
                fileContent.setContent(fileName);
                fileContent.setLength(fileSize);
                fileContent.setIndex(i);
                fileContent.setLeft(true);
                fileContent.setUserName(device.getDevName());
                fileContent.setDataVersion(device.getDataVersion());
                fileContent.setDevMode(device.getDevMode());
                fileContentList.add(fileContent);
            }
        }

        RecvFileCallback recvFileCallback = new RecvFileCallback(device, fileContentList, client, input, out, encData) {
            @Override
            public void receviceFile(boolean isAgree) {
                startRecvFile(device, fileContentList, client, input, out, encData, isAgree);
            }
        };

        // 是否弹出确认接收dialog（SP 无记录时默认 true：无需确认）
        boolean isNotRecvDialog = App.getPrefUtil().getBoolean("not_recv_dialog", true);
        if (isNotRecvDialog) {
            recvFileCallback.receviceFile(true);
        } else {
            // 弹出是否接收文件请求弹窗
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_IF_RECIVE_FILES;
            mMessage.arg1 = count;
            mMessage.obj = recvFileCallback;
            messageSend(mMessage);
        }
    }

    /**
     * 分段并行接收：一条连接只负责文件的某一段。
     *
     * 与 fsShareFile 的关键区别：
     * 1. 不进 synchronized (recvBuffer)，也不用静态 recvBuffer/sendBuffer ——
     *    那两处锁和共享缓冲是为了保护单流传输的，4 条并行连接会互相串行化，必须绕开
     * 2. 落盘用 RandomAccessFile.seek(segStart)，且写之前先 setLength 预分配全长 ——
     *    否则 4 个句柄各自按需扩展会互相截断
     * 3. 落盘路径只算一次（avoidDuplication 调 4 次会得到 4 个不同文件名）
     * 4. 确认弹窗只弹一次，整体进度取 4 段字节数之和，收尾只由凑齐 segCount 的那一段做
     */
    private void fsSegShare(DataDec dataDec, Device device, Socket client,
                            InputStream input, OutputStream out) throws Exception {
        boolean encData = dataDec.getBool();
        byte[] buffer = new byte[1024 * 1024];
        DataDec fd = new DataDec(buffer);
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize()) {
            throw new RuntimeException("seg read header error");
        }
        int plen = fd.getLength();
        if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), plen) != plen) {
            throw new RuntimeException("seg read payload error");
        }
        fd.setData(buffer, DataEnc.getHeaderSize() + plen);
        long fileSize = fd.getLong();
        String fileName = fd.getString();
        String segId = fd.getString();
        int segIndex = fd.getInt();
        int segCount = fd.getInt();
        long segStart = fd.getLong();
        long segLen = fd.getLong();
        xfer("SEG-IN segId=" + segId + " idx=" + segIndex + "/" + segCount
                + " start=" + segStart + " len=" + segLen + " fileSize=" + fileSize + " name=" + fileName);

        // 落盘路径只在这里算一次：join 在锁内先算好路径再发布，
        // 4 段拿到的都是同一个 coord.file()，绝不能各算各的 avoidDuplication
        MessageFileContent fileContent = new MessageFileContent();
        fileContent.setId(StringUtils.getUUID());
        fileContent.setStatus(MessageContent.IN);
        fileContent.setContent(fileName);
        fileContent.setLength(fileSize);
        fileContent.setIndex(0);
        fileContent.setLeft(true);
        fileContent.setUserName(device.getDevName());
        fileContent.setDataVersion(device.getDataVersion());
        fileContent.setDevMode(device.getDevMode());
        final SegCoord coord = SegCoord.join(segId, fileSize, segCount, fileContent, () -> {
            String path = FileUtil.createPath(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileName));
            return FileUtil.avoidDuplication(new File(path + "/", fileName));
        });
        if (coord == null) {
            throw new RuntimeException("seg target path not resolved");
        }
        final File outFile = coord.file();
        final boolean first = coord.isFirst();
        xfer("SEG-JOIN segId=" + segId + " idx=" + segIndex + "/" + segCount
                + " first=" + first + " alreadyDone=" + coord.arrived()
                + " path=" + outFile.getPath() + " peer=" + safePeer(client));
        if (first) {
            // 传输期间锁住 Wi-Fi 高性能模式，否则省电策略会把多流吞吐压住
            acquireWifiLock();
        }

        if (first && !App.getPrefUtil().getBoolean("not_recv_dialog", true)) {
            // 只在首段弹确认框，其余段直接等结果
            final SegCoord fcoord = coord;
            final Socket fclient = client;
            final InputStream finput = input;
            final OutputStream fout = out;
            final boolean fenc = encData;
            final File ffile = outFile;
            final int fsegs = segCount;
            final int fsegIdx = segIndex;
            final long fstart = segStart;
            final long flen = segLen;
            RecvFileCallback cb = new RecvFileCallback(device,
                    Collections.singletonList(coord.fileContent), client, input, out, encData) {
                @Override
                public void receviceFile(boolean isAgree) {
                    xfer("SEG-DIALOG segId=" + fcoord.segId + " idx=" + fsegIdx + "/" + fsegs
                            + " agree=" + isAgree + " thread=" + Thread.currentThread().getName());
                    if (!isAgree) {
                        fcoord.markBroken();
                        try {
                            IOUtil.write(fout, LCmd.FS_CLOSE);
                        } catch (IOException ignored) {
                        }
                        // 拒绝也算这条段结束了：凑满 segCount 立即整体收尾删文件，
                        // 否则如果其它段早已写完，这条被拒的段就永远没人触发收尾
                        if (fcoord.arrive()) {
                            finishSeg(fcoord, ffile, false);
                        }
                        return;
                    }
                    // 关键：弹窗回调在主线程，绝不能在主线程收网络数据
                    // （NetworkOnMainThreadException 会把整批段带崩）；段号也不能写死 0，
                    // 弹窗那条连接的 idx 可能是 1、2…（这次实测就是 idx=1）
                    ThreadUtils.runThread(() -> runSeg(fcoord, fsegIdx, fsegs, fstart, flen,
                            fout, finput, fclient, fenc, ffile));
                }
            };
            xfer("SEG-DIALOG segId=" + coord.segId + " idx=" + segIndex + "/" + segCount
                    + " show=true 因 not_recv_dialog=false");
            Message m = Message.obtain();
            m.what = LCmd.SERVICE_IF_RECIVE_FILES;
            m.arg1 = 1;
            m.obj = cb;
            messageSend(m);
            return;
        }
        runSeg(coord, segIndex, segCount, segStart, segLen, out, input, client, encData, outFile);
    }

    private void runSeg(SegCoord coord, int segIndex, int segCount, long segStart, long segLen,
                        OutputStream out, InputStream input, Socket client, boolean encData, File outFile) {
        final long tRun = System.currentTimeMillis();
        xfer("SEG-RUN idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                + " start=" + segStart + " len=" + segLen + " fileSize=" + coord.fileSize
                + " enc=" + encData + " peer=" + safePeer(client) + " path=" + outFile.getPath());
        try {
            if (coord.broken()) {
                xfer("SEG-SKIP idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                        + " reason=ALREADY_BROKEN arrived=" + coord.arrived() + "/" + segCount);
                // 也必须算「已结束」：否则别的段先失败、本段又被跳过时，
                // arrived 永远凑不满，就没人收尾 → 文件残留、进度条不消失
                if (coord.arrive()) {
                    finishSeg(coord, outFile, false);
                }
                return;
            }
            // 预分配全长：并发句柄各自扩展长度会互相截断，必须在写之前定死
            // 弹窗那条连接可能不是 idx=0（实测首段是 idx=1），所以首连接也必须预分配
            if (segIndex == 0 || coord.isFirst()) {
                RandomAccessFile pre = new RandomAccessFile(outFile, "rw");
                try {
                    pre.setLength(coord.fileSize);
                } finally {
                    pre.close();
                }
            }
            // 只让一条连接把「接收中」进度行加进列表，4 条连接共用同一个 coord
            if (coord.presentOnce()) {
                Message show = Message.obtain();
                show.what = LCmd.SERVICE_SHOW_PROGRESS;
                show.obj = Collections.singletonList(coord.fileContent);
                messageSend(show);
            }
            long thatTotal = recvSegFile(client, input, out, segIndex, segCount, segStart,
                    segLen, outFile, coord, encData);
            // arrive 必须在数据阶段结束之后调：谁最后结束谁做收尾，
            // 这样收尾时 4 段的字节都已落盘并计进 received，不会误判失败去删文件
            boolean last = coord.arrive();
            long total = coord.received();
            // 终态字节只看「本段自己收满没有」：非最后一段此时整体 total 还没凑齐，
            // 若用整体判断会给发送端回失败字节，导致 4 条连接里 3 条被误判
            boolean segOk = !coord.broken() && thatTotal == segLen;
            boolean allOk = segOk && total == coord.fileSize;
            String reason;
            if (segOk) {
                reason = "OK";
            } else if (thatTotal != segLen) {
                reason = coord.broken() ? "SHORT+BROKEN" : "SHORT";
            } else {
                reason = "BROKEN";
            }
            xfer("SEG-DONE idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                    + " thatSeg=" + thatTotal + "/" + segLen
                    + " total=" + total + "/" + coord.fileSize
                    + " arrived=" + coord.arrived() + "/" + segCount
                    + " last=" + last + " ok=" + segOk + " reason=" + reason
                    + " ms=" + (System.currentTimeMillis() - tRun));
            // 每段都回一个终态字节，发送端每条连接各自等自己的
            try {
                IOUtil.write(out, segOk ? 2 : 3);
            } catch (IOException e) {
                xfer("SEG-ACK-FAIL idx=" + segIndex + " e=" + e.getMessage());
            }
            if (last) {
                finishSeg(coord, outFile, allOk);
            }
        } catch (Exception e) {
            LLog.error("seg error", e);
            coord.markBroken();
            xfer("SEG-ERROR idx=" + segIndex + " segId=" + coord.segId + " e=" + e);
            // 关键：即使本段中途炸了也必须算「已结束」，
            // 否则凑不满 segCount 就永远没人做收尾，已经落盘的那半截文件会一直残留
            if (coord.arrive()) {
                finishSeg(coord, outFile, false);
            }
        } finally {
            IOUtil.closeIO(input, out, client);
        }
    }

    private void finishSeg(SegCoord coord, File outFile, boolean ok) {
        // 收尾只做一次：异常路径里的 arrive()+finishSeg 可能和最后一段的正常收尾撞上
        if (!coord.finishOnce()) {
            xfer("SEG-FINISH-DUP segId=" + coord.segId + " 忽略，已收尾过");
            return;
        }
        releaseWifiLock();
        MessageFileContent fc = coord.fileContent;
        if (ok) {
            fc.setPath(outFile.getPath());
            fc.setStatus(MessageContent.SUCCESS);
            fc.setStateMessage("接收成功");
        } else {
            fc.setStatus(MessageContent.ERROR);
            fc.setStateMessage("接收失败");
        }
        Message m = Message.obtain();
        m.what = LCmd.SERVICE_CLOSE_PROGRESS;
        m.obj = fc;
        messageSend(m);
        boolean deleted = false;
        if (!ok) {
            // 任何一段缺失/出错都按整体失败处理，删掉半截文件
            if (outFile.exists()) {
                deleted = outFile.delete();
            }
        } else if (Config.SAVE_MESSAGE) {
            checkAndAddChatTime(fc.getId());
            messsageDButil.addListMessage(Collections.singletonList(fc));
        }
        xfer("SEG-FINISH segId=" + coord.segId + " ok=" + ok
                + " received=" + coord.received() + "/" + coord.fileSize
                + " arrived=" + coord.arrived() + "/" + coord.segCount
                + " broken=" + coord.broken()
                + " action=" + (ok ? "KEEP" : (deleted ? "DELETED" : "DELETE_FAILED_OR_GONE"))
                + " path=" + outFile.getPath());
        SegCoord.remove(coord.segId);
    }

    /**
     * 收一段数据并按偏移落盘。
     *
     * 结构与 recvFile 相同的双缓冲流水线，但：
     * 写的是 RandomAccessFile（按 segStart 定位），进度累加到 coord 上（4 段之和），
     * 缓冲是本连接私有的，不碰静态 recvBuffer。
     */
    private long recvSegFile(Socket client, InputStream input, OutputStream out,
                             int segIndex, int segCount, long segStart, long segLen, File outFile,
                             SegCoord coord, boolean dec) throws IOException {
        final int headerLen = DataEnc.getHeaderSize();
        final int chunkSize = sendBuffer.length - headerLen;
        final long t0 = System.currentTimeMillis();
        xfer("SEG-RECV-START idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                + " start=" + segStart + " len=" + segLen + " dec=" + dec
                + " chunk=" + chunkSize + " peer=" + safePeer(client) + " path=" + outFile.getPath());
        final ArrayBlockingQueue<XferChunk> free = new ArrayBlockingQueue<>(2);
        final ArrayBlockingQueue<XferChunk> ready = new ArrayBlockingQueue<>(1);
        for (int i = 0; i < 2; i++) {
            free.add(new XferChunk(chunkSize));
        }
        final XferChunk end = new XferChunk(1);
        final boolean[] readerDone = {false};
        final int[] readerEnd = {0};
        final long[] segOff = {0};
        // 本段私有的头缓冲，绝不使用静态 recvBuffer（4 段会互相踩）
        final byte[] headBuf = new byte[headerLen];
        final DataDec headDec = new DataDec(headBuf);
        final XferStat st = new XferStat();
        final RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
        final long[] filePos = {segStart};
        final long[] thatTotal = {0};
        final int[] lastProgress = {0};

        Thread reader = new Thread(() -> {
            try {
                raf.seek(segStart);
                while (true) {
                    long tA = System.nanoTime();
                    int hdr = IOUtil.read(input, headBuf, 0, headerLen);
                    long tB = System.nanoTime();
                    st.sockNs += tB - tA;
                    if (hdr != headerLen) {
                        readerEnd[0] = 3;
                        ready.put(end);
                        return;
                    }
                    int cmd = headDec.getByteCmd();
                    if (cmd == LCmd.FS_DATA) {
                        int thatLength = headDec.getLength();
                        if (thatLength < 0 || thatLength > chunkSize) {
                            readerEnd[0] = 3;
                            ready.put(end);
                            return;
                        }
                        XferChunk c = free.take();
                        long tC = System.nanoTime();
                        st.freeNs += tC - tB;
                        int gotLen = IOUtil.read(input, c.buf, 0, thatLength);
                        st.sockNs += System.nanoTime() - tC;
                        st.chunk(gotLen);
                        if (gotLen != thatLength) {
                            // 段尾短读：要么对端提前关了，要么对端压根没发完这段
                            xfer("SEG-SHORT idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                                    + " start=" + segStart + " atSeg=" + segOff[0]
                                    + " got=" + gotLen + " want=" + thatLength);
                            readerEnd[0] = 3;
                            ready.put(end);
                            return;
                        }
                        if (dec) {
                            decData(c.buf, gotLen, 0, segOff[0]);
                        }
                        c.len = gotLen;
                        segOff[0] += gotLen;
                        ready.put(c);
                    } else if (cmd == LCmd.FS_END) {
                        readerEnd[0] = 1;
                        ready.put(end);
                        return;
                    } else if (cmd == LCmd.FS_CLOSE && segOff[0] == segLen) {
                        // 发送端计数误判发来 FS_CLOSE，但本段字节已经收满：
                        // 以实际收到的字节为准，按正常收尾处理，不因对端计数 bug 整批报废
                        xfer("SEG-FULL-CLOSE idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                                + " segOff=" + segOff[0] + "/" + segLen
                                + " 对端发了 FS_CLOSE 但字节已收满，按正常收尾");
                        readerEnd[0] = 1;
                        ready.put(end);
                        return;
                    } else {
                        readerEnd[0] = 2;
                        ready.put(end);
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                readerEnd[0] = 3;
            } catch (IOException e) {
                LLog.error("seg reader", e);
                readerEnd[0] = 3;
            } finally {
                readerDone[0] = true;
            }
        }, "seg-recv-" + segStart);
        reader.setDaemon(true);
        reader.start();

        try {
            while (true) {
                long tA = System.nanoTime();
                XferChunk c = ready.poll(200, TimeUnit.MILLISECONDS);
                st.pollNs += System.nanoTime() - tA;
                if (c != null) {
                    if (c == end) {
                        break;
                    }
                    long tB = System.nanoTime();
                    // 关键：4 段各自 seek 到自己的偏移再写，段间乱序到达也正确
                    raf.seek(filePos[0]);
                    raf.write(c.buf, 0, c.len);
                    long tC = System.nanoTime();
                    st.readNs += tC - tB;
                    // 本块长度必须在归还缓冲【之前】抓住：offer 后读线程立即复用并改写 c.len，
                    // filePos 加错会让下一块 seek 到错误偏移（文件直接写坏）
                    long wn = c.len;
                    free.offer(c);
                    filePos[0] += wn;
                    thatTotal[0] += wn;
                    long sum = coord.addReceived(wn);
                    int progress = (int) Math.min(99, sum * 100 / coord.fileSize);
                    if (progress != lastProgress[0]) {
                        lastProgress[0] = progress;
                        coord.fileContent.setProgress(progress);
                        Message pm = Message.obtain();
                        pm.what = LCmd.SERVICE_PROGRESS;
                        pm.obj = coord.fileContent;
                        messageSend(pm);
                    }
                    if (coord.fileContent.isNextStep()) {
                        IOUtil.write(out, LCmd.FS_NEXT);
                    } else {
                        coord.markBroken();
                        IOUtil.write(out, LCmd.FS_BREAK);
                    }
                    // 被拒绝/被取消后立即停下，不再往文件里写半个字节
                    if (coord.broken()) {
                        break;
                    }
                } else {
                    // 没有待写块。读线程都退出了才算真正的收尾，
                    // 但必须先把它可能还留在队列里的块全部写干净（end 永远在最后，只认 end 收工）。
                    if (readerDone[0]) {
                        XferChunk left = ready.poll();
                        while (left != null) {
                            if (left == end) {
                                break;
                            }
                            raf.seek(filePos[0]);
                            raf.write(left.buf, 0, left.len);
                            long ln = left.len;
                            free.offer(left);
                            filePos[0] += ln;
                            thatTotal[0] += ln;
                            coord.addReceived(ln);
                            left = ready.poll();
                        }
                        if (left == end) {
                            break;
                        }
                        // 没有 end：连接在 FS_END 之前就断了（短读/异常），按失败收尾
                        xfer("SEG-NOEND idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                                + " start=" + segStart + " thatSeg=" + thatTotal[0] + "/" + segLen
                                + " readerEnd=" + readerEnd[0]);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LLog.error("seg recv error", e);
            readerEnd[0] = 3;
            xfer("SEG-RECV-EX idx=" + segIndex + " type=WRITE e=" + e);
        } catch (InterruptedException e) {
            LLog.error("seg recv error", e);
            readerEnd[0] = 3;
            xfer("SEG-RECV-EX idx=" + segIndex + " type=INTERRUPTED e=" + e);
        } finally {
            reader.interrupt();
            stopXferReader(reader, input, out, client);
            raf.close();
        }
        // readerEnd：1=对端正常发来 FS_END；2=对端 FS_CLOSE/未知指令；3=短读或异常
        String endName = readerEnd[0] == 1 ? "FS_END" : readerEnd[0] == 2 ? "PEER_CLOSE" : "SHORT_ERR";
        if (readerEnd[0] == 2 || readerEnd[0] == 3) {
            coord.markBroken();
        }
        xfer("SEG-STAT idx=" + segIndex + "/" + segCount + " segId=" + coord.segId
                + " thatSeg=" + thatTotal[0] + "/" + segLen + " end=" + endName + " " + st.tail()
                + xferSpeed(thatTotal[0], System.currentTimeMillis() - t0));
        return thatTotal[0];
    }

    /**
     * @author fgsq
     * @comments cmd-接收消息
     * @date 2024/5/21 15:24
     */
    private void fsMessage(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) {
        try {
            String messageEnc = dataDec.getString();
            String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
            MessageContent content = new MessageContent();
            content.setId(StringUtils.getUUID());
            content.setStatus(MessageContent.SUCCESS);
            content.setUserName(device.getDevName());
            content.setContent(message);
            content.setLeft(true);
            Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
            mMessage.obj = content;
            messageSend(mMessage);
            if (Config.SAVE_MESSAGE) {
                messsageDButil.addMessage(content);
            }
        } catch (Exception e) {
            LLog.error("message decrypt error:", e);
            T.s((R.string.message_decryption_failed));
        } finally {
            IOUtil.closeIO(out, input, client);
        }
    }

    /**
     * @author fgsq
     * @comments cmd-获取媒体同步
     * @date 2024/5/21 15:24
     */
    private void fsGetMediaSync(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        int count = dataDec.getCount();
        byte[] buffer = new byte[1024 * 1024 * 2];
        DataEnc dataEnc = new DataEnc(buffer);
        dataEnc.setCount(count);
        int syncCount = 0;
        for (int i = 0; i < count; i++) {
            long mediaId = dataDec.getLong();
            if (!mediaIdPathDBUtil.isIdExists(mediaId)) {
                syncCount++;
                dataEnc.putLong(mediaId);
            }
        }
        dataEnc.setCount(syncCount);
        IOUtil.write(out, dataEnc);
    }

    /**
     * @author fgsq
     * @comments cmd-获取应用更新
     * @date 2024/5/21 15:24
     */
    private void fsUpdateApps(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        List<ApkInfo> apkFileList = AnyData.apkFileList;
        if (apkFileList == null || apkFileList.isEmpty()) {
            return;
        }
        byte[] buffer = new byte[1024 * 1024 * 2];
        dataDec = new DataDec(buffer);
        // 读取头数据
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            throw new RuntimeException("read error");
        // 从头数据中获取数据包大小
        int length = dataDec.getLength();
        // 接收数据包
        if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
            throw new RuntimeException("read error");
        String apkArrayJson = dataDec.getString();
        JSONArray apkArray = JSON.parseArray(apkArrayJson);
        Iterator<Object> iterator = apkArray.iterator();
        JSONArray jsonArray = new JSONArray();
        while (iterator.hasNext()) {
            JSONObject apkObject = (JSONObject) iterator.next();
            String packageName = apkObject.getString("packageName");
            int versionCode = apkObject.getIntValue("versionCode");
            for (ApkInfo apkInfo : apkFileList) {
                if (apkInfo.getPackageName().equals(packageName) && apkInfo.getVersionCode() > versionCode) {
                    apkObject.put("versionCode", apkInfo.getVersionCode());
                    jsonArray.add(apkObject);
                    iterator.remove();
                    break;
                }
            }
        }
        DataEnc dataEnc = new DataEnc(buffer);
        dataEnc.putString(jsonArray.toJSONString());
        IOUtil.write(out, dataEnc);
        IOUtil.closeIO(out, input, client);
    }

    /**
     * @author fgsq
     * @comments cmd-获取应用
     * @date 2024/5/21 15:24
     */
    private void fsGetApps(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        List<ApkInfo> apkFileList = AnyData.apkFileList;
        if (apkFileList == null || apkFileList.isEmpty()) {
            T.s("APP列表为空");
            return;
        }
        byte[] buffer = new byte[1024 * 1024];
        dataDec = new DataDec(buffer);
        // 读取头数据
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            throw new RuntimeException("read error");
        // 从头数据中获取数据包大小
        int length = dataDec.getLength();
        // 接收数据包
        if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
            throw new RuntimeException("read error");
        String apkArrayJson = dataDec.getString();
        JSONArray apkArray = JSON.parseArray(apkArrayJson);
        List<FileInfo> fileInfos = new ArrayList<>();
        Iterator<Object> iterator = apkArray.iterator();
        while (iterator.hasNext()) {
            JSONObject apkObject = (JSONObject) iterator.next();
            String packageName = apkObject.getString("packageName");
            for (ApkInfo apkInfo : apkFileList) {
                if (apkInfo.getPackageName().equals(packageName)) {
                    fileInfos.add(apkInfo);
                    iterator.remove();
                    break;
                }
            }
        }
        IOUtil.closeIO(out, input, client);
        if (!fileInfos.isEmpty()) {
            Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_GET_APPS;
            mMessage.obj = new Object[]{device, fileInfos};
            messageSend(mMessage);
        }
    }


    /**
     * @author fgsq
     * @comments 处理tcp消息
     * @date 2024/5/21 15:40
     */
    public void handleTcp(Socket client, byte[] magicBytes, InputStream input, OutputStream out) {
        byte[] buffer = new byte[1024 * 1024];
        try {
            // 新版本兼容
            if (magicBytes != null) {
                System.arraycopy(magicBytes, 0, buffer, 0, 4);
                int headSize = DataEnc.getHeaderSize() - 4;
                if (IOUtil.read(input, buffer, 4, headSize) != headSize) return;
            } else {
                if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                    return;
            }
            // 自定义数据包解包工具
            DataDec dataDec = new DataDec(buffer, DataEnc.getHeaderSize());
            int length = dataDec.getLength();
            if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length) return;
            dataDec.setData(buffer, DataEnc.getHeaderSize() + length);
            // 设备端口
            int devPort = dataDec.getInt();
            // 设备ip
            String devIp = dataDec.getString();
            // 设备名
            String devName = dataDec.getString();
            // 设备类型
            int devMode = dataDec.getInt();
            // 设备唯一码
            String uniqueUUid = dataDec.getString();
            int dataVersion = dataDec.getInt();
            // 电量
            int batteryLevel = dataDec.getInt();
            byte chargeStatus = dataDec.getByte();
            int webDeviceCount = dataDec.getInt();
            String address = devIp + ":" + devPort;
            Device device = onLineDevices.get(address);
            if (device == null) {
                device = new Device();
            }
            device.setDevPort(devPort);
            device.setDevIP(devIp);
            device.setDevName(devName);
            device.setUniqueUUid(uniqueUUid);
            device.setDevMode(devMode);
            device.setSetTime(System.currentTimeMillis());
            device.setDataVersion(dataVersion);
            device.setBatteryLevel(batteryLevel);
            device.setChargeStatus(chargeStatus);
            addDevice(device);

            if (webDeviceCount > 0) {
                for (int i = 0; i < webDeviceCount; i++) {
                    int webDevicePort = dataDec.getInt();
                    String webDeviceIp = dataDec.getString();
                    String webDeviceName = dataDec.getString();
                    String addr = webDeviceIp + ":" + webDevicePort;
                    Device debDevice = onLineDevices.get(addr);
                    if (debDevice == null) {
                        debDevice = new Device();
                    }
                    debDevice.setDevPort(webDevicePort);
                    debDevice.setDevIP(webDeviceIp);
                    debDevice.setDevName(webDeviceName);
                    debDevice.setUniqueUUid(webDeviceIp + webDevicePort + webDeviceName);
                    debDevice.setDevMode(Device.WEB);
                    debDevice.setSetTime(System.currentTimeMillis());
                    addDevice(debDevice);
                }
            }
            // 协议处理
            handleVersion(dataDec, device, client, input, out);
        } catch (Exception e) {
            LLog.error("handleTcp error", e);
            IOUtil.closeIO(out, input, client);
        }
    }

    private void handleVersion(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        int cmd = dataDec.getCmd();
        if (cmd == LCmd.FS_ADD_DEVICE) {
            /* 添加设备 */
            fsAddDevice(dataDec, device, client, input, out);
        } else if (cmd == LCmd.FS_SHARE_FILE) {
            /* 接收文件 */
            // 兼容新版本
            /*if (device.getDataVersion() == LVersion.DATA_VERSION_4) {
                new NewVersion().test(dataDec, device, client, input, out);
            } else*/
          /*  if (device.getDataVersion() < LVersion.DATA_VERSION_4) {
                fsShareFile(dataDec, device, client, input, out);
            } else {
                T.s("不支持的版本");
            }*/
            fsShareFile(dataDec, device, client, input, out);
        } else if (cmd == LCmd.FS_SHARE_SEG) {
            /* 分段并行接收：每条连接只写自己那一段 */
            fsSegShare(dataDec, device, client, input, out);
        } else if (cmd == LCmd.FS_MESSAGE) {
            /* 消息 */
            fsMessage(dataDec, device, client, input, out);
        } else if (cmd == LCmd.FS_GET_NO_SYNC_MEDIA) {
            /* 获取没有同步的媒体列表 */
            fsGetMediaSync(dataDec, device, client, input, out);
        } else if (cmd == LCmd.FS_UPDATE_APPS) {
            /* 更新APP */
            fsUpdateApps(dataDec, device, client, input, out);
        } else if (cmd == LCmd.FS_GET_APPS) {
            /* 获取APP */
            fsGetApps(dataDec, device, client, input, out);
        }

    }

    public void updateAppsFromOtherDevice(Device device) {
        ThreadUtils.runThread(() -> {
            try {
                Device fromDevice = getDevice(device);
                List<ApkInfo> apkFileList = AnyData.apkFileList;
                if (apkFileList == null || apkFileList.isEmpty()) {
                    T.s("APP列表为空");
                    return;
                }
                JSONArray apkArray = new JSONArray();
                for (ApkInfo apkInfo : apkFileList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("packageName", apkInfo.getPackageName());
                    jsonObject.put("versionCode", apkInfo.getVersionCode());
                    jsonObject.put("versionName", apkInfo.getVersionName());
                    apkArray.add(jsonObject);
                }
                byte[] bytes = apkArray.toJSONString().getBytes(FileUtil.UTF_8);
                Socket socket = makeSocket(device.getDevIP(), device.getDevPort());
                OutputStream outputStream = socket.getOutputStream();
                DataEnc dataEnc = makeDataEnc(fromDevice, bytes.length + 100);
                dataEnc.setCmd(LCmd.FS_UPDATE_APPS);
                IOUtil.write(outputStream, dataEnc);
                dataEnc.reset();
                dataEnc.putBytes(bytes);
                IOUtil.write(outputStream, dataEnc);
                byte[] data = dataEnc.getData();
                InputStream inputStream = socket.getInputStream();
                DataDec dataDec = new DataDec(data);
                try {
                    // 读取头数据
                    if (IOUtil.read(inputStream, data, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                        return;
                } catch (IOException e) {
                    LLog.error("error", e);
                    IOUtil.closeIO(inputStream, socket.getInputStream(), socket);
                    return;
                }
                // 从头数据中获取数据包大小
                int length = dataDec.getLength();
                try {
                    // 接收数据包
                    if (IOUtil.read(inputStream, data, DataEnc.getHeaderSize(), length) != length)
                        return;
                } catch (IOException e) {
                    LLog.error("error", e);
                    IOUtil.closeIO(outputStream, inputStream, socket);
                    return;
                }
                IOUtil.closeIO(outputStream, inputStream, socket);
                String string = dataDec.getString();
                apkArray = JSON.parseArray(string);
                if (apkArray.isEmpty()) {
                    T.s("没有在" + device.getDevName() + "中找到更新");
                } else {
                    T.s("在" + device.getDevName() + "中找到" + apkArray.size() + "个更新");
                }
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_UPDATE_APPS;
                mMessage.obj = new Object[]{device, apkArray};
                messageSend(mMessage);
            } catch (IOException e) {
                LLog.error("updateAppsFromOtherDevice:", e);
            }
        });
    }

    public void updateApp(Device device, JSONArray jsonArray) {
        ThreadUtils.runThread(() -> {
            byte[] bytes = jsonArray.toString().getBytes();
            Socket socket = null;
            try {
                socket = makeSocket(device.getDevIP(), device.getDevPort());
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();
                Device fromDevice = getDevice(device);
                DataEnc dataEnc = makeDataEnc(fromDevice, bytes.length + 1024);
                dataEnc.setCmd(LCmd.FS_GET_APPS);
                IOUtil.write(outputStream, dataEnc);
                dataEnc.reset();
                dataEnc.putBytes(bytes);
                IOUtil.write(outputStream, dataEnc);
                IOUtil.closeIO(outputStream, inputStream, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // 接收文件缓存
    private final static byte[] recvBuffer = new byte[2 * 1024 * 1024];
    // 发送文件缓存
    private final static byte[] sendBuffer = new byte[2 * 1024 * 1024];

    public long baseRecv(
            Socket client, InputStream input, OutputStream out, DataDec dataDec,
            long fileLength, long mTotalRecv, long totalLength, File outFile,
            MessageFileContent fileContent
    ) {
        return recvFile(client, input, out, dataDec, fileLength, mTotalRecv,
                totalLength, outFile, fileContent, false);
    }

    /**
     * 接收文件：读 socket 线程 + 落盘线程的双缓冲流水线。
     *
     * 这一步是提速的关键。原来是单线程「读一块 → 落盘 → 回执」，落盘期间完全不读 socket，
     * socket 接收缓冲被填满后就会反向阻塞发送端，吞吐被磁盘速度死死卡住。
     * 这里把 socket 读取挪到独立线程并用两块缓冲区交替，让「收包」与「写盘」真正并行，
     * 于是整体吞吐取决于两者中较慢的那个，而不是它们的串行和。
     *
     * 线上字节格式与回执时序都没有变化：
     * 仍然逐块回 FS_NEXT（且仍然是在该块落盘之后才回），仍然以 FS_END/FS_CLOSE 收尾。
     */
    private long recvFile(
            Socket client, InputStream input, OutputStream out, DataDec dataDec,
            long fileLength, long mTotalRecv, long totalLength, File outFile,
            MessageFileContent fileContent, boolean dec
    ) {
        File parentFile = outFile.getParentFile();
        // 目录不存在则创建；并行分段同时 mkdirs 会互相返回 false，
        // 所以创建后再用 isDirectory 判定，避免被别的线程抢先创建而误判失败
        if (parentFile != null && !parentFile.isDirectory()) {
            parentFile.mkdirs();
        }
        if (parentFile == null || !parentFile.isDirectory()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        // 文件输出流
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
        } catch (IOException e) {
            LLog.error("error", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        final int headerLen = DataEnc.getHeaderSize();
        final int chunkSize = recvBuffer.length - headerLen;
        final long t0 = System.currentTimeMillis();
        // 双缓冲：free 是「空缓冲池」，ready 是「已收到待落盘」。
        // ready 深度 1 保证读线程最多领先落盘线程一块，内存占用恒定。
        final ArrayBlockingQueue<XferChunk> free = new ArrayBlockingQueue<>(2);
        final ArrayBlockingQueue<XferChunk> ready = new ArrayBlockingQueue<>(1);
        for (int i = 0; i < 2; i++) {
            free.add(new XferChunk(chunkSize));
        }
        // 独立的结束哨兵对象，避免用 len==0 和「刚放进来的空缓冲」混淆
        final XferChunk end = new XferChunk(1);
        // 0=还在读 1=正常 FS_END 2=FS_CLOSE 被中断 3=短读/连接异常
        final int[] readerEnd = {0};
        final boolean[] readerDone = {false};
        final XferStat st = new XferStat();
        // 解密偏移量 = 该块在文件中的起始位置，用一个跨线程可见的计数器交给读线程累加
        final long[] chunkOffset = {0};
        // 读线程：负责收包 + 解密，绝不碰文件流；解密放在这里可以和落盘完全重叠
        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    // 12 字节头单独读进 dataDec 绑定的 recvBuffer（仅本线程访问），载荷读进分块缓冲
                    long tA = System.nanoTime();
                    int hdr = IOUtil.read(input, recvBuffer, 0, headerLen);
                    long tB = System.nanoTime();
                    st.sockNs += tB - tA;
                    if (hdr != headerLen) {
                        readerEnd[0] = 3;
                        ready.put(end);
                        return;
                    }
                    int cmd = dataDec.getByteCmd();
                    if (cmd == LCmd.FS_DATA) {                    // 数据
                        int thatLength = dataDec.getLength();
                        if (thatLength < 0 || thatLength > chunkSize) {
                            // 头部异常，直接按协议错误收尾
                            readerEnd[0] = 3;
                            ready.put(end);
                            return;
                        }
                        XferChunk c = free.take();
                        long tC = System.nanoTime();
                        st.freeNs += tC - tB;
                        int gotLen = IOUtil.read(input, c.buf, 0, thatLength);
                        st.sockNs += System.nanoTime() - tC;
                        st.chunk(gotLen);
                        if (gotLen != thatLength) {
                            // 短读：这块不投递，落盘线程通过哨兵收尾
                            readerEnd[0] = 3;
                            ready.put(end);
                            return;
                        }
                        if (dec) {
                            decData(c.buf, gotLen, 0, chunkOffset[0]);
                        }
                        c.len = gotLen;
                        c.offset = chunkOffset[0];
                        chunkOffset[0] += gotLen;
                        // ready 深度 1：最多阻塞到落盘线程取走上一块，保证顺序不乱
                        ready.put(c);
                    } else if (cmd == LCmd.FS_END) {              // 传输完毕
                        readerEnd[0] = 1;
                        ready.put(end);
                        return;
                    } else /*if (cmd == LCmd.FS_CLOSE)*/ {        // 被动关闭传输
                        readerEnd[0] = 2;
                        ready.put(end);
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                readerEnd[0] = 3;
            } catch (IOException e) {
                LLog.error("error", e);
                readerEnd[0] = 3;
            } finally {
                readerDone[0] = true;
            }
        }, "xfer-recv-read");
        reader.setDaemon(true);
        reader.start();
        xfer("RECV-START dec=" + dec + " declared=" + fileLength
                + " recvBuf=" + recvBuffer.length + " chunk=" + chunkSize
                + " target=" + outFile.getAbsolutePath());

        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        boolean aborted = false;
        try {
            // 落盘 + 回执，严格按到达顺序处理，保证写出的字节序与发送端一致
            while (true) {
                long tA = System.nanoTime();
                XferChunk c = ready.poll(200, TimeUnit.MILLISECONDS);
                st.pollNs += System.nanoTime() - tA;
                if (c == null) {
                    // 没有待落盘数据：读线程收尾后才会持续出现这种情况
                    if (readerDone[0]) {
                        break;
                    }
                    continue;
                }
                if (c == end) {
                    break;
                }
                long tB = System.nanoTime();
                IOUtil.write(outFileStream, c.buf, 0, c.len);
                long tC = System.nanoTime();
                st.readNs += tC - tB;
                // 先把缓冲还给读线程，让它在下面回执/更新进度时继续收下一块
                // 长度必须在归还前抓住（c.len 会被读线程改写），否则 thatTotal 对不上
                // fileLength → 传输完整却判失败、把文件删掉
                long wn = c.len;
                free.offer(c);
                totalRecv += wn;
                thatTotal += wn;
                long tD = System.nanoTime();
                int progress = (int) (totalRecv * 100 / totalLength);
                if (progress != p) {
                    // 更新视图进度条
                    fileContent.setProgress(progress);
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_PROGRESS;
                    mMessage.obj = fileContent;
                    messageSend(mMessage);
                    p = progress;
                }
                st.progNs += System.nanoTime() - tD;
                long tE = System.nanoTime();
                if (fileContent.isNextStep()) {
                    IOUtil.write(out, LCmd.FS_NEXT);
                } else {
                    // 被取消：回 FS_BREAK，但继续把 socket 排空直到发送端 FS_CLOSE，
                    // 否则发送端会一直阻塞在写 socket 上
                    aborted = true;
                    IOUtil.write(out, LCmd.FS_BREAK);
                }
                st.ackNs += System.nanoTime() - tE;
            }
        } catch (IOException e) {
            LLog.error("error", e);
            thatTotal = 0;
        } catch (InterruptedException e) {
            LLog.error("error", e);
            thatTotal = 0;
        } finally {
            // 提前退出（取消/异常）时读线程可能正阻塞在 free.take()/ready.put()，必须唤醒它
            reader.interrupt();
            stopXferReader(reader, input, out, client);
        }
        // 发送端计数误判可能在字节收满后仍发 FS_CLOSE：以实际字节数为准
        if (readerEnd[0] == 2 && thatTotal == fileLength) {
            xfer("RECV-CLOSE-FULL at=" + thatTotal + "/" + fileLength
                    + " 对端发了 FS_CLOSE 但字节已收满，按成功处理");
            readerEnd[0] = 1;
        }
        if (readerEnd[0] == 2) {
            xfer("RECV-CLOSE at=" + thatTotal);
            Log.d(TAG, "close");
            T.s("接收：" + fileContent.getContent() + " 被中断");
            aborted = true;
        } else if (readerEnd[0] == 1) {
            xfer("RECV-END thatTotal=" + thatTotal + " declared=" + fileLength
                    + xferSpeed(thatTotal, System.currentTimeMillis() - t0));
            xfer("RECV-STAT " + st.tail());
        } else if (readerEnd[0] == 3) {
            xfer("RECV-SHORT at=" + thatTotal + " declared=" + fileLength);
        }
        if (aborted) {
            thatTotal = -3;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }

    public long baseRecvDec(
            Socket client, InputStream input, OutputStream out, DataDec dataDec,
            long fileLength, long mTotalRecv, long totalLength, File outFile,
            MessageFileContent fileContent
    ) {
        return recvFile(client, input, out, dataDec, fileLength, mTotalRecv,
                totalLength, outFile, fileContent, true);
    }


    public void startRecvFile(Device device, List<MessageFileContent> messageFileContents, Socket client, InputStream input, OutputStream out, boolean encData, boolean isAgree) {
        ThreadUtils.runThread(() -> {
            Message mMessage;
            DataEnc dataEnc = new DataEnc();
            // 返回是否接收文件
            try {
                if (isAgree) {
                    dataEnc.setCmd(LCmd.FS_AGREE);
                    IOUtil.write(out, dataEnc);
                } else {
                    dataEnc.setCmd(LCmd.FS_NOT_AGREE);
                    IOUtil.write(out, dataEnc);
                    IOUtil.closeIO(input, out, client);
                    return;
                }
            } catch (IOException e) {
                IOUtil.closeIO(input, out, client);
                LLog.error("error", e);
                return;
            }
            // 通知视图添加文件列表
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = messageFileContents;
            messageSend(mMessage);
            if (Config.SAVE_MESSAGE) {
                checkAndAddChatTime(messageFileContents.get(0).getId());
                messsageDButil.addListMessage(messageFileContents);
            }
//            try {
//                TimeUnit.MILLISECONDS.sleep(500);
//            } catch (InterruptedException ignored) {
//            }
            synchronized (recvBuffer) {
                // 接收文件列表遍历
                for (MessageFileContent fileContent : messageFileContents) {
                    // 接收文件总大小
                    long totalRecv = 0;
                    File file;
                    // 接收文件夹
                    if (fileContent instanceof MessageFolderContent) {
                        MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                        DataDec dataDec = new DataDec(recvBuffer);
                        file = new File(FileUtil.createPath(Config.FILE_SAVE_PATH, Config.FOLDER) + "/" + folderContent.getContent() + "/");
                        for (int j = 0; j < folderContent.getFileCount(); j++) {
                            // 读取文件信息
                            try {
                                if (!IOUtil.read(input, dataDec)) break;
                            } catch (IOException e) {
                                LLog.error("error", e);
                                break;
                            }
                            // 从头数据中获取数据包大小
                            long fileLength = dataDec.getLong();
                            String fileName = dataDec.getString();
                            Log.d(TAG, "接收文件:" + fileName + " 大小:" + fileLength);
                            File outFile = new File(FileUtil.createPath(Config.FILE_SAVE_PATH, Config.FOLDER) + "/", fileName);
                            // 防止重名文件覆盖
                            outFile = FileUtil.avoidDuplication(outFile);
                            long thatTotal = 0;
                            if (encData) {
                                thatTotal = baseRecvDec(
                                        client, input, out, dataDec,
                                        fileLength, totalRecv, folderContent.getLength(),
                                        outFile, folderContent
                                );
                            } else {
                                thatTotal = baseRecv(
                                        client, input, out, dataDec,
                                        fileLength, totalRecv, folderContent.getLength(),
                                        outFile, folderContent
                                );
                            }
                            if (thatTotal == -3) {
                                break;
                            } else if (thatTotal <= 0) {
                                continue;
                            } else {
                                folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                                mMessage = Message.obtain();
                                mMessage.what = LCmd.SERVICE_COMPLETE_COUNT;
                                mMessage.obj = folderContent;
                                messageSend(mMessage);
                            }
                            totalRecv += thatTotal;
                        }
                    } else {
                        // 接收单个文件
                        DataDec dataDec = new DataDec(recvBuffer);
                        String path = FileUtil.createPath(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileContent.getContent()));
                        file = new File(path + "/", fileContent.getContent());
                        file = FileUtil.avoidDuplication(file);
                        if (encData) {
                            totalRecv = baseRecvDec(
                                    client, input, out, dataDec,
                                    fileContent.getLength(), 0, fileContent.getLength(),
                                    file, fileContent
                            );
                        } else {
                            totalRecv = baseRecv(
                                    client, input, out, dataDec,
                                    fileContent.getLength(), 0, fileContent.getLength(),
                                    file, fileContent
                            );
                        }
                    }
                    // 接收成功设置文件路径 失败则删除文件
                    xfer("RECV-JUDGE recv=" + totalRecv + " declared=" + fileContent.getLength() + " file=" + fileContent.getContent());
                    if (totalRecv != fileContent.getLength()) {
                        fileContent.setStatus(MessageContent.ERROR);
                        fileContent.setStateMessage("接收失败");
                    } else {
                        fileContent.setPath(file.getPath());
                        fileContent.setStatus(MessageContent.SUCCESS);
                        fileContent.setStateMessage("接收成功");
                        if (fileContent instanceof MessageMediaContent) {
                            Long mediaId = ((MessageMediaContent) fileContent).getMediaId();
                            if (mediaId > -1) {
                                mediaIdPathDBUtil.addMediaIdPath(mediaId, fileContent.getContent(), fileContent.getPath(), new Date(), true);
                            }
                        }
                    }
                    try {
                        // 响应给发送方继续发送文件
                        IOUtil.write(out, 2);
                    } catch (IOException e) {
                        LLog.error(e);
                    }
                    // 更新视图
                    mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                    mMessage.obj = fileContent;
                    messageSend(mMessage);
                    if (Config.SAVE_MESSAGE) {
                        messsageDButil.updateMessage(fileContent);
                    }
                    LHttpServer.sendMessage(fileContent.getContent(), fileContent.getUserName(), fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), true, false);
                }
                IOUtil.closeIO(input, out, client);
            }
        });
    }

    public List<Device> getDeviceList() {
        List<Device> deviceList;
        if (!onLineDevices.isEmpty()) {
            deviceList = new ArrayList<>(onLineDevices.values());
        } else {
            deviceList = new ArrayList<>();
        }
        return deviceList;
    }

    public void syncMedia() {
        // 在这里处理新图片的逻辑
        List<FileSyncData> fileSyncData = fileSyncDBUtil.queryList();
        List<Device> deviceList = getDeviceList();
        List<Object[]> toDevice = new ArrayList<>();
        for (FileSyncData fileSyncDatum : fileSyncData) {
            for (Device device : deviceList) {
                if (fileSyncDatum.getDeviceId().equals(device.getUniqueUUid())) {
                    toDevice.add(new Object[]{device, fileSyncDatum});
                }
            }
        }
        if (!toDevice.isEmpty()) {
            for (PhotoFolder folder : AnyData.mediaResult.getmFolders()) {
                for (Object[] objects : toDevice) {
                    Device device = (Device) objects[0];
                    FileSyncData syncData = (FileSyncData) objects[1];
                    if (syncData.getFolderPath().equals(folder.getFolderPath())) {
                        startSyncingMedias(device, folder.getImages());
                    }
                }
            }
        }
           /* Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_MEDIA_CHANGES;
            mMessage.obj = null;
            lanService.messageSend(mMessage);*/
    }

    public void startSyncingMedias(Device device, List<MediaInfo> media) {
        ThreadUtils.runThread(() -> {
            Lock lock = StringLockManager.getStringLock(device.getUniqueUUid());
            if (lock.tryLock()) {
                try {
                    Socket socket = null;
                    Device d = null;
                    if (device.isIPv4()) {
                        d = getDevice(device);
                    } else {
                        d = makeIPv6Device();
                    }
                    try {
                        socket = LANService.getInstance().makeSocket(device.getDevIP(), device.getDevPort());
                    } catch (IOException e) {
                        LLog.error(e);
                        return;
                    }
                    DataEnc dataEnc = makeDataEnc(d, 1024 * 1024 * 4);
                    dataEnc.setCmd(LCmd.FS_GET_NO_SYNC_MEDIA);
                    dataEnc.setCount(media.size());
                    for (MediaInfo mediaInfo : media) {
                        dataEnc.putLong(mediaInfo.getMediaId());
//                        Log.d(TAG, "MediaInfo: " + mediaInfo.getMediaId());
                    }
                    if (socket == null || !socket.isConnected()) {
                        T.s(String.format(getString(R.string.connection_to_device_failed), device.getDevName()));
                        return;
                    }
                    List<FileInfo> fileList = new ArrayList<>();
                    InputStream input = null;
                    OutputStream output = null;
                    try {
                        input = socket.getInputStream();
                        output = socket.getOutputStream();
                        TimeUnit.MILLISECONDS.sleep(10);
                        IOUtil.write(output, dataEnc);
                        DataDec dataDec = new DataDec(dataEnc.getData());
                        if (IOUtil.read(input, dataEnc.getData(), 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                            return;
                        int thatLength = dataDec.getLength();
                        if (IOUtil.read(input, dataEnc.getData(), DataEnc.getHeaderSize(), thatLength) != thatLength)
                            return;
                        int count = dataDec.getCount();
                        for (int i = 0; i < count; i++) {
                            long mediaId = dataDec.getLong();
                            MediaInfo mediaInfo = AnyData.mediaResult.getMediaInfoMap().get(mediaId);
                            if (mediaInfo != null) {
                                fileList.add(mediaInfo);
                            }
                            Log.d(TAG, "mediaId: " + mediaId);
                        }
                    } catch (Exception e) {
                        LLog.error(e);
                    } finally {
                        IOUtil.closeIO(input, output, socket);
                    }
                    if (fileList.isEmpty()) {
                        return;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    fileSendSync(d, device, fileList);


                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public void fileSend(Device fromDevice, Device device, List<FileInfo> fileList) {
        ThreadUtils.runThread(() -> fileSendSync(fromDevice, device, fileList));
    }

    /**
     * 文件发送等待列表
     */
    List<SendTask> sendTasks = new Vector<>();

    /**
     * 文件发送服务
     *
     * @param fileList 需要发送的文件列表
     */
    public void fileSendSync(Device fromDevice, Device device, List<FileInfo> fileList) {
        boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
        if (isParallelEligible(device, fileList, encData)) {
            // 整个批次都走 4 段并行：直接为每个文件开 4 条 FS_SHARE_SEG 连接，
            // 绝不打开原始 FS_SHARE_FILE 握手连接。
            // 之前的问题是握手连接先发了 FS_SHARE_FILE + 文件描述，然后被晾在一边，
            // 接收端读完描述后就死等这个连接的数据——多出一个空文件、传完也不结束。
            fileSendParallel(fromDevice, device, fileList);
            return;
        }
        Socket socket = null;
        try {
            socket = makeSocket(device.getDevIP(), device.getDevPort());
        } catch (IOException e) {
            LLog.error(e);
        }
        if (socket == null || !socket.isConnected()) {
            T.s(String.format(getString(R.string.connection_to_device_failed), device.getDevName()));
            return;
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();
            TimeUnit.MILLISECONDS.sleep(10);
            fileSend(fromDevice, socket, input, output, device, fileList);
            ThreadUtils.runThread(() -> {
                Lock sendLock = StringLockManager.getStringLock("sendBuffer");
                try {
                    sendLock.lock();
                    for (SendTask sendTask : sendTasks) {
                        handleSendFile(sendTask.getSocket(), sendTask.getInput(), sendTask.getOut(), sendTask.getDevice(), sendTask.getMessageFileContents(), sendTask.isEncData());
                    }
                    sendTasks.clear();
                } catch (Exception e) {
                    LLog.error(e);
                } finally {
                    sendLock.unlock();
                }
            });
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /**
     * 整个发送批次能不能全部走 4 段并行。
     * 必须「全或无」：只要有一个文件不能走并行，就整批回落到单流，
     * 否则会出现「新连接在传、老握手连接空等」两条并行的脏路径。
     */
    private boolean isParallelEligible(Device device, List<FileInfo> fileList, boolean encData) {
        if (encData) {
            xfer("SEND-MODE parallel=false reason=ENC_ENABLED");
            return false;
        }
        if (device.getDataVersion() < LVersion.DATA_VERSION_5) {
            xfer("SEND-MODE parallel=false reason=PEER_OLD ver=" + device.getDataVersion()
                    + " need>=" + LVersion.DATA_VERSION_5);
            return false;
        }
        if (Config.PARALLEL_SEGS <= 1) {
            xfer("SEND-MODE parallel=false reason=SEGS_DISABLED segs=" + Config.PARALLEL_SEGS);
            return false;
        }
        if (fileList == null || fileList.isEmpty()) {
            xfer("SEND-MODE parallel=false reason=EMPTY_LIST");
            return false;
        }
        for (FileInfo f : fileList) {
            if (!isFileParallelEligible(f)) {
                return false;
            }
        }
        xfer("SEND-MODE parallel=true segs=" + Config.PARALLEL_SEGS + " files=" + fileList.size()
                + " peerVer=" + device.getDataVersion() + " peer=" + device.getDevIP());
        return true;
    }

    /**
     * 单个文件能不能走 4 段并行。
     * 流/uri/媒体走单流：流和 uri 没有可按偏移复用的文件句柄，
     * 媒体走单流是为了避开 MediaInfo 的转换/缩略图逻辑。
     */
    private boolean isFileParallelEligible(FileInfo f) {
        if (f instanceof StreamInfo || f instanceof UriFileInfo || f instanceof MediaInfo) {
            xfer("SEND-MODE file=" + f.getName() + " eligible=false reason=SPECIAL_INFO class="
                    + f.getClass().getSimpleName());
            return false;
        }
        if (!f.isFile()) {
            xfer("SEND-MODE file=" + f.getName() + " eligible=false reason=NOT_A_FILE");
            return false;
        }
        if (f.getLength() < (long) Config.PARALLEL_MIN_SIZE * Config.PARALLEL_SEGS) {
            // 每段 base = 总长/段数，base 必须 >= PARALLEL_MIN_SIZE 才值得切；
            // 否则 sendFileParallel 会返回 -1，那个文件根本发不出去
            xfer("SEND-MODE file=" + f.getName() + " eligible=false reason=TOO_SMALL_FOR_SEGS len="
                    + f.getLength() + " need=" + (long) Config.PARALLEL_MIN_SIZE * Config.PARALLEL_SEGS
                    + " segs=" + Config.PARALLEL_SEGS);
            return false;
        }
        return true;
    }

    /**
     * 整个批次 4 段并行发送。不发 FS_SHARE_FILE 握手帧、不弹接收确认，
     * 每个文件依次在自己的一批 FS_SHARE_SEG 连接上传输。
     */
    private void fileSendParallel(Device fromDevice, Device device, List<FileInfo> fileList) {
        String userName = fromDevice.getDevName();
        List<MessageFileContent> contents = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++) {
            FileInfo f = fileList.get(i);
            MessageFileContent fc = new MessageFileContent();
            fc.setId(StringUtils.getUUID());
            fc.setContent(f.getName());
            fc.setLength(f.getLength());
            fc.setPath(f.getPath());
            fc.setIndex(i);
            fc.setLeft(false);
            fc.setUserName(userName);
            fc.setToUser(device.getDevName());
            contents.add(fc);
        }
        Message m = Message.obtain();
        m.what = LCmd.SERVICE_SHOW_PROGRESS;
        m.obj = contents;
        messageSend(m);
        if (Config.SAVE_MESSAGE) {
            checkAndAddChatTime(contents.get(0).getId());
            messsageDButil.addListMessage(contents);
        }
        for (MessageFileContent fc : contents) {
            long sent = sendFileParallel(device, fc);
            if (sent < 0) {
                // 兜底：资格判定已挡住常见情况，真走到这说明 mDevice 解析失败或对端不支持
                xfer("SEND-PARA file=" + fc.getContent() + " len=" + fc.getLength()
                        + " result=FAILED sent=-1 reason=NOT_APPLICABLE");
            }
            finishSendOne(fc, sent);
        }
    }

    /**
     * 发送文件
     */
    public void fileSend(Device fromDevice, Socket socket, InputStream input, OutputStream out, Device device, List<FileInfo> fileList) {
        List<MessageFileContent> messageFileContents = new ArrayList<>();
        boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
        try {
            Device mDevice = null;
            if (device.isIPv4()) {
                for (Device d : localDevices) {
                    if (!device.isIPv4() || NetWorkUtil.subNet(d.getDevIP(), device.getDevIP(), d.getDevNetMask())) {
                        mDevice = d;
                        break;
                    }
                }
            } else {
                mDevice = makeIPv6Device();
            }
            if (mDevice == null) {
                return;
            }
            DataEnc dataEnc = makeDataEnc(mDevice, 1024 * 1024);
            dataEnc.setCmd(LCmd.FS_SHARE_FILE);
            dataEnc.setCount(fileList.size());
            dataEnc.putBool(encData);
            IOUtil.write(out, dataEnc);
//            String userName = App.getPrefUtil().getString(PreConfig.USER_NAME);
            String userName = fromDevice.getDevName();
            for (int i = 0; i < fileList.size(); i++) {
                FileInfo fileInfo = fileList.get(i);
                dataEnc.reset();
                if (fileInfo instanceof StreamInfo) {
                    // 写入文件大小
                    dataEnc.putLong(fileInfo.getLength());
                    // 写入文件名称
                    dataEnc.putString(fileInfo.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    StreamInfo streamInfo = (StreamInfo) fileInfo;
                    MessageStreamContent streamContent = new MessageStreamContent(streamInfo.getInputStream());
                    streamContent.setId(StringUtils.getUUID());
                    streamContent.setContent(fileInfo.getName());
                    streamContent.setLength(fileInfo.getLength());
                    streamContent.setPath(fileInfo.getPath());
                    streamContent.setIndex(i);
                    streamContent.setLeft(false);
                    streamContent.setUserName(userName);
                    streamContent.setToUser(device.getDevName());
                    messageFileContents.add(streamContent);
                } else if (fileInfo instanceof MediaInfo) { // 媒体
                    MediaInfo mediaInfo = (MediaInfo) fileInfo;
                    MessageMediaContent mediaContent = new MessageMediaContent();
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    if (mediaInfo.isVideo()) {
                        dataEnc.putInt(LCmd.FILE_VIEDO);
                        mediaContent.setVideo(true);
                    } else {
                        dataEnc.putInt(LCmd.FILE_IMAGE);
                        mediaContent.setVideo(false);
                    }
                    String videoTime = ((MediaInfo) fileInfo).getVideoTime();
                    dataEnc.putString(videoTime == null ? "" : videoTime);
                    if (mediaInfo.getMediaId() > -1) {
                        dataEnc.putLong(mediaInfo.getMediaId());
                    }
                    mediaContent.setId(StringUtils.getUUID());
                    mediaContent.setContent(fileInfo.getName());
                    mediaContent.setLength(fileInfo.getLength());
                    mediaContent.setPath(fileInfo.getPath());
                    mediaContent.setIndex(i);
                    mediaContent.setLeft(false);
                    mediaContent.setUserName(userName);
                    mediaContent.setToUser(device.getDevName());
                    mediaContent.setVideoTime(videoTime);
                    messageFileContents.add(mediaContent);
                    // 其他文件
                } else if (fileInfo.isFile() && fileInfo instanceof UriFileInfo) {
                    // 写入文件大小
                    dataEnc.putLong(fileInfo.getLength());
                    // 写入文件名称
                    dataEnc.putString(fileInfo.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    MessageUriContent fileContent = new MessageUriContent(((UriFileInfo) fileInfo).getUri());
                    fileContent.setId(StringUtils.getUUID());
                    fileContent.setContent(fileInfo.getName());
                    fileContent.setLength(fileInfo.getLength());
                    fileContent.setPath(fileInfo.getPath());
                    fileContent.setIndex(i);
                    fileContent.setLeft(false);
                    fileContent.setUserName(userName);
                    fileContent.setToUser(device.getDevName());
                    messageFileContents.add(fileContent);
                    // 文件夹
                } else if (!fileInfo.isFile()) {
                    File file = new File(fileInfo.getPath());
                    if (fileInfo instanceof UriFileInfo) {
                        UriFileInfo uriFileInfo = (UriFileInfo) fileInfo;
                        DocumentFile fileRealNameFromUri = FileUtil.getDocumentFileFromTreeUri(uriFileInfo.getUri());
                        if (!fileRealNameFromUri.exists()) {
                            T.s((R.string.path_not_found));
                            continue;
                        }
                    } else {
                        if (!file.exists()) {
                            T.s((R.string.path_not_found));
                            continue;
                        }
                    }
                    List<FileInfo> fileInfos = new LinkedList<>();
                    long totalSize = 0;
                    T.s((R.string.scanning_files));
                    // 区分Uri路径还是File路径
                    if (fileInfo instanceof UriFileInfo) {
                        UriFileInfo uriFileInfo = (UriFileInfo) fileInfo;
                        totalSize = FileSearchUtils.scanUriPathFileSize(FileUtil.getDocumentFileFromTreeUri(uriFileInfo.getUri()), new File(uriFileInfo.getPath()).getParent(), fileInfos);
                    } else {
                        // 扫描文件并返回扫描到的文件总大小
                        totalSize = FileSearchUtils.scanPathFileSize(file, fileInfos);
                    }
                    T.s((R.string.file_scan_complete));
                    // 写入总文件大小
                    dataEnc.putLong(totalSize);
                    // 写入文件名称
                    dataEnc.putString(file.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FOLDER);
                    dataEnc.putString("");
                    dataEnc.putInt(fileInfos.size());
                    // 创建Message实体类
                    MessageFolderContent folderContent = new MessageFolderContent();
                    folderContent.setId(StringUtils.getUUID());
                    folderContent.setFileCount(fileInfos.size());
                    folderContent.setLength(totalSize);
                    folderContent.setFileInfoList(fileInfos);
                    folderContent.setBasePath(file.getPath());
                    folderContent.setLeft(false);
                    folderContent.setContent(file.getName());
                    folderContent.setIndex(i);
                    folderContent.setUserName(userName);
                    folderContent.setBasePath(fileInfo.getPath());
                    folderContent.setToUser(device.getDevName());
                    folderContent.setPath(file.getPath());
                    messageFileContents.add(folderContent);
                    // Uri分享文件
                } else {
                    // 写入文件大小
                    dataEnc.putLong(fileInfo.getLength());
                    // 写入文件名称
                    dataEnc.putString(fileInfo.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    MessageFileContent fileContent = new MessageFileContent();
                    fileContent.setId(StringUtils.getUUID());
                    fileContent.setContent(fileInfo.getName());
                    fileContent.setLength(fileInfo.getLength());
                    fileContent.setPath(fileInfo.getPath());
                    fileContent.setIndex(i);
                    fileContent.setLeft(false);
                    fileContent.setUserName(userName);
                    fileContent.setToUser(device.getDevName());
                    messageFileContents.add(fileContent);
                }
                IOUtil.write(out, dataEnc);
            }
            IOUtil.read(input, dataEnc.getBuffer(), 0, DataEnc.getHeaderSize());
            DataDec dataDec = new DataDec(dataEnc.getBuffer(), DataEnc.getHeaderSize());
            if (dataDec.getCmd() == LCmd.FS_NOT_AGREE) {
                T.s(device.getDevName() + " " + getString(R.string.cancel_file_reception));
                IOUtil.closeIO(input, out, socket);
                return;
            }
            // 发送显示的文件
            Message mMessage;
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = messageFileContents;
            messageSend(mMessage);
            if (Config.SAVE_MESSAGE) {
                checkAndAddChatTime(messageFileContents.get(0).getId());
                messsageDButil.addListMessage(messageFileContents);
            }
        } catch (IOException e) {
            LLog.error(e);
        }
        Lock sendLock = StringLockManager.getStringLock("sendBuffer");
        boolean tryLock = sendLock.tryLock();
        if (tryLock) {
            try {
                handleSendFile(socket, input, out, device, messageFileContents, encData);
            } catch (Exception e) {
                LLog.error(e);
            } finally {
                sendLock.unlock();
            }
        } else {
            sendTasks.add(new SendTask(socket, input, out, device, messageFileContents, encData));
        }
    }

    private void handleSendFile(
            Socket socket, InputStream input, OutputStream out, Device device,
            List<MessageFileContent> messageFileContents, boolean encData
    ) {
        /*if (device.getDataVersion() == LVersion.DATA_VERSION_4) {
            new NewVersion().writeFiles(
                    socket, input, out, device, messageFileContents, encData);
        } else*/
        // v1~v5 全部走 writeFiles：v5 与老版本的唯一区别是里面会判断能否走 4 段并行，
        // 不支持时自动回落到原来的单流，所以这里不能把 v5 挡在门外
        if (device.getDataVersion() > LVersion.DATA_VERSION_5) {
            T.s("不支持的协议");
        } else {
            writeFiles(socket, input, out, device, messageFileContents, encData);
        }
    }

    private void writeFiles(
            Socket socket, InputStream input, OutputStream out, Device device,
            List<MessageFileContent> messageFileContents, boolean encData
    ) {
        try {
            DataEnc dataEnc = new DataEnc(sendBuffer);
            // 开始发送文件
            for (MessageFileContent fileContent : messageFileContents) {
                dataEnc.reset();
                // OutputStream 是用来判断文件取消的，下面写出文件必须要用它
                long totalSend = 0;
                Class<? extends MessageFileContent> aClass = fileContent.getClass();
                if (aClass.equals(MessageFolderContent.class)) {
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                    // 遍历需要传输的文件
                    for (FileInfo fileInfo : folderContent.getFileInfoList()) {
                        File file = new File(folderContent.getBasePath());
                        String relativePath = fileInfo.getPath().replace(file.getParent(), "");
                        dataEnc.reset();
                        dataEnc.putLong(fileInfo.getLength());
                        dataEnc.putString(relativePath);
                        Log.d(TAG, "发送文件:" + relativePath + " 大小:" + fileInfo.getLength());
                        try {
                            IOUtil.write(out, dataEnc);
                        } catch (IOException e) {
                            LLog.error(e);
                            break;
                        }
                        InputStream fileIs;
                        try {
                            // Uri文件转流
                            if (fileInfo instanceof UriFileInfo) {
                                fileIs = FileUtil.getInputStreamFromUri(((UriFileInfo) fileInfo).getUri());
                            } else {
                                fileIs = new FileInputStream(fileInfo.getPath());
                            }
                        } catch (FileNotFoundException e) {
                            LLog.error(e);
                            continue;
                        }
                        // 文件发送
                        long thatSend = 0;
                        if (encData) {
                            // 文件发送
                            thatSend = baseSendEec(fileContent, fileIs, input, out,
                                    dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        } else {
                            // 文件发送
                            thatSend = baseSend(fileContent, fileIs, input, out,
                                    dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        }
                        if (thatSend == -3) {
                            break;
                        } else if (thatSend <= 0) {
                            continue;
                        } else {
                            folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                            Message mMessage = Message.obtain();
                            mMessage.what = LCmd.SERVICE_COMPLETE_COUNT;
                            mMessage.obj = folderContent;
                            messageSend(mMessage);
                        }
                        totalSend += thatSend;
                    }
                } else {
                    InputStream fileIs;
                    try {
                        if (fileContent instanceof MessageStreamContent) {
                            fileIs = ((MessageStreamContent) fileContent).getInputStream();
                        } else if (fileContent instanceof MessageUriContent) {
                            fileIs = FileUtil.getInputStreamFromUri(((MessageUriContent) fileContent).getUri());
                        } else {
                            // 用 mmap 读：read() 是纯内存拷贝、无系统调用、无阻塞。
                            // 实测本机磁盘 1.5~2.7GB/s，比网络快 20 倍，
                            // 这里磁盘根本不是瓶颈，mmap 的零阻塞特性最稳，不要换 FileInputStream
                            fileIs = new MappedByteBufferInputStream(fileContent.getPath(), sendBuffer.length);
                        }
                    } catch (FileNotFoundException e) {
                        LLog.error(e);
                        T.s("发送文件失败，文件：" + fileContent.getContent() + "不存在");
                        continue;
                    }
                    if (encData) {
                        // 文件接收
                        totalSend += baseSendEec(fileContent, fileIs, input, out, dataEnc, 0, fileContent.getLength(), fileContent.getLength());
                    } else {
                        // 文件接收
                        totalSend += baseSend(fileContent, fileIs, input, out, dataEnc, 0, fileContent.getLength(), fileContent.getLength());
                    }
                }
                try {
                    // 等待接收方响应再继续发送文件：必须读到终态字节，不能只读一个字节
                    int read = waitFileResponse(input);
                    xfer("SEND-RESP resp=" + read + " file=" + fileContent.getContent());
                } catch (IOException e) {
                    LLog.error(e);
                }
                xfer("SEND-JUDGE send=" + totalSend + " declared=" + fileContent.getLength() + " file=" + fileContent.getContent());
                finishSendOne(fileContent, totalSend);
            }
        } catch (Exception e) {
            LLog.error(e);
        } finally {
            IOUtil.closeIO(input, out, socket);
        }
    }

    /**
     * 单个文件传完后的统一收尾：状态、通知栏、关闭进度框、消息入库。
     * 单流和 4 段并行都走这里，保证两条路径的对外表现完全一致。
     */
    private void finishSendOne(MessageFileContent fileContent, long totalSend) {
        if (totalSend != fileContent.getLength()) {
            fileContent.setStatus(MessageContent.ERROR);
            fileContent.setStateMessage("发送失败");
        } else {
            fileContent.setStatus(MessageContent.SUCCESS);
            fileContent.setStateMessage("发送成功");
        }
        LHttpServer.sendMessage(fileContent.getContent(), fileContent.getToUser() + " ← " + getDevName(), fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), false, false);
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
        mMessage.obj = fileContent;
        messageSend(mMessage);
        if (Config.SAVE_MESSAGE) {
            messsageDButil.updateMessage(fileContent);
        }
        Log.d(TAG, "发送成功:" + totalSend);
    }

    /**
     * 把一个文件切成 Config.PARALLEL_SEGS 段，每段一条独立连接并行发送。
     *
     * 分段：base = total/segs，段 i 起点 i*base；前 segs-1 段各 base 字节，
     * 最后一段是余数 (total - (segs-1)*base)。(segs-1)*base + 余数 == total，不重不漏。
     *
     * 每条连接自带 DataEnc 和双缓冲，不共用静态 sendBuffer，也不经过
     * StringLockManager("sendBuffer") —— 那把锁会把 4 段重新串行化。
     *
     * @return 实际发出的字节数；返回 -1 表示不适合并行，调用方应回落到单流
     */
    private long sendFileParallel(Device device, MessageFileContent fileContent) {
        final int segs = Config.PARALLEL_SEGS;
        // 对端必须也支持，否则它不认 FS_SHARE_SEG
        if (segs <= 1 || device.getDataVersion() < LVersion.DATA_VERSION_5) {
            xfer("SEND-PARA skip peer dataVersion=" + device.getDataVersion());
            return -1;
        }
        final long total = fileContent.getLength();
        final long base = total / segs;
        if (base < Config.PARALLEL_MIN_SIZE) {
            xfer("SEND-PARA skip file=" + fileContent.getContent() + " reason=BASE_TOO_SMALL base="
                    + base + " min=" + Config.PARALLEL_MIN_SIZE);
            return -1;
        }
        Device mDevice = null;
        if (device.isIPv4()) {
            for (Device d : localDevices) {
                if (!device.isIPv4() || NetWorkUtil.subNet(d.getDevIP(), device.getDevIP(), d.getDevNetMask())) {
                    mDevice = d;
                    break;
                }
            }
        } else {
            mDevice = makeIPv6Device();
        }
        if (mDevice == null) {
            xfer("SEND-PARA skip file=" + fileContent.getContent()
                    + " reason=NO_LOCAL_DEV peer=" + device.getDevIP());
            return -1;
        }
        final String segId = StringUtils.getUUID();
        final Device md = mDevice;
        final long[] exp = new long[segs];
        final long[] got = new long[segs];
        final AtomicInteger broken = new AtomicInteger();
        final SegSendCtx ctx = new SegSendCtx(fileContent, total);
        xfer("SEND-PARA-START segId=" + segId + " segs=" + segs + " total=" + total
                + " each=" + base + " peer=" + device.getDevIP() + ":" + device.getDevPort()
                + " peerVer=" + device.getDataVersion() + " file=" + fileContent.getContent());
        Thread[] threads = new Thread[segs];
        for (int i = 0; i < segs; i++) {
            final int idx = i;
            final long start = i * base;
            final long len = (i == segs - 1) ? (total - start) : base;
            exp[i] = len;
            threads[i] = new Thread(() -> {
                ctx.active.incrementAndGet();
                try {
                    got[idx] = sendOneSeg(md, device, fileContent, segId, idx, segs, start, len, ctx);
                } catch (Throwable e) {
                    LLog.error("seg send error " + idx, e);
                    xfer("SEND-SEG-EX idx=" + idx + "/" + segs + " segId=" + segId
                            + " start=" + start + " len=" + len + " e=" + e);
                    broken.incrementAndGet();
                } finally {
                    ctx.active.decrementAndGet();
                }
            }, "seg-send-" + idx);
            threads[i].setDaemon(true);
        }
        long t0 = System.currentTimeMillis();
        acquireWifiLock();
        try {
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    broken.incrementAndGet();
                }
            }
        } finally {
            releaseWifiLock();
        }
        long sum = 0;
        boolean ok = broken.get() == 0;
        for (int i = 0; i < segs; i++) {
            sum += got[i];
            if (got[i] != exp[i]) {
                ok = false;
            }
        }
        if (sum != total) {
            ok = false;
        }
        xfer("SEND-PARA segId=" + segId + " segs=" + segs + " sum=" + sum + "/" + total
                + " broken=" + broken.get() + " ok=" + ok
                + " " + xferSpeed(sum, System.currentTimeMillis() - t0));
        return ok ? sum : -1;
    }

    /**
     * 在一条新连接上发一段数据，返回本段实际发出的字节数。
     */
    private long sendOneSeg(Device mDevice, Device device, MessageFileContent fileContent,
                            String segId, int idx, int segs, long start, long len,
                            SegSendCtx ctx) throws IOException {
        // L 能被 segs 整除时最后一段是空段，直接跳过（seek 到文件尾是非法的）
        if (len <= 0) {
            xfer("SEND-SEG-SKIP idx=" + idx + "/" + segs + " reason=EMPTY_SEG len=" + len);
            return 0;
        }
        final long tSeg = System.currentTimeMillis();
        xfer("SEND-SEG-START idx=" + idx + "/" + segs + " segId=" + segId
                + " start=" + start + " len=" + len + " file=" + fileContent.getContent()
                + " peer=" + device.getDevIP() + ":" + device.getDevPort());
        Socket socket = makeSocket(device.getDevIP(), device.getDevPort());
        InputStream in = null;
        OutputStream out = null;
        MappedByteBufferInputStream fis = null;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            DataEnc dataEnc = makeDataEnc(mDevice, 1024 * 1024);
            dataEnc.setCmd(LCmd.FS_SHARE_SEG);
            dataEnc.setCount(1);
            dataEnc.putBool(false);
            IOUtil.write(out, dataEnc);
            // 分段描述帧：整文件大小 + 段号/段数 + 本段起点/长度
            // 接收端靠 segId 把 4 条连接聚成一个文件，靠起点防止写错位置
            DataEnc fd = new DataEnc(new byte[1024 * 1024]);
            fd.putLong(fileContent.getLength());
            fd.putString(fileContent.getContent());
            fd.putString(segId);
            fd.putInt(idx);
            fd.putInt(segs);
            fd.putLong(start);
            fd.putLong(len);
            IOUtil.write(out, fd);

            fis = new MappedByteBufferInputStream(fileContent.getPath(), sendBuffer.length);
            fis.seek(start);
            // 复用已验证的双缓冲流水线，只是把长度限制在这一段
            long sent = sendFile(fileContent, fis, in, out, 0, len, len, false, ctx);
            int resp = waitFileResponse(in);
            String respName = resp == 2 ? "RECV_OK" : resp == 3 ? "RECV_FAIL" : "RESP_" + resp;
            xfer("SEND-SEG-END idx=" + idx + "/" + segs + " segId=" + segId
                    + " sent=" + sent + "/" + len + " resp=" + resp + "(" + respName + ")"
                    + " ms=" + (System.currentTimeMillis() - tSeg)
                    + " " + xferSpeed(sent, System.currentTimeMillis() - tSeg));
            return sent;
        } finally {
            IOUtil.closeIO(in, out, socket);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 4 段并行时共用的进度累加器：每段只报自己那段，整体进度是 4 段字节数之和。
     */
    private final class SegSendCtx {
        final MessageFileContent fileContent;
        final long total;
        final AtomicLong sent = new AtomicLong();
        final AtomicInteger lastProgress = new AtomicInteger(-1);
        // 当前还在发的段数：每秒随 SEND-TP 打出来，用来判断总速变化是
        // 「随时间爬坡」还是「随活跃段数减少而上升」（空口争用）
        final AtomicInteger active = new AtomicInteger();
        final AtomicLong lastTpMs = new AtomicLong(System.currentTimeMillis());
        final AtomicLong lastTpSent = new AtomicLong();

        SegSendCtx(MessageFileContent fileContent, long total) {
            this.fileContent = fileContent;
            this.total = total;
        }

        void add(long n) {
            long done = sent.addAndGet(n);
            int p = (int) Math.min(99, done * 100 / total);
            if (lastProgress.getAndSet(p) != p) {
                fileContent.setProgress(p);
                Message m = Message.obtain();
                m.what = LCmd.SERVICE_PROGRESS;
                m.obj = fileContent;
                messageSend(m);
            }
            // 每秒一行聚合吞吐：一次测试就能画出整条速率曲线
            long now = System.currentTimeMillis();
            long lm = lastTpMs.get();
            if (now - lm >= 1000 && lastTpMs.compareAndSet(lm, now)) {
                long ls = lastTpSent.getAndSet(done);
                long ds = done - ls;
                long ms = now - lm;
                long aggX10 = ds * 10000L / ms / 1048576L;
                xfer("SEND-TP agg=" + aggX10 / 10 + "." + aggX10 % 10 + "MB/s delta="
                        + (ds >> 20) + "MB done=" + (done >> 20) + "/" + (total >> 20)
                        + "MB active=" + active.get());
            }
        }
    }

    public long baseSend(
            MessageFileContent content, InputStream fileIs,
            InputStream mInput, OutputStream mOut, DataEnc dataEnc,
            long mTotalSend, long totalLength, long fileLength
    ) {
        return sendFile(content, fileIs, mInput, mOut, mTotalSend, totalLength, fileLength, false, null);
    }

    public long baseSendEec(
            MessageFileContent content, InputStream fileIs,
            InputStream mInput, OutputStream mOut, DataEnc dataEnc,
            long mTotalSend, long totalLength, long fileLength
    ) {
        return sendFile(content, fileIs, mInput, mOut, mTotalSend, totalLength, fileLength, true, null);
    }

    /**
     * 发送文件：读盘线程 + 写 socket 线程的双缓冲流水线。
     *
     * 原来是单线程「读一块 → 写一块」，读盘期间 socket 完全空转、写 socket 期间磁盘完全空转，
     * 整条流水线的吞吐被较慢的那个阶段卡住。这里把读盘挪到独立线程，用两块缓冲区交替，
     * 让磁盘读取与 socket 写入真正重叠。
     *
     * 线上字节格式完全没有变化，旧版本接收端照常工作：
     * 依然是「12 字节头 + 载荷」一次写出、依然每块回一个 FS_NEXT、依然以 FS_END/FS_CLOSE 收尾。
     *
     * @param segCtx 非空表示本次是在发 4 段中的某一段，此时进度改用「4 段字节数之和」上报，
     *               否则用本连接自己的 mTotalSend/totalLength
     */
    private long sendFile(
            MessageFileContent content, InputStream fileIs,
            InputStream mInput, OutputStream mOut,
            long mTotalSend, long totalLength, long fileLength, boolean enc,
            SegSendCtx segCtx
    ) {
        final int headerLen = DataEnc.getHeaderSize();
        final int chunkSize = sendBuffer.length - headerLen;
        final long t0 = System.currentTimeMillis();
        // 双缓冲：free 是「空缓冲池」，ready 是「已填好待发送」。
        // ready 深度 1 保证读线程最多领先写线程一块，不会把整个文件读进内存。
        final ArrayBlockingQueue<XferChunk> free = new ArrayBlockingQueue<>(2);
        final ArrayBlockingQueue<XferChunk> ready = new ArrayBlockingQueue<>(1);
        for (int i = 0; i < 2; i++) {
            free.add(new XferChunk(chunkSize + headerLen));
        }
        // 独立的结束哨兵对象：写线程靠它判断「读线程已正常读完」，
        // 不用 len==0 表示，否则会和「刚放进来的空缓冲」混淆
        final XferChunk end = new XferChunk(1);
        final boolean[] producerDone = {false};
        final XferStat st = new XferStat();
        // 读盘线程：负责读盘 + 加密，绝不碰 socket；加密放在这里可以和写 socket 完全重叠
        Thread reader = new Thread(() -> {
            try {
                long off = 0;
                while (off < fileLength) {
                    long tA = System.nanoTime();
                    XferChunk c = free.take();
                    long tB = System.nanoTime();
                    st.freeNs += tB - tA;
                    int toRead = (int) Math.min(chunkSize, fileLength - off);
                    int n = fileIs.read(c.buf, headerLen, toRead);
                    st.readNs += System.nanoTime() - tB;
                    st.chunk(n);
                    if (n <= 0) {
                        // 源文件比声明长度短：不再产出数据，写线程按 FS_CLOSE 收尾
                        xfer("SEND-SRC-EOF off=" + off + "/" + fileLength + " n=" + n);
                        return;
                    }
                    if (enc) {
                        encData(c.buf, n, headerLen, off);
                    }
                    c.len = n;
                    c.offset = off;
                    off += n;
                    // ready 深度 1：这里最多阻塞到写线程取走上一块，绝不会堆积
                    ready.put(c);
                }
                // 结束哨兵在所有数据块之后入队，顺序天然有保证
                ready.put(end);
            } catch (InterruptedException | IOException e) {
                // 被取消或源文件出错：写线程会因 producerDone 退出
                xfer("SEND-SRC-EX " + e);
            } finally {
                producerDone[0] = true;
            }
        }, "xfer-send-read");
        reader.setDaemon(true);
        content.setStatus(MessageContent.IN);
        reader.start();
        xfer("SEND-START enc=" + enc + " declared=" + fileLength
                + " sendBuf=" + sendBuffer.length + " chunk=" + chunkSize
                + " src=" + fileIs.getClass().getName());

        int p = 0;
        int probeTick = 0;
        long totalSend = mTotalSend;
        long thatSend = 0;
        // 本连接为什么退出发送循环，直接写进 SEND-BASE，一眼定位失败原因
        String why = "UNKNOWN";
        try {
            while (true) {
                long tA = System.nanoTime();
                XferChunk c = ready.poll(200, TimeUnit.MILLISECONDS);
                st.pollNs += System.nanoTime() - tA;
                if (c == null) {
                    // 没有待发数据：读盘线程收尾后才会持续出现这种情况
                    if (producerDone[0]) {
                        why = "SRC_DONE_WITHOUT_END";
                        break;
                    }
                    continue;
                }
                if (c == end) {
                    why = "END";
                    break;
                }
                c.dataEnc.reset();
                c.dataEnc.setByteCmd(LCmd.FS_DATA);
                // 数据写在头部之后，索引必须落到 头长度 + 本块长度，
                // IOUtil.write 才会把「12 字节头 + 载荷」一次写出去
                c.dataEnc.setDataIndex(c.len);
                long tB = System.nanoTime();
                IOUtil.write(mOut, c.dataEnc);
                long tC = System.nanoTime();
                st.sockNs += tC - tB;
                // 关键：本块长度必须在归还缓冲【之前】抓住。
                // free.offer 之后读线程会立刻复用这块缓冲、把 c.len 改成下一块的长度，
                // 下面再读 c.len 就会被污染（实测 idx=5 少算 1897147 字节 → 误发 FS_CLOSE → 整批失败）
                int justSent = c.len;
                // 先把缓冲还给读线程，让它在下面处理回执/进度时继续预读
                free.offer(c);
                // 连续发送：非阻塞排空接收端逐块回执，仅收到中断指令才停（兼容旧版逐块回执协议）
                if (drainAck(mInput) == LCmd.FS_BREAK) {
                    thatSend = 0;
                    why = "PEER_BREAK";
                    T.s("发送文件：" + content.getContent() + " 被中断");
                    break;
                }
                st.ackNs += System.nanoTime() - tC;
                if (!content.isNextStep()) {
                    thatSend = -3;
                    why = "CANCELLED";
                    break;
                }
                totalSend += justSent;
                thatSend += justSent;
                if (segCtx != null) {
                    // 分段：整体进度 = 4 段已发字节之和，由 4 条连接共享同一个累加器
                    segCtx.add(justSent);
                } else {
                    long tD = System.nanoTime();
                    int progress = (int) (totalSend * 100 / totalLength);
                    if (progress != p) {
                        content.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = content;
                        messageSend(mMessage);
                        p = progress;
                    }
                    st.progNs += System.nanoTime() - tD;
                }
                if (++probeTick % 50 == 0) {
                    TcpProbe tp = probeTcp();
                    xfer("SEND-TCP at=" + thatSend + " sndq=" + tp.sndq
                            + " retr=" + tp.retr + " l=" + tp.local + " r=" + tp.remote);
                }
            }
        } catch (IOException e) {
            LLog.error(e);
            thatSend = 0;
            why = "WRITE_IO:" + e;
        } catch (InterruptedException e) {
            LLog.error(e);
            thatSend = 0;
            why = "INTERRUPTED";
        } finally {
            // 提前退出（取消/异常）时读线程可能正阻塞在 free.take()/ready.put()，必须唤醒它
            reader.interrupt();
            stopXferReader(reader, null, null, null);
        }
        DataEnc tail = new DataEnc(headerLen);
        tail.reset();
        if (thatSend != fileLength) {
            tail.setByteCmd(LCmd.FS_CLOSE);
        } else {
            tail.setByteCmd(LCmd.FS_END);
        }
        try {
            IOUtil.write(mOut, tail);
        } catch (IOException e) {
            LLog.error(e);
        }
        xfer("SEND-BASE why=" + why + " thatSend=" + thatSend + "/" + fileLength
                + " tail=" + (thatSend == fileLength ? "FS_END" : "FS_CLOSE")
                + xferSpeed(thatSend, System.currentTimeMillis() - t0));
        xfer("SEND-STAT " + st.tail());
        return thatSend;
    }

    /**
     * 传输诊断日志：同时进 logcat 和手机存储里的文件，两边内容一致。
     */
    private void xfer(String msg) {
        Log.e("XFER", msg);
    }

    private static final class TcpProbe {
        long sndq = -1;
        long retr = -1;
        String local = "?";
        String remote = "?";
    }

    /**
     * 读 /proc/net/tcp，取「发送队列最大」的那条 ESTABLISHED 连接。
     * 一次传输只有一条连接在传大块数据，所以发送队列最大的那条就是它。
     *
     * sndq 长期贴着 sndBuf 上限 → 发送缓冲写满，对端接收窗口耗尽（对端慢）
     * sndq 长期为 0         → 根本没往内核里塞数据，写线程卡在别处
     * retr 快速增长         → 无线丢包重传，cubic 在退让
     */
    private TcpProbe probeTcp() {
        TcpProbe p = new TcpProbe();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/tcp"));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] f = line.trim().split("\\s+");
                if (f.length < 8 || !"01".equals(f[3])) {
                    continue;
                }
                String[] q = f[4].split(":");
                long sndq = Long.parseLong(q[0], 16);
                long retr = Long.parseLong(f[6]);
                if (sndq > p.sndq) {
                    p.sndq = sndq;
                    p.retr = retr;
                    p.local = f[1];
                    p.remote = f[2];
                }
            }
        } catch (Exception ignored) {
        } finally {
            IOUtil.closeIO(br);
        }
        return p;
    }

    private static final class XferStat {
        long readNs;
        long sockNs;
        long ackNs;
        long pollNs;
        long freeNs;
        long progNs;
        int chunks;
        int zero;
        long chunkSum;
        int min = Integer.MAX_VALUE;
        int max;

        void chunk(int n) {
            if (n <= 0) {
                zero++;
                return;
            }
            chunks++;
            chunkSum += n;
            if (n < min) {
                min = n;
            }
            if (n > max) {
                max = n;
            }
        }

        long avg() {
            return chunks == 0 ? 0 : chunkSum / chunks;
        }

        long ms(long ns) {
            return ns / 1000000L;
        }

        String tail() {
            return "chunks=" + chunks + " avg=" + avg() + " min=" + (chunks == 0 ? 0 : min)
                    + " max=" + max + " zero=" + zero
                    + " | ms read=" + ms(readNs) + " sock=" + ms(sockNs)
                    + " ack=" + ms(ackNs) + " poll=" + ms(pollNs)
                    + " free=" + ms(freeNs) + " prog=" + ms(progNs);
        }
    }

    /**
     * 拼一个吞吐量后缀，方便直接对比不同版本的速度，例如：
     * XFER-SEND 1048576/1048576B 50.2MB/s 20.9s
     */
    private String xferSpeed(long bytes, long costMs) {
        if (costMs <= 0 || bytes <= 0) {
            return "";
        }
        return String.format(" %.1fMB/s %dms",
                bytes / 1024.0 / 1024.0 / (costMs / 1000.0), costMs);
    }

    /**
     * 收尾传输读线程，避免线程泄漏。
     *
     * 读线程可能阻塞在 socket 的 read() 上，而 interrupt() 唤不醒阻塞中的 socket 读，
     * 所以先 join 一小段时间；万一还活着就关掉连接强制它退出。
     * 这一点对接收端是必须的：读线程操作的是静态 recvBuffer，
     * 如果它活过本次接收，就会和下一个文件（共用同一把 recvBuffer 锁）的接收互相踩。
     *
     * @param client 传 null 表示不能关连接（发送端还要复用同一条连接读终态字节）
     */
    private void stopXferReader(Thread reader, InputStream input, OutputStream out, Socket client) {
        try {
            reader.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (reader.isAlive() && client != null) {
            xfer("XFER-READER-STUCK thread=" + reader.getName() + " 强制关闭连接");
            IOUtil.closeIO(input, out, client);
            try {
                reader.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 非阻塞排空接收端回执字节。旧版协议接收端每收一块回一字节（FS_NEXT），
     * 取消时回 FS_BREAK。发送端不再逐块阻塞等待，只在缓冲中发现 FS_BREAK 时中断，
     * 使 TCP 窗口能持续填满，吞吐仅受网卡/磁盘限制（对旧版接收端完全兼容）。
     */
    private int drainAck(InputStream mInput) throws IOException {
        int read = 0;
        while (mInput.available() > 0) {
            read = mInput.read();
            if (read == LCmd.FS_BREAK) {
                return LCmd.FS_BREAK;
            }
        }
        return read;
    }

    /**
     * 等待接收方对当前文件的终态响应。
     * 接收端每收一块数据回一个 FS_NEXT，直到文件全部写入磁盘并关闭输出流之后才回一个终态字节(2)，
     * 取消时回 FS_BREAK。drainAck() 是非阻塞的，快速发送时大部分块回执会滞留在缓冲区里，
     * 所以这里必须跳过所有 FS_NEXT 一直读到终态字节，否则会出现两个问题：
     * 1) 发送端提前进入 finally 关闭 socket，而接收方向仍有未读字节，内核会发 RST 而非 FIN，
     *    对端会丢弃已缓冲数据并在下一次 read() 抛 "Connection reset"，导致接收端统计不足而报"接收失败"，
     *    已完整接收的文件还会被误删；
     * 2) 收发两端的回执流失步，多文件/文件夹传输会错位。
     *
     * @return 终态字节；连接被对端关闭时返回 -1
     */
    private int waitFileResponse(InputStream input) throws IOException {
        int resp;
        do {
            resp = input.read();
            if (resp == -1) {
                return -1;
            }
        } while (resp == LCmd.FS_NEXT);
        return resp;
    }

    /**
     * @return List<Device>
     * @author fgsq
     * @comments 获取自己的设备信息
     * @date 2024/5/22 10:07
     */
    public List<Device> getSelfDevices() {
//        long startTime = System.currentTimeMillis();
        List<NetInfo> netInfoList = NetWorkUtil.getNetInfoList();
        List<Device> mDeviceList = getDevices(netInfoList);
        String ip = "";
        if (!netInfoList.isEmpty()) {
            ip = "http://" + netInfoList.get(0).getIp();
        } else if (!ipv6NetInfoList.isEmpty()) {
            ip = "http://[" + ipv6NetInfoList.get(0).getIp();
        }
        if (!ip.equals(currentIp)) {
            Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_NETWORK_CHANGES;
            mMessage.obj = ip;
            messageSend(mMessage);
            currentIp = ip;
        }
//        LLog.info("mDeviceList: " + mDeviceList.size());
//        LLog.info("time: " + (System.currentTimeMillis() - startTime) + "ms");
        return mDeviceList;
    }

    private static List<Device> getDevices(List<NetInfo> netInfoList) {
        int[] batteryLevel = BatteryUtil.getBatteryLevel(LANService.getInstance());
        List<Device> mDeviceList = new ArrayList<>();
        for (NetInfo netInfo : netInfoList) {
            Device device = new Device();
            device.setDevName(Config.USER_NAME);
            device.setDevIP(netInfo.getIp());
            device.setDevNetMask(netInfo.getMask());
            device.setDevBrotIP(netInfo.getBrodIp());
            device.setDevPort(Config.FILE_SERVER_PORT);
            device.setDevMode(Device.ANDROID);
            device.setDataVersion(Config.DATA_VERSION);
            device.setInterfaceName(netInfo.getInterfaceName());
            device.setBatteryLevel(batteryLevel[0]);
            device.setChargeStatus((byte) batteryLevel[1]);
            mDeviceList.add(device);
//            Log.d(TAG, netInfoList.size() + " " + netInfo.getIp() + " " + netInfo.getBrodIp() + " " + netInfo.getMask());
        }
        return mDeviceList;
    }

    public String getDevName() {
        return App.getPrefUtil().getString(PreConfig.USER_NAME);
    }

    /**
     * @author fgsq
     * @comments 发送当前设备状态
     * @date 2024/4/29 11:32
     */
    public void noticeDeviceStateByIp(Device fromDevice, boolean isBroadcast, boolean isOnLine) {
        String fromIp = isBroadcast ? fromDevice.getDevBrotIP() : fromDevice.getDevIP();
        for (Device toDevice : localDevices) {
            if (NetWorkUtil.subNet(toDevice.getDevIP(), fromIp, toDevice.getDevNetMask())) {
                DataEnc dataEnc = makeDataEnc(toDevice, 1024);
                dataEnc.setCmd(isOnLine ? LCmd.UDP_SET_DEVICES : LCmd.UDP_DEVICES_OFF_LINE);
                try {
                    DatagramSocket datagramSocket;
                    if (isBroadcast) {
                        multicastLock.acquire();
                        datagramSocket = new MulticastSocket();
                        ((MulticastSocket) datagramSocket).setNetworkInterface(NetworkInterface.getByName(fromDevice.getInterfaceName()));
                    } else {
                        datagramSocket = new DatagramSocket();
                    }
                    UDPTools.sendData(datagramSocket, dataEnc, fromIp, Config.UDP_PORT);
                } catch (IOException e) {
                    LLog.error("noticeDeviceOnLineByIp error:", e);
                } finally {
                    if (isBroadcast) {
                        multicastLock.release();
                    }
                }
                dataEnc.reset();
            }
        }
    }

 /*   public void noticeDeviceMuteMedia() {
        for (Device device : onLineDevices.values()) {
            for (Device localDev : localDevices) {
                if (NetWorkUtil.subNet(device.getDevIP(), localDev.getDevIP(), localDev.getDevNetMask())) {
                    DataEnc dataEnc = makeDataEnc(localDev, 2048);
                    dataEnc.setCmd(LCmd.UDP_SEND_MEDIA_MUTE);
                    try {
                        UDPTools.sendData(new DatagramSocket(), dataEnc, device.getDevIP(), Config.UDP_PORT);
                    } catch (IOException e) {
                        LLog.error(e);
                    }
                    break;
                }
            }
        }
    }

    public void noticeDeviceRestoreMedia() {
        for (Device device : onLineDevices.values()) {
            for (Device localDev : localDevices) {
                if (NetWorkUtil.subNet(device.getDevIP(), localDev.getDevIP(), localDev.getDevNetMask())) {
                    DataEnc dataEnc = makeDataEnc(localDev, 2048);
                    dataEnc.setCmd(LCmd.UDP_SEND_MEDIA_RESTORE);
                    try {
                        UDPTools.sendData(new DatagramSocket(), dataEnc, device.getDevIP(), Config.UDP_PORT);
                    } catch (IOException e) {
                        LLog.error(e);
                    }
                    break;
                }
            }
        }
    }*/

    // 文件接收服务
    public void tcpServer() {
        try {
            fileReceive = new ServerSocket(Config.FILE_SERVER_PORT);
        } catch (IOException e) {
            LLog.error(e);
            T.ss((R.string.start_tcp_service_failed));
        }
        ThreadUtils.runThread(() -> {
            while (running) {
                Socket client;
                try {
                    // 等待客户端连接
                    client = fileReceive.accept();
                    tuneSocket(client);
                } catch (IOException e) {
                    LLog.error(e);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                ThreadUtils.runThread(() -> {
                    try {
                        byte[] magicBytes = new byte[4];
                        InputStream is = client.getInputStream();
                        OutputStream out = client.getOutputStream();
                        if (is.read(magicBytes) != 4) return;
                        int magicNum = ByteUtil.bytesToInt(magicBytes, 0);
                        // 版本兼容，魔法数字判断
                        if (magicNum == Config.MAGIC_NUM) {
                            /* 处理自定义协议消息 */
                            handleTcp(client, null, is, out);
                        } else if (Config.WEB_SERVICE) {
                            /* 处理HTTP服务 */
                            String http = new String(magicBytes);
                            String magicStr = http.toUpperCase();
                            if (magicStr.contains("GET") || magicStr.contains("POST")) {
                                httpServer.getHttpServer().newWebClient(client, magicStr, is, out);
                            }
                        }
                    } catch (Exception e) {
                        LLog.error(e);
                        try {
                            // 忘了为什么加延时，不敢删除
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException ignored) {
                        }
                    }
                });
            }
        });
    }


    private DatagramSocket ipGetSocket = null;
//    private MulticastSocket multicastSocket = null;

    // 监听并处理获取客户和设置客户端命令
    public void udpServer() {
        ThreadUtils.runThread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                ipGetSocket = new DatagramSocket(null);
//                ipGetSocket.setReuseAddress(true);
                ipGetSocket.setBroadcast(true);
                ipGetSocket.bind(new InetSocketAddress(Config.UDP_PORT));
//                multicastSocket = new MulticastSocket(Config.UDP_PORT);
//                MulticastSocket multicastSocket = new MulticastSocket();
                // 获取网络接口列表
//                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//                while (interfaces.hasMoreElements()) {
//                    NetworkInterface networkInterface = interfaces.nextElement();
//                    // 检查网络接口是否支持多播
//                    if (networkInterface.isUp() && networkInterface.supportsMulticast()) {
//                        // 绑定到该接口
//                        multicastSocket.setNetworkInterface(networkInterface);
//                        break;
//                    }
//                }
                LLog.debug("runReceive running");
            } catch (IOException e) {
                LLog.error(e);
                T.ss((R.string.start_udp_service_failed));
                return;
            }
            while (running) {
                try {
                    multicastLock.acquire();
                    // receive 会把 packet.length 缩成实际包长，不重置的话后续包会被
                    // 截断到"历史最小包长"，文本消息尾部（AES密文）被切掉导致解密失败
                    packet.setLength(buf.length);
                    ipGetSocket.receive(packet);
//                    multicastSocket.receive(packet);
                } catch (Exception e) {
                    LLog.error("udp receive error:", e);
                } finally {
                    multicastLock.release();
                }
                byte[] data = packet.getData();
                int len = packet.getLength();
                // 去除无效数据包（用实际收到的长度判断，continue 而非 return，避免监听线程退出）
                if (len < 12) {
                    Log.d(TAG, "UDP drop too-short len=" + len);
                    continue;
                }
                int magicNum = ByteUtil.bytesToInt(data, 0);
                if (magicNum == Config.MAGIC_NUM) {
                    ThreadUtils.runThread(() -> {
                        try {
                            byte[] buff = new byte[data.length - 4];
                            System.arraycopy(data, 4, buff, 0, buff.length);
                            handleUdp(new DataDec(buff, len - 4));
                        } catch (Throwable t) {
                            // submit() 会吞掉异常，必须自己兜住打日志
                            LLog.error("handleUdp error:", t);
                        }
                    });
                } else {
                    Log.d(TAG, "UDP magic mismatch len=" + len + " magic=" + magicNum);
                }
            }
        });
    }

    private void handleUdp(DataDec dataDec) {
        dataDec.decAllData();
        int cmd = dataDec.getCmd();
        // 设备端口
        int devPort = dataDec.getInt();
        // 设备ip
        String devIp = dataDec.getString();
        // 设备名
        String devName = dataDec.getString();
        Log.d(TAG, "UDP cmd=" + cmd + " from=" + devIp + ":" + devPort + " name=" + devName);
        // 设备类型
        int devMode = dataDec.getInt();
        // 设备唯一码
        String uniqueUUid = dataDec.getString();
        // dataVersion
        int dataVersion = dataDec.getInt();
        // 电量
        int batteryLevel = dataDec.getInt();
        // 充电状态
        byte chargeStatus = dataDec.getByte();
        int webDeviceCount = dataDec.getInt();
        // 排除自己发送的数据包
        for (Device device : localDevices) {
            if (devIp != null && devIp.equals(device.getDevIP())) {
                return;
            }
        }
        String address = devIp + ":" + devPort;
        Device device = onLineDevices.get(address);
        if (device == null) {
            device = new Device();
        }
        device.setDevPort(devPort);
        device.setDevIP(devIp);
        device.setDevName(devName);
        device.setDevMode(devMode);
        device.setSetTime(System.currentTimeMillis());
        device.setDataVersion(dataVersion);
        device.setUniqueUUid(uniqueUUid);
        device.setBatteryLevel(batteryLevel);
        device.setChargeStatus(chargeStatus);
        addDevice(device);
        if (webDeviceCount > 0) {
            for (int i = 0; i < webDeviceCount; i++) {
                int webDevicePort = dataDec.getInt();
                String webDeviceIp = dataDec.getString();
                String webDeviceName = dataDec.getString();
                String addr = webDeviceIp + ":" + webDevicePort;
                Device debDevice = onLineDevices.get(addr);
                if (debDevice == null) {
                    debDevice = new Device();
                }
                debDevice.setDevPort(webDevicePort);
                debDevice.setDevIP(webDeviceIp);
                debDevice.setDevName(webDeviceName);
                debDevice.setUniqueUUid(webDeviceIp + webDevicePort + webDeviceName);
                debDevice.setDevMode(Device.WEB);
                debDevice.setSetTime(System.currentTimeMillis());
                addDevice(debDevice);
            }
        }
        if (cmd == LCmd.UDP_GET_DEVICES) {
            // 向设备发送自己的数据
            noticeDeviceStateByIp(device, false, true);
        }/* else if (cmd == LCmd.UDP_SET_DEVICES) {
            // 通讯协议版本
//            Log.d("dataVersion", "" + dataVersion);
            // 排除自己发送的数据
        } */ else if (cmd == LCmd.UDP_DEVICES_OFF_LINE) {
            if (device.isCanRemove()) {
                removeDevice(address);
            }
        } else if (cmd == LCmd.UDP_DEVICES_MESSAGE) {
            Log.d(TAG, "UDP_DEVICES_MESSAGE reached, from=" + devIp);
            String messageEnc = dataDec.getString();
            String packageName = dataDec.getString();

            try {
                String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
                // 广播消息
                broadcastMessage(true, devIp, message, devName, packageName);
                // webSocket
                LHttpServer.sendMessage(message, devName, "", 0, 0, "", true, false);
                MessageContent content = new MessageContent();
                content.setId(StringUtils.getUUID());
                content.setStatus(MessageContent.SUCCESS);
                content.setUserName(devName);
                content.setContent(message);
                content.setLeft(true);
                content.setDevMode(device.getDevMode());
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
                mMessage.obj = content;
                messageSend(mMessage);
                if (Config.SAVE_MESSAGE) {
                    checkAndAddChatTime(content.getId());
                    messsageDButil.addMessage(content);
                }
                Log.d(TAG, message);
            } catch (Exception e) {
                LLog.error(e);
                T.s((R.string.message_decryption_failed));
            }
        } else if (cmd == LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD) {
            String messageEnc = dataDec.getString();
            String packageName = dataDec.getString();
            try {
                String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
                broadcastMessage(true, devIp, message, devName, packageName);
                LHttpServer.sendMessage(message, devName, "", 0, 0, "", true, true);
                MessageContent content = new MessageContent();
                content.setId(StringUtils.getUUID());
                content.setStatus(MessageContent.SUCCESS);
                content.setUserName(devName);
                content.setContent(message);
                content.setLeft(true);
                content.setDevMode(device.getDevMode());
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
                mMessage.obj = content;
                messageSend(mMessage);
                if (Config.SAVE_MESSAGE) {
                    checkAndAddChatTime(content.getId());
                    messsageDButil.addMessage(content);
                }
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("text", message));
                T.s((R.string.copy_text_to_clipboard_successful));
            } catch (Exception e) {
                LLog.error(e);
                T.s((R.string.message_decryption_failed));
            }
        } else if (cmd == LCmd.UDP_SEND_MAP) {
            // GPS 位置消息：body 为明文 JSON（type/latitude/longitude/address/name）
            String gpsJson = dataDec.getString();
            try {
                MessageGPSContent gpsContent = new MessageGPSContent(gpsJson);
                gpsContent.setId(StringUtils.getUUID());
                gpsContent.setStatus(MessageContent.SUCCESS);
                gpsContent.setUserName(devName);
                gpsContent.setLeft(true);
                gpsContent.setDevMode(device.getDevMode());
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
                mMessage.obj = gpsContent;
                messageSend(mMessage);
                if (Config.SAVE_MESSAGE) {
                    checkAndAddChatTime(gpsContent.getId());
                    messsageDButil.addMessage(gpsContent);
                }
            } catch (Exception e) {
                LLog.error("parse GPS message failed", e);
            }
        } /*else if (cmd == LCmd.UDP_SEND_MEDIA_MUTE) {
            if (App.getPrefUtil().getBoolean(PreConfig.RECEIVE_MUTE)) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    Log.d(TAG, "设置静音");
                }
            }
        } else if (cmd == LCmd.UDP_SEND_MEDIA_RESTORE) {
            if (App.getPrefUtil().getBoolean(PreConfig.RECEIVE_MUTE)) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, //音量类型
                            systemVolume, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    Log.d(TAG, "取消静音");
                }
            }
        }*/
    }


    private void broadcastMessage(boolean isClip, String devIp, String message, String devName, String packageName) {
        try {
            boolean broadcastMessage = App.getPrefUtil().getBoolean(PreConfig.BROADCAST_MESSASGE);
            if (broadcastMessage && !StringUtils.isEmpty(packageName) && !StringUtils.isEmpty(message)) {
                // 广播消息
                Intent newIntent = new Intent(LANShareReceiver.LANSHARE_BORADCAST_EXPORT_INERFACE);
                newIntent.setPackage(packageName);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ip", devIp);
                jsonObject.put("msg", message);
                jsonObject.put("isClip", isClip);
                jsonObject.put("devName", devName);
                newIntent.putExtra("content", jsonObject.toString());
                sendBroadcast(newIntent);
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LLog.debug("onDestroy 退出");
        running = false;
        ThreadUtils.runThread(() -> {
            for (Device device : localDevices) {
                noticeDeviceStateByIp(device, true, false);
            }
        });
        if (Config.MEDIA_SYNC) {
            imageObserver.unregisterObserver();
        }
        IOUtil.closeIO(ipGetSocket, fileReceive);
//        IOUtil.closeIO(multicastSocket, fileRecive);
        unregisterReceiver(netWorkReceiver);
    }

    /**
     * 拉满网卡的关键 socket 参数：关闭 Nagle（小包回执不延迟）。
     * 缓冲给 1MB（内核×2≈2MB）：866Mbps×5ms≈540KB，2MB 已是 ~4×BDP，单流能跑满。
     * 之前给 8MB 是错的——16 条流合计 256MB，首秒把 237MB 洪注入径，
     * 全路径队列冲爆 → 丢包恢复 + RTT 膨胀，开局 9 秒爬不上线速。
     */
    private void tuneSocket(Socket socket) throws IOException {
        socket.setTcpNoDelay(true);
        socket.setSendBufferSize(1024 * 1024);
        socket.setReceiveBufferSize(1024 * 1024);
        xfer("TUNE local=" + socket.getLocalSocketAddress()
                + " remote=" + socket.getRemoteSocketAddress()
                + " sndBufReq=" + (1024 * 1024)
                + " sndBufAct=" + socket.getSendBufferSize()
                + " rcvBufAct=" + socket.getReceiveBufferSize());
    }

    public Socket makeSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        tuneSocket(socket);
        socket.connect(new InetSocketAddress(host, port), 4000);
//        socket.setSoTimeout(4000);
        xfer("PEER local=" + socket.getLocalSocketAddress()
                + " remote=" + socket.getRemoteSocketAddress()
                + " sndBuf=" + socket.getSendBufferSize()
                + " rcvBuf=" + socket.getReceiveBufferSize());
        OutputStream outputStream = socket.getOutputStream();
        byte[] magicBytes = ByteUtil.intToBytes(Config.MAGIC_NUM);
        // 数据协议版本
        outputStream.write(magicBytes);
        outputStream.flush();
        return socket;
    }

    public Device makeIPv6Device() {
        Device device = new Device();
        device.setDevName(App.getPrefUtil().getString(PreConfig.USER_NAME));
        if (ipv6NetInfoList == null || ipv6NetInfoList.isEmpty()) {
            device.setDevIP("");
        } else {
            device.setDevIP(ipv6NetInfoList.get(0).getIp());
        }
        device.setDevNetMask("");
        device.setDevBrotIP("");
        device.setDevPort(Config.FILE_SERVER_PORT);
        device.setDevMode(Device.ANDROID);
        device.setDataVersion(Config.DATA_VERSION);
        return device;
    }

    public Socket makeSocket(InetAddress host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        tuneSocket(socket);
        OutputStream outputStream = socket.getOutputStream();
        byte[] magicBytes = ByteUtil.intToBytes(Config.MAGIC_NUM);
        // 数据协议版本
        outputStream.write(magicBytes);
        outputStream.flush();
        return socket;
    }

    public DataEnc makeDataEnc(Device device, int size) {
        DataEnc dataEnc = new DataEnc(size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        dataEnc.putString(Config.uniqueUUid);
        dataEnc.putInt(Config.DATA_VERSION);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        // webDeviceCount 必须始终写入（没有 web 设备时写 0 占位）：
        // 接收端无条件读这个字段，缺了它会把后面的消息长度前缀误读成
        // webDeviceCount，错位读取后消息内容被吃掉，表现为收到空白文本。
        dataEnc.putInt(onLineWebDevices.size());
        if (!onLineWebDevices.isEmpty()) {
            Set<Map.Entry<String, Device>> entries = onLineWebDevices.entrySet();
            for (Map.Entry<String, Device> entry : entries) {
                Device value = entry.getValue();
                dataEnc.putInt(value.getDevPort());
                dataEnc.putString(value.getDevIP());
                dataEnc.putString(value.getDevName());
            }
        }
        return dataEnc;
    }

    public DataEnc makeDataEnc(Device device, byte[] buff, int size) {
        DataEnc dataEnc = new DataEnc(buff, size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        dataEnc.putString(Config.uniqueUUid);
        dataEnc.putInt(Config.DATA_VERSION);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        // webDeviceCount 必须始终写入（没有 web 设备时写 0 占位）：
        // 接收端无条件读这个字段，缺了它会把后面的消息长度前缀误读成
        // webDeviceCount，错位读取后消息内容被吃掉，表现为收到空白文本。
        dataEnc.putInt(onLineWebDevices.size());
        if (!onLineWebDevices.isEmpty()) {
            Set<Map.Entry<String, Device>> entries = onLineWebDevices.entrySet();
            for (Map.Entry<String, Device> entry : entries) {
                Device value = entry.getValue();
                dataEnc.putInt(value.getDevPort());
                dataEnc.putString(value.getDevIP());
                dataEnc.putString(value.getDevName());
            }
        }
        return dataEnc;
    }

    public Device getDevice(Device device) {
        for (Device fromDevice : localDevices) {
            if (!device.isIPv4() || NetWorkUtil.subNet(device.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                return fromDevice;
            }
        }
        return null;
    }

    // 局域网扫描设备
    public void scanDevices() {
        LLog.debug("scanDevices：" + running);
        ThreadUtils.runThread(() -> {
            while (running) {
                localDevices.clear();
                localDevices.addAll(getSelfDevices());
                for (Device selfDevice : localDevices) {
                    try {
                        if (!StringUtils.isEmpty(selfDevice.getDevBrotIP())) {
                            DataEnc dataEnc = makeDataEnc(selfDevice, 1024);
                            dataEnc.setCmd(LCmd.UDP_GET_DEVICES);
                            UDPTools.sendData(new DatagramSocket(), dataEnc, selfDevice.getDevBrotIP(), Config.UDP_PORT);
                        }
                        long currentTime = System.currentTimeMillis();
                        // 移除没有心跳的设备
                        for (String key : onLineDevices.keySet()) {
                            Device device = onLineDevices.get(key);
                            if (device != null && device.isCanRemove()) {
                                long setTime = device.getSetTime();
                                long timeOut = currentTime - setTime;
                                // 超过20秒没有心跳的设备直接移除
                                if (timeOut > (1000 * 20)) {
                                    removeDevice(key);
                                }
                            }
                        }
                    } catch (IOException e) {
                        LLog.error(e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    public void broadcastMessage(Device toDevice, String message, boolean isClip) {
        broadcastMessage(toDevice, message, isClip, "", true);
    }

    public void broadcastMessage(Device toDevice, String message, boolean isClip, String packageName) {
        broadcastMessage(toDevice, message, isClip, packageName, true);
    }

    /**
     * 发送消息
     *
     * @param toDevice    发送给哪个设备，为空则发送给所有设备
     * @param msg         发送的内容
     * @param isClip      写入剪切板状态
     * @param packageName 广播消息时指定接收广播的应用包名（高版本安卓必须指定包名）
     * @param shareWS     是否分享消息内容给websocket网页聊天界面
     */
// 广播发送信息
    public void broadcastMessage(Device toDevice, String msg, boolean isClip, String packageName, boolean shareWS) {
        String message;
        try {
            message = AESUtils.encrypt(msg, Config.MESSAGE_KEY);
        } catch (Exception e) {
            LLog.error("encrypt msg error", e);
            T.s((R.string.message_encryption_failed));
            return;
        }
        Log.d(TAG, "broadcastMessage:" + message);
        if (shareWS) {
            if (toDevice == null) {
                ThreadUtils.runThread(() -> {
                    LHttpServer.sendMessage(
                            msg, getString(R.string.all_devices) + " ← " + getDevName(),
                            "", 0, 0, "", false, isClip
                    );
                });
            } else if (toDevice.getDevMode() == Device.WEB) {
                ThreadUtils.runThread(() -> {
                    LHttpServer.sendMessage(
                            toDevice.getWebSocketServer(), msg,
                            toDevice.getDevName() + " ← " + getDevName(), "",
                            0, toDevice.getDevMode(), "", false, isClip
                    );
                });
                return;
            }
        }

        if (msg.length() > 700) {
            T.s(R.string.text_length_exceeds_limit);
            ThreadUtils.runThread(() -> {
                if (toDevice == null) {
                    for (Device dev : onLineDevices.values()) {
                        if (dev.isIPv4()) {
                            for (Device fromDevice : localDevices) {
                                if (NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                    DataEnc dataEnc = makeDataEnc(fromDevice, 1024 + message.getBytes().length);
                                    dataEnc.setCmd(LCmd.FS_MESSAGE);
                                    dataEnc.putString(message);
                                    dataEnc.putString(packageName);
                                    Socket socket = null;
                                    try {
                                        socket = makeSocket(dev.getDevIP(), dev.getDevPort());
                                        OutputStream outputStream = socket.getOutputStream();
                                        IOUtil.write(outputStream, dataEnc);
                                        IOUtil.closeIO(outputStream, socket);
                                    } catch (Exception e) {
                                        LLog.error(e);
                                    }
                                    break;
                                }

                            }
                        } else {
                            DataEnc dataEnc = makeDataEnc(makeIPv6Device(), 1024 + message.getBytes().length);
                            dataEnc.setCmd(LCmd.FS_MESSAGE);
                            dataEnc.putString(message);
                            dataEnc.putString(packageName);
                            Socket socket = null;
                            try {
                                socket = makeSocket(dev.getDevIP(), dev.getDevPort());
                                OutputStream outputStream = socket.getOutputStream();
                                IOUtil.write(outputStream, dataEnc);
                                IOUtil.closeIO(outputStream, socket);
                            } catch (Exception e) {
                                LLog.error(e);
                            }
                        }
                    }
                } else {
                    if (toDevice.isIPv4()) {
                        for (Device fromDevice : localDevices) {
                            // 指定了设备单独发送
                            if (!toDevice.isIPv4() || NetWorkUtil.subNet(toDevice.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                DataEnc dataEnc = makeDataEnc(fromDevice, 1024 + message.getBytes().length);
                                dataEnc.setCmd(LCmd.FS_MESSAGE);
                                dataEnc.putString(message);
                                dataEnc.putString(packageName);
                                Socket socket = null;
                                try {
                                    socket = makeSocket(toDevice.getDevIP(), toDevice.getDevPort());
                                    OutputStream outputStream = socket.getOutputStream();
                                    IOUtil.write(outputStream, dataEnc);
                                    IOUtil.closeIO(outputStream, socket);
                                } catch (Exception e) {
                                    LLog.error(e);
                                }
                                break;
                            }
                        }
                    } else {
                        DataEnc dataEnc = makeDataEnc(makeIPv6Device(), 1024 + message.getBytes().length);
                        dataEnc.setCmd(LCmd.FS_MESSAGE);
                        dataEnc.putString(message);
                        dataEnc.putString(packageName);
                        Socket socket = null;
                        try {
                            socket = makeSocket(toDevice.getDevIP(), toDevice.getDevPort());
                            OutputStream outputStream = socket.getOutputStream();
                            IOUtil.write(outputStream, dataEnc);
                            IOUtil.closeIO(outputStream, socket);
                        } catch (Exception e) {
                            LLog.error(e);
                        }
                    }
                }

            });
            return;
        }
        ThreadUtils.runThread(() -> {
            if (toDevice == null) {
                for (Device dev : onLineDevices.values()) {
                    if (dev.isIPv4()) {
                        for (Device fromDevice : localDevices) {
                            if (!dev.isIPv4() || NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                DataEnc dataEnc = makeDataEnc(fromDevice, 1024 + message.getBytes().length);
                                dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                                dataEnc.putString(message);
                                dataEnc.putString(packageName);
                                try {
                                    UDPTools.sendData(new DatagramSocket(), dataEnc, dev.getDevIP(), Config.UDP_PORT);
                                } catch (IOException e) {
                                    LLog.error(e);
                                }
                                Log.d(TAG, "onLineDevices:" + dev.getDevName() + " " + dev.getDevIP());
                                break;
                            }
                        }
                    } else {
                        DataEnc dataEnc = makeDataEnc(makeIPv6Device(), 1024 + message.getBytes().length);
                        dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                        dataEnc.putString(message);
                        dataEnc.putString(packageName);
                        try {
                            UDPTools.sendData(new DatagramSocket(), dataEnc, dev.getDevIP(), Config.UDP_PORT);
                        } catch (IOException e) {
                            LLog.error(e);
                        }
                    }
                }
            } else {
                if (toDevice.isIPv4()) {
                    for (Device fromDevice : localDevices) {
                        if (NetWorkUtil.subNet(toDevice.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                            DataEnc dataEnc = makeDataEnc(fromDevice, 1024 + message.getBytes().length);
                            dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                            dataEnc.putString(message);
                            dataEnc.putString(packageName);
                            try {
                                UDPTools.sendData(new DatagramSocket(), dataEnc, toDevice.getDevIP(), Config.UDP_PORT);
                            } catch (IOException e) {
                                LLog.error(e);
                            }
                            break;
                        }
                    }
                } else {
                    DataEnc dataEnc = makeDataEnc(makeIPv6Device(), 1024 + message.getBytes().length);
                    dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                    dataEnc.putString(message);
                    dataEnc.putString(packageName);
                    try {
                        UDPTools.sendData(new DatagramSocket(), dataEnc, toDevice.getDevIP(), Config.UDP_PORT);
                    } catch (IOException e) {
                        LLog.error(e);
                    }
                }
            }
        });

    }

    public void checkAndAddChatTime(String bindId) {
        if (DateUtils.isFiveMinutesAgo(Config.lastMessageTime)) {
            MessageTimeContent messageTimeContent = new MessageTimeContent();
            messageTimeContent.setId(StringUtils.getUUID());
            messageTimeContent.setBindId(bindId);
            if (Config.SAVE_MESSAGE) {
                messsageDButil.addMessage(messageTimeContent);
            }
            Config.lastMessageTime = messageTimeContent.getCreateTime().getTime();
        }
    }
    /**
     * 发送 GPS 位置消息（UDP 明文 JSON，不加密，命令 UDP_SEND_MAP=1011）
     *
     * @param toDevice  目标设备，null 则发给所有在线设备
     * @param type      地图类型（MessageGPSContent.TYPE_AMAP 等）
     * @param latitude  纬度
     * @param longitude 经度
     * @param address   地址文本
     * @param name      地点名称
     */
    public void sendGPSMessage(Device toDevice, int type, String latitude, String longitude, String address, String name) {
        MessageGPSContent gps = new MessageGPSContent(type, latitude, longitude, address, name);
        final String gpsJson = gps.toString();
        ThreadUtils.runThread(() -> {
            if (toDevice == null) {
                for (Device dev : onLineDevices.values()) {
                    sendGPSToDevice(dev, gpsJson);
                }
            } else {
                sendGPSToDevice(toDevice, gpsJson);
            }
        });
        // 本地聊天界面也显示一条
        gps.setId(StringUtils.getUUID());
        gps.setStatus(MessageContent.SUCCESS);
        gps.setUserName(getDevName());
        gps.setToUser(toDevice != null ? toDevice.getDevName() : getString(R.string.all_devices));
        gps.setLeft(false);
        gps.setDevMode(Device.ANDROID);
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
        mMessage.obj = gps;
        messageSend(mMessage);
        if (Config.SAVE_MESSAGE) {
            checkAndAddChatTime(gps.getId());
            messsageDButil.addMessage(gps);
        }
    }

    private void sendGPSToDevice(Device dev, String gpsJson) {
        try {
            if (dev.isIPv4()) {
                for (Device fromDevice : localDevices) {
                    if (NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                        DataEnc dataEnc = makeDataEnc(fromDevice, 1024 + gpsJson.getBytes().length);
                        dataEnc.setCmd(LCmd.UDP_SEND_MAP);
                        dataEnc.putString(gpsJson);
                        UDPTools.sendData(new DatagramSocket(), dataEnc, dev.getDevIP(), Config.UDP_PORT);
                        break;
                    }
                }
            } else {
                DataEnc dataEnc = makeDataEnc(makeIPv6Device(), 1024 + gpsJson.getBytes().length);
                dataEnc.setCmd(LCmd.UDP_SEND_MAP);
                dataEnc.putString(gpsJson);
                UDPTools.sendData(new DatagramSocket(), dataEnc, dev.getDevIP(), Config.UDP_PORT);
            }
        } catch (Exception e) {
            LLog.error("send GPS failed", e);
        }
    }

}


