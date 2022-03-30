package com.fgsqw.lanshare.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseService;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.FileSource;
import com.fgsqw.lanshare.pojo.MediaInfo;
import com.fgsqw.lanshare.pojo.MessageContent;
import com.fgsqw.lanshare.pojo.MessageFileContent;
import com.fgsqw.lanshare.pojo.MessageFolderContent;
import com.fgsqw.lanshare.pojo.MessageMediaContent;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.pojo.mOutputStream;
import com.fgsqw.lanshare.pojo.mSocket;
import com.fgsqw.lanshare.receiver.NetWorkReceiver;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.DataDec;
import com.fgsqw.lanshare.utils.DataEnc;
import com.fgsqw.lanshare.utils.FIleSerachUtils;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.NetWorkUtil;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.UDPTools;
import com.fgsqw.lanshare.utils.ViewUpdate;
import com.fgsqw.lanshare.utils.mUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LANService extends BaseService {

    public static final String TAG = "LANService";

    public static LANService service;
    ServerSocket fileRecive;
    public Map<String, Device> devices = new ConcurrentHashMap<>();
    Messenger mMessenger;
    Device mDevice;
    String locAddrIndex = "255.255.255.255";
    NetWorkReceiver netWorkReceiver;
    PrefUtil prefUtil;
    private boolean isRun = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Object messenger = intent.getExtras().get("messenger");
            if (messenger != null) {
                mMessenger = (Messenger) messenger;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        // 自定义配置文件工具类
        prefUtil = new PrefUtil(this);
        // Service保活
        mUtil.showNotification(this, getString(R.string.app_name), "服务正在运行...");
        // 初始化数据
        initData();
        // 监听wifi
        receiver();
        // UDP广播监听
        runRecive();
        // 文件接收监听
        fileServer();
        // 通告局域网所有设备我已上线
        ViewUpdate.runThread(() -> noticeDeviceOnLineByIp(locAddrIndex));
        // UDP 广播扫描设备
        scannDevice();

    }


    // 初始化数据
    public void initData() {
        mDevice = getDevice();
        locAddrIndex = NetWorkUtil.getLocAddrIndex(mDevice.getDevIP()) + "255";
    }


    // 网络状态监听
    public void receiver() {
        netWorkReceiver = new NetWorkReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkReceiver, filter);
    }

    // 发送消息
    public void messageSend(Message message) {
        if (mMessenger == null) return;
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // 监听接收文件信息
    public void handelFile(Socket client) {

        InputStream input = null;
        OutputStream out = null;

        try {
            input = client.getInputStream();
            out = client.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[1024 * 1024];

        try {
            if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // 自定义数据包解包工具
        DataDec dataDec = new DataDec(buffer, DataEnc.getHeaderSize());

        int cmd = dataDec.getCmd();

        // 接收文件
        if (cmd == mCmd.FS_SHARE_FILE) {
            // 文件数量
            int count = dataDec.getCount();
            // 数据包大小
            int length = dataDec.getLength();

            try {
                if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length) return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            dataDec.setData(buffer, DataEnc.getHeaderSize() + length);

            // 获取设备信息
            int port = dataDec.getInt();
            String ip = dataDec.getString();
            String name = dataDec.getString();

            Device device = devices.get(ip + ":" + port);
            if (device == null) {
                device = new Device(name, ip, port);
            }

            List<MessageFileContent> fileContentList = new ArrayList<>();
            Message mMessage;
            for (int i = 0; i < count; i++) {
                try {
                    // 读取头数据
                    if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                        return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                // 从头数据中获取数据包大小
                length = dataDec.getLength();

                try {
                    // 接收数据包
                    if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
                        return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                dataDec.setData(buffer, buffer.length);
                // 文件大小
                long fileSize = dataDec.getLong();
                // 文件名称
                String fileName = dataDec.getString();
                int fileType = dataDec.getInt();
                String videoTime = dataDec.getString();

                Log.d(TAG, "filename:" + fileName + " fileSize:" + fileSize);

                if (fileType == mCmd.FILE_IMAGE || fileType == mCmd.FILE_VIEDO) {
                    MessageMediaContent mediaContent = new MessageMediaContent();
                    mediaContent.setContent(fileName);
                    mediaContent.setLength(fileSize);
                    mediaContent.setSocket(new mSocket(input, out));
                    mediaContent.setIndex(i);
                    mediaContent.setLeft(true);
                    mediaContent.setUserName(name);
                    mediaContent.setVideo(fileType == mCmd.FILE_VIEDO);
                    mediaContent.setVideoTime(videoTime);

                    if (device.getDevMode() == Device.ANDROID) {
                        mediaContent.setHeader(R.drawable.ic_phone);
                    } else if (device.getDevMode() == Device.WIN) {
                        mediaContent.setHeader(R.drawable.ic_win);
                    }

                    fileContentList.add(mediaContent);
                } else if (fileType == mCmd.FILE_FOLDER) {
                    // 获取文件数量
                    int fileCount = dataDec.getInt();

                    MessageFolderContent folderContent = new MessageFolderContent();
                    folderContent.setContent(fileName);
                    folderContent.setLength(fileSize);
                    folderContent.setSocket(new mSocket(input, out));
                    folderContent.setIndex(i);
                    folderContent.setLeft(true);
                    folderContent.setUserName(name);
                    folderContent.setFileCount(fileCount);

                    if (device.getDevMode() == Device.ANDROID) {
                        folderContent.setHeader(R.drawable.ic_phone);
                    } else if (device.getDevMode() == Device.WIN) {
                        folderContent.setHeader(R.drawable.ic_win);
                    }

                    fileContentList.add(folderContent);
                } else {
                    MessageFileContent fileContent = new MessageFileContent();
                    fileContent.setContent(fileName);
                    fileContent.setLength(fileSize);
                    fileContent.setSocket(new mSocket(input, out));
                    fileContent.setIndex(i);
                    fileContent.setLeft(true);
                    fileContent.setUserName(name);
                    if (device.getDevMode() == Device.ANDROID) {
                        fileContent.setHeader(R.drawable.ic_phone);
                    } else if (device.getDevMode() == Device.WIN) {
                        fileContent.setHeader(R.drawable.ic_win);
                    }
                    fileContentList.add(fileContent);
                }


            }
            Object[] objects = {device, fileContentList, client, input, out};
            // 是否弹出确认接收dialog
            boolean isNotRecvDialog = prefUtil.getBoolean("not_recv_dialog");
            if (isNotRecvDialog) {
                startRecvFile(objects, true);
            } else {
                // 弹出是否接收文件请求弹窗
                mMessage = Message.obtain();
                mMessage.what = mCmd.SERVICE_IF_RECIVE_FILES;
                mMessage.arg1 = count;
                mMessage.obj = objects;
                messageSend(mMessage);
            }
        }
    }

    // 接收文件缓存
    final byte[] recvBuffer = new byte[2 * 1024 * 1024];
    // 发送文件缓存
    final byte[] sendBuffer = new byte[2 * 1024 * 1024];

    public long baseRecv(Object[] objects, DataDec dataDec,
                         long fileLength, long mTotalRecv,
                         long totalLength, File outFile,
                         MessageFileContent fileContent) {

        Socket client = (Socket) objects[2];
        InputStream input = (InputStream) objects[3];
        OutputStream out = (OutputStream) objects[4];

        File parentFile = outFile.getParentFile();
        // 文件夹存在创建文件夹
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        String name = outFile.getName();

        // 防止重名文件覆盖
        if (outFile.exists()) {
            for (int s = 1; s < 65535; s++) {
                String str;
                if (name.contains(".")) {
                    String prefix = name.substring(0, name.lastIndexOf(".")) + "(" + s + ")";
                    String suffix = name.substring(name.lastIndexOf("."));
                    str = prefix + suffix;
                } else {
                    str = name + "(" + s + ")";
                }
                outFile = new File(parentFile, str);
                if (!outFile.exists()) {
                    break;
                }
            }
        }
        // 文件输出流
        OutputStream outFileStream = null;
        try {
            outFileStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            IOUtil.closeIO(input, out, client);
            return -1;
        }

        InputStream mInput = fileContent.getSocket().getInputStream();
        dataDec.reset();

        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            // 接收文件
            while (true) {
                // 接收文件信息
                if (IOUtil.read(mInput, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                    break;
                int cmd = dataDec.getByteCmd();
                if (cmd == mCmd.FS_DATA) {          // 数据
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(mInput, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength)
                        break;
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progeress = (int) (totalRecv * 100.0F / totalLength);

                    if (progeress != p) {
                        // 更新视图进度条
                        fileContent.setProgress(progeress);
                        Message mMessage = Message.obtain();
                        mMessage.what = mCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        messageSend(mMessage);
                        p = progeress;
                    }
                } else if (cmd == mCmd.FS_END) {    // 传输完毕
                    break;
                } else {  // 被动关闭传输
                    Log.d(TAG, "close");
                    thatTotal = 0;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);

        if (thatTotal != fileLength) {
            outFile.delete();
        }

        return thatTotal;
    }


    public void startRecvFile(Object[] objects, boolean isAgree) {
        ViewUpdate.runThread(() -> {
            List<MessageFileContent> messageFileContents = (List<MessageFileContent>) objects[1];
            Socket client = (Socket) objects[2];
            InputStream input = (InputStream) objects[3];
            OutputStream out = (OutputStream) objects[4];

            Message mMessage;

            DataEnc dataEnc = new DataEnc(0);
            // 返回是否接收文件
            try {
                if (isAgree) {
                    dataEnc.setCmd(mCmd.FS_AGREE);
                    IOUtil.write(out, dataEnc.getData(), dataEnc.getDataLen());
                } else {
                    dataEnc.setCmd(mCmd.FS_NOT_AGREE);
                    IOUtil.write(out, dataEnc.getData(), dataEnc.getDataLen());
                    IOUtil.closeIO(input, out, client);
                    return;
                }

            } catch (IOException e) {
                e.printStackTrace();
                IOUtil.closeIO(input, out, client);
                return;
            }

            // 通知视图添加文件列表
            mMessage = Message.obtain();
            mMessage.what = mCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = messageFileContents;
            messageSend(mMessage);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (recvBuffer) {
                // 接收文件列表遍历
                for (int i = 0; i < messageFileContents.size(); i++) {
                    MessageFileContent fileContent = messageFileContents.get(i);
                    long totalRecv = 0;
                    File file;
                    if (fileContent instanceof MessageFolderContent) {
                        MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                        DataDec dataDec = new DataDec(recvBuffer);
                        file = new File(Config.FILE_SAVE_PATH + Config.FORDER + "/" + folderContent.getContent() + "/");

                        for (int j = 0; j < folderContent.getFileCount(); j++) {
                            // 读取文件信息
                            try {
                                // 读取头数据
                                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                                    break;
                                int dataLength = dataDec.getLength();
                                if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), dataLength) != dataLength)
                                    break;
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                            // 从头数据中获取数据包大小

                            long fileLength = dataDec.getLong();
                            String fileName = dataDec.getString();

                            Log.d(TAG, "接收文件:" + fileName + " 大小:" + fileLength);


                            File outFile = new File(Config.FILE_SAVE_PATH + Config.FORDER + "/", fileName);

                            long thatTotal = baseRecv(
                                    objects,
                                    dataDec,
                                    fileLength,
                                    totalRecv,
                                    folderContent.getLength(),
                                    outFile,
                                    folderContent
                            );

                            // 传输过程中有问题直接break
                            if (thatTotal <= 0) {
                                break;
                            } else {
                                folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                                mMessage = Message.obtain();
                                mMessage.what = mCmd.SERVICE_COMPLETE_COUNT;
                                mMessage.obj = folderContent;
                                messageSend(mMessage);
                            }

                            totalRecv += thatTotal;
                        }

                    } else {
                        DataDec dataDec = new DataDec(recvBuffer);
                        file = new File(Config.FILE_SAVE_PATH + getNameType(fileContent.getContent()) + "/", fileContent.getContent());
                        totalRecv = baseRecv(
                                objects,
                                dataDec,
                                fileContent.getLength(),
                                0,
                                fileContent.getLength(),
                                file,
                                fileContent
                        );


                    }


                    // 接收成功设置文件路径 失败则删除文件
                    if (totalRecv != fileContent.getLength()) {
                        fileContent.setSuccess(false);
                        fileContent.setStateMessage("接收失败");
                    } else {
                        fileContent.setPath(file.getPath());
                        fileContent.setSuccess(true);
                        fileContent.setStateMessage("接收成功");
                    }


                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 更新视图
                    mMessage = Message.obtain();
                    mMessage.what = mCmd.SERVICE_CLOSE_PROGRESS;
                    mMessage.obj = fileContent;
                    messageSend(mMessage);

                }
                IOUtil.closeIO(input, out, client);
            }
        });
    }


    /**
     * 通过后缀名判断文件类型
     */
    public String getNameType(String name) {
        if (name.contains(".")) {
            String suffix = name.substring(name.lastIndexOf(".") + 1);
            for (String[] strings : Config.fileType) {
                if (strings[0].equalsIgnoreCase(suffix)) {
                    return strings[1];
                }
            }
        }
        return "其他";
    }


    /**
     * 接收文件时发送取消接收某一文件命令
     */
    public void sendCloseCmd(MessageFileContent fileContent, OutputStream out) {
        ViewUpdate.runThread(() -> {
            DataEnc dataEnc = new DataEnc();
            dataEnc.setByteCmd(mCmd.FS_CLOSE);
            dataEnc.setCount(fileContent.getIndex());
            try {
                IOUtil.write(out, dataEnc.getData(), dataEnc.getDataLen());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 发送文件同时接收指令 此线程用于接收端取消接收文件
     */
    public void startRecvCmd(List<MessageFileContent> fileContentList, InputStream input) {
        ViewUpdate.runThread(() -> {
            DataDec dataDec = new DataDec();
            try {
                while (isRun) {
                    int read = IOUtil.read(input, dataDec.getData(), 0, dataDec.getByteLen());
                    if (read > 0) {
                        if (read == dataDec.getByteLen()) {
                            int cmd = dataDec.getByteCmd();
                            if (cmd == mCmd.FS_CLOSE) {
                                int index = dataDec.getCount();
                                for (MessageFileContent messageFileContent : fileContentList) {
                                    if (messageFileContent.getIndex() == index) {
                                        messageFileContent.getSocket().mClose();
                                    }
                                }
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 文件发送服务
     *
     * @param fileList 需要发送的文件列表
     */

    public void fileSend(Device device, List<FileInfo> fileList) {
        ViewUpdate.runThread(() -> {
            Socket socket = null;
            try {
                socket = new Socket(device.getDevIP(), device.getDevPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (socket == null || !socket.isConnected()) {
                T.s("连接" + device.getDevName() + "失败");
                return;
            }

            InputStream input = null;
            OutputStream output = null;

            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
                fileSend(input, output, device, fileList);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IOUtil.closeIO(input, output, socket);
            }

        });

    }


    public void fileSend(InputStream input, OutputStream out, Device device, List<FileInfo> fileList) {
        List<MessageFileContent> messageFileContents = new ArrayList<>();
        synchronized (sendBuffer) {
            try {
                DataEnc dataEnc = new DataEnc(sendBuffer);
                dataEnc.setCmd(mCmd.FS_SHARE_FILE);
                dataEnc.setCount(fileList.size());
                dataEnc.putInt(mDevice.getDevPort());
                dataEnc.putString(mDevice.getDevIP());
                dataEnc.putString(prefUtil.getString(PreConfig.USER_NAME));

                IOUtil.write(out, dataEnc.getData(), dataEnc.getDataLen());

                String userName = prefUtil.getString(PreConfig.USER_NAME);

                for (int i = 0; i < fileList.size(); i++) {
                    FileInfo fileInfo = fileList.get(i);
                    dataEnc.reset();
                    // 媒体
                    if (fileInfo instanceof MediaInfo) {
                        MediaInfo mediaInfo = (MediaInfo) fileInfo;
                        MessageMediaContent mediaContent = new MessageMediaContent();

                        dataEnc.putLong(fileInfo.getLength());
                        dataEnc.putString(fileInfo.getName());

                        if (mediaInfo.isVideo()) {
                            dataEnc.putInt(mCmd.FILE_VIEDO);
                            mediaContent.setVideo(true);
                        } else {
                            dataEnc.putInt(mCmd.FILE_IMAGE);
                            mediaContent.setVideo(false);
                        }

                        String videoTime = ((MediaInfo) fileInfo).getVideoTime();
                        dataEnc.putString(videoTime == null ? "" : videoTime);

                        mediaContent.setContent(fileInfo.getName());
                        mediaContent.setLength(fileInfo.getLength());
                        mediaContent.setPath(fileInfo.getPath());
                        mediaContent.setSocket(new mSocket(input, out));
                        mediaContent.setIndex(i);
                        mediaContent.setLeft(false);
                        mediaContent.setUserName(userName);
                        mediaContent.setToUser(device.getDevName());
                        mediaContent.setVideoTime(videoTime);
                        messageFileContents.add(mediaContent);

                        // 文件夹
                    } else if (fileInfo instanceof FileSource && !((FileSource) fileInfo).isFile()) {
                        FileSource fileSource = (FileSource) fileInfo;

                        File file = new File(fileSource.getPath());
                        if (!file.exists()) {
                            T.s("路径不存在");
                            continue;
                        }
                        List<FileInfo> fileInfos = new LinkedList<>();
                        // 扫描文件并返回扫描到的文件总大小
                        long totalSize = FIleSerachUtils.scanPathFiles(file, fileInfos);

                        // 写入总文件大小
                        dataEnc.putLong(totalSize);
                        // 写入文件名称
                        dataEnc.putString(file.getName());
                        // 写入文件类型
                        dataEnc.putInt(mCmd.FILE_FOLDER);
                        dataEnc.putString("");
                        dataEnc.putInt(fileInfos.size());
                        // 创建Message实体类
                        MessageFolderContent folderContent = new MessageFolderContent();
                        folderContent.setFileCount(fileInfos.size());
                        folderContent.setLength(totalSize);
                        folderContent.setFileInfoList(fileInfos);
                        folderContent.setBasePath(file.getPath());
                        folderContent.setLeft(false);
                        folderContent.setContent(file.getName());
                        folderContent.setSocket(new mSocket(input, out));
                        folderContent.setIndex(i);
                        folderContent.setUserName(userName);
                        folderContent.setBasePath(fileSource.getPath());
                        folderContent.setToUser(device.getDevName());

                        messageFileContents.add(folderContent);

                        // 其他文件
                    } else {
                        // 写入文件大小
                        dataEnc.putLong(fileInfo.getLength());
                        // 写入文件名称
                        dataEnc.putString(fileInfo.getName());
                        // 写入文件类型
                        dataEnc.putInt(mCmd.FILE_FILE);
                        dataEnc.putString("");

                        MessageFileContent fileContent = new MessageFileContent();
                        fileContent.setContent(fileInfo.getName());
                        fileContent.setLength(fileInfo.getLength());
                        fileContent.setPath(fileInfo.getPath());
                        fileContent.setSocket(new mSocket(input, out));
                        fileContent.setIndex(i);
                        fileContent.setLeft(false);
                        fileContent.setUserName(userName);
                        fileContent.setToUser(device.getDevName());
                        messageFileContents.add(fileContent);
                    }
                    IOUtil.write(out, dataEnc.getData(), dataEnc.getDataLen());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // 读取对方是否同意接收文件
            try {
                IOUtil.read(input, sendBuffer, 0, DataEnc.getHeaderSize());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            DataDec dataDec = new DataDec(sendBuffer, DataEnc.getHeaderSize());
            if (dataDec.getCmd() == mCmd.FS_NOT_AGREE) {
                T.s(device.getDevName() + " 取消接收文件");
                return;
            }

            // 发送显示的文件
            Message mMessage;
            mMessage = Message.obtain();
            mMessage.what = mCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = messageFileContents;
            messageSend(mMessage);

            // 取消文件接收监听
            startRecvCmd(messageFileContents, input);

            for (MessageFileContent fileContent : messageFileContents) {
                DataEnc dataEnc = new DataEnc(sendBuffer);
                // mOutputStream 是用来判断文件取消的，下面写出文件必须要用它
                mOutputStream mOut = fileContent.getSocket().getOutputStream();
                long totalSend = 0;
                if (fileContent.getClass().equals(MessageFolderContent.class)) {
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;

                    // 遍历需要传输的文件
                    for (FileInfo fileInfo : folderContent.getFileInfoList()) {
                        File file = new File(folderContent.getBasePath());
                        String fileName = fileInfo.getPath().replace(file.getParent(), "");

                        dataEnc.reset();
                        dataEnc.putLong(fileInfo.getLength());
                        dataEnc.putString(fileName);

                        Log.d(TAG, "发送文件:" + fileName + " 大小:" + fileInfo.getLength());

                        try {
                            IOUtil.write(mOut, dataEnc.getData(), dataEnc.getDataLen());
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }


                        // 文件接收
                        long thatSend = baseSend(
                                fileContent,
                                fileInfo.getPath(),
                                dataEnc,
                                totalSend,
                                folderContent.getLength(),
                                fileInfo.getLength()
                        );

                        if (thatSend <= 0) {
                            break;
                        } else {
                            folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                            mMessage = Message.obtain();
                            mMessage.what = mCmd.SERVICE_COMPLETE_COUNT;
                            mMessage.obj = folderContent;
                            messageSend(mMessage);
                        }

                        totalSend += thatSend;
                    }
                } else {
                    // 文件接收
                    totalSend += baseSend(
                            fileContent,
                            fileContent.getPath(),
                            dataEnc,
                            0,
                            fileContent.getLength(),
                            fileContent.getLength()
                    );
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (totalSend != fileContent.getLength()) {
                    fileContent.setSuccess(false);
                    fileContent.setStateMessage("发送失败");
                } else {
                    fileContent.setSuccess(true);
                    fileContent.setStateMessage("发送成功");
                }

                mMessage = Message.obtain();
                mMessage.what = mCmd.SERVICE_CLOSE_PROGRESS;
                mMessage.obj = fileContent;
                messageSend(mMessage);
                Log.d(TAG, "发送成功:" + totalSend);
            }
        }

    }


    public long baseSend(MessageFileContent folderContent, String filePath, DataEnc dataEnc, long mTotalSend, long totalLength, long fileLength) {
        mOutputStream mOut = folderContent.getSocket().getOutputStream();
        InputStream fileIs;
        try {
            fileIs = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            return -1;
        }
        // 发送文件
//        Log.d(TAG, "发送文件:" + filePath + " 大小:" + fileLength);
        int ten = 0;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(mCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        try {
            // 读取时偏移掉头的位置
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), sendBuffer.length - DataEnc.getHeaderSize())) != -1) {
                dataEnc.setDataIndex(ten);
                IOUtil.write(mOut, dataEnc.getData(), dataEnc.getDataLen());
                totalSend += ten;
                thatSend += ten;
                int prngeress = (int) (totalSend * 100.0F / totalLength);
                if (prngeress != p) {
                    folderContent.setProgress(prngeress);
                    Message mMessage = Message.obtain();
                    mMessage.what = mCmd.SERVICE_PROGRESS;
                    mMessage.obj = folderContent;
                    messageSend(mMessage);
                    p = prngeress;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            thatSend = 0;
        }

        if (thatSend != fileLength) {
            dataEnc.reset();
            dataEnc.setByteCmd(mCmd.FS_CLOSE);
        } else {
            dataEnc.reset();
            dataEnc.setByteCmd(mCmd.FS_END);
        }

        try {
            IOUtil.write(folderContent.getSocket().getOut(), dataEnc.getData(), dataEnc.getDataLen());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return thatSend;
    }


    // 文件接收服务
    public void fileServer() {
        try {
            fileRecive = new ServerSocket(Config.FILE_SERVER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ViewUpdate.runThread(() -> {
            while (isRun) {
                try {
                    // 等待客户端连接
                    Socket client = fileRecive.accept();
                    ViewUpdate.runThread(() -> handelFile(client));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    // 获取自己的设备信息
    public Device getDevice() {
        Device device = new Device();
        device.setDevName(prefUtil.getString(PreConfig.USER_NAME));
        device.setDevIP(NetWorkUtil.getLocAddress(service));
        device.setDevPort(Config.FILE_SERVER_PORT);
        device.setDevMode(Device.ANDROID);
        return device;
    }

    public String getDevName() {
        return prefUtil.getString(PreConfig.USER_NAME);
    }

    // 通告设备我已上线
    public void noticeDeviceOnLineByIp(String ip) {
        byte[] bytes = new byte[2048];
        DataEnc dataEnc = new DataEnc(bytes);
        dataEnc.setCmd(mCmd.UDP_SET_DEVICES);
        dataEnc.putInt(mDevice.getDevPort());
        dataEnc.putString(mDevice.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(mDevice.getDevMode());
        try {
            UDPTools.sendData(new DatagramSocket(), dataEnc.getData(), dataEnc.getDataLen(), ip, Config.UDP_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    DatagramSocket ipGetSocket = null;

    // 监听并处理获取客户和设置客户端命令
    public void runRecive() {
        new Thread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            // 某些设备UDP不能接收广播数据需要使用下面这几行代码才能正常使用
            WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock multicastLock = mWifiManager.createMulticastLock("multicastLock");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();

            try {
                ipGetSocket = new DatagramSocket(null);
                ipGetSocket.setReuseAddress(true);
                ipGetSocket.setBroadcast(true);
                ipGetSocket.bind(new InetSocketAddress(Config.UDP_PORT));
            } catch (SocketException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    ipGetSocket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                byte[] clone = buf.clone();
                int len = packet.getLength();
                ViewUpdate.runThread(() -> {
                    DataDec dataDec = new DataDec(clone, len);
                    int cmd = dataDec.getCmd();
                    if (cmd == mCmd.UDP_GET_DEVICES) {
                        String devIp = dataDec.getString();
                        // 排除自己发送的数据
                        if (devIp != null && devIp.equals(mDevice.getDevIP())) {
                            return;
                        }
                        // 向设备发送自己的数据
                        ViewUpdate.runThread(() -> noticeDeviceOnLineByIp(devIp));
                    } else if (cmd == mCmd.UDP_SET_DEVICES) {
                        int devPort = dataDec.getInt();
                        String devIp = dataDec.getString();
                        String devName = dataDec.getString();
                        int devMode = dataDec.getInt();

                        // 排除自己发送的数据
                        if (devIp != null && devIp.equals(mDevice.getDevIP())) {
                            return;
                        }

                        String address = devIp + ":" + devPort;
                        Device device = devices.get(address);
                        if (device == null) {
                            device = new Device();
                        }
                        device.setDevPort(devPort);
                        device.setDevIP(devIp);
                        device.setDevName(devName);
                        device.setDevMode(devMode);
                        device.setSetTime(System.currentTimeMillis());
                        devices.put(address, device);

                    } else if (cmd == mCmd.UDP_DEVICES_MESSAGE) {
                        String devIp = dataDec.getString();
                        // 排除自己发送的数据
                        if (devIp != null && devIp.equals(mDevice.getDevIP())) {
                            return;
                        }
                        String devName = dataDec.getString();
                        String message = dataDec.getString();

                        MessageContent content = new MessageContent();
                        content.setUserName(devName);
                        content.setContent(message);
                        content.setLeft(true);
                        Message mMessage = Message.obtain();
                        mMessage.what = mCmd.SERVICE_ADD_MESSGAGE;
                        mMessage.obj = content;
                        messageSend(mMessage);
                    } else if (cmd == mCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD) {
                        String devIp = dataDec.getString();
                        // 排除自己发送的数据
                        if (devIp != null && devIp.equals(mDevice.getDevIP())) {
                            return;
                        }
                        String devName = dataDec.getString();
                        String message = dataDec.getString();

                        MessageContent content = new MessageContent();
                        content.setUserName(devName);
                        content.setContent(message);
                        content.setLeft(true);
                        Message mMessage = Message.obtain();
                        mMessage.what = mCmd.SERVICE_ADD_MESSGAGE;
                        mMessage.obj = content;
                        messageSend(mMessage);

                        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("text", message));
                    }
                });
            }

        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRun = false;
        IOUtil.closeIO(ipGetSocket, fileRecive);
        unregisterReceiver(netWorkReceiver);
    }

    // 局域网扫描设备
    public void scannDevice() {
        ViewUpdate.runThread(() -> {
            while (isRun) {
                try {
                    DataEnc dataEnc = new DataEnc(1024);
                    dataEnc.setCmd(mCmd.UDP_GET_DEVICES);
                    dataEnc.putString(mDevice.getDevIP());
                    UDPTools.sendData(new DatagramSocket(), dataEnc.getData(), dataEnc.getDataLen(), locAddrIndex, Config.UDP_PORT);
                    Thread.sleep(5000);
                    long currentTime = System.currentTimeMillis();
                    // 移除没有心跳的设备
                    for (String key : devices.keySet()) {
                        Device device = devices.get(key);
                        long setTime = device.getSetTime();
                        long timeOut = currentTime - setTime;
                        // 超过20秒没有心跳的设备直接移除
                        if (timeOut > (1000 * 20)) {
                            devices.remove(key);
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 广播发送信息
    public void broadcastMessage(Device device, String message, boolean isClip) {
        if (message.length() > 700) {
            return;
        }
        DataEnc dataEnc = new DataEnc(1024 + message.getBytes().length);
        dataEnc.setCmd(isClip ? mCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : mCmd.UDP_DEVICES_MESSAGE);
        dataEnc.putString(mDevice.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putString(message);

        ViewUpdate.runThread(() -> {
            try {
                if (device == null) {
                    for (Device dev : devices.values()) {
                        UDPTools.sendData(new DatagramSocket(), dataEnc.getData(), dataEnc.getDataLen(), dev.getDevIP(), Config.UDP_PORT);
                    }
                } else {
                    UDPTools.sendData(new DatagramSocket(), dataEnc.getData(), dataEnc.getDataLen(), device.getDevIP(), Config.UDP_PORT);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        });

    }

}



















