package com.fgsqw.lanshare.service;

import android.app.Service;
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

import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.pojo.DataObject;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.fgsqw.lanshare.pojo.RecordFile;
import com.fgsqw.lanshare.pojo.mOutputStream;
import com.fgsqw.lanshare.pojo.mSocket;
import com.fgsqw.lanshare.receiver.NetWorkReceiver;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.DataDec;
import com.fgsqw.lanshare.utils.DataEnc;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LANService extends Service {

    public static final String TAG = "LANService";

    public static LANService service;
    ServerSocket fileRecive;
    public Map<String, Device> devices = new ConcurrentHashMap<>();
    Messenger mMessenger;
    Device mDevice;
    String locAddrIndex = "255.255.255.255";
    NetWorkReceiver netWorkReceiver;
    PrefUtil prefUtil;

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
        //receiver();
        prefUtil = new PrefUtil(this);
        mDevice = getDevice();
        mUtil.showNotification(this, "LANFile", "服务正在运行...");
        locAddrIndex = NetWorkUtil.getLocAddrIndex(mDevice.getDevIP()) + "255";

        try {
            fileRecive = new ServerSocket(Config.FILE_SERVER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        runRecive();
        fileServer();
        // 通告局域网所有设备我已上线
        ViewUpdate.runThread(() -> noticeDeviceOnLineByIp(locAddrIndex));
        scannDevice();

    }

    public void receiver() {
        netWorkReceiver = new NetWorkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkReceiver, filter);
    }

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
            if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize()) return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        DataDec dataDec = new DataDec(buffer, DataEnc.getHeaderSize());

        int cmd = dataDec.getCmd();

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

            List<RecordFile> recordFileList = new ArrayList<>();
            Message mMessage;

            for (int i = 0; i < count; i++) {

                try {
                    if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize()) return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                length = dataDec.getLength();

                try {
                    if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length) return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                dataDec.setData(buffer, buffer.length);

                long fileSize = dataDec.getLong();
                // 文件名称
                String fileName = dataDec.getString();

                Log.d(TAG, "filename:" + fileName + " fileSize:" + fileSize);

                RecordFile recordFile = new RecordFile();
                recordFile.setName(fileName);
                recordFile.setLength(fileSize);
//                recordFile.setPath(outFile.getPath());
                recordFile.setSocket(new mSocket(input, out));
                recordFile.setIndex(i);
                recordFile.setRecv(true);
                recordFileList.add(recordFile);
            }
            // 是否弹出确认接收dialog
            boolean isNotRecvDialog = prefUtil.getBoolean("not_recv_dialog");
            DataObject dataObject = new DataObject(device, recordFileList, client, input, out);

            if (isNotRecvDialog) {
                startRecvFile(dataObject, true);
            } else {
                // 弹出是否接收文件请求弹窗
                mMessage = Message.obtain();

                mMessage.what = mCmd.SERVICE_IF_RECIVE_FILES;
                mMessage.arg1 = count;
                mMessage.obj = dataObject;
                messageSend(mMessage);
            }
        }
    }

    // 接收文件缓存
    final byte[] recvBuffer = new byte[2 * 1024 * 1024];
    // 发送文件缓存
    final byte[] sendBuffer = new byte[2 * 1024 * 1024];


    public void sendCmd(RecordFile recordFile, OutputStream out) {
        ViewUpdate.runThread(() -> {
            DataEnc dataEnc = new DataEnc();
            dataEnc.setByteCmd(mCmd.FS_CLOSE);
            dataEnc.setCount(recordFile.getIndex());
            try {
                out.write(dataEnc.getData(), 0, dataEnc.getDataLen());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void startRecvFile(DataObject dataObject, boolean isAgree) {
        ViewUpdate.runThread(() -> {
            List<RecordFile> recordFileList = (List<RecordFile>) dataObject.getObj2();
            Socket client = (Socket) dataObject.getObj3();
            InputStream input = (InputStream) dataObject.getObj4();
            OutputStream out = (OutputStream) dataObject.getObj5();

            Message mMessage;

            // 此处需要是否接收文件逻辑
            DataEnc dataEnc = new DataEnc(0);
            try {
                if (isAgree) {
                    dataEnc.setCmd(mCmd.FS_AGREE);
                    out.write(dataEnc.getData(), 0, dataEnc.getDataLen());
                } else {
                    dataEnc.setCmd(mCmd.FS_NOT_AGREE);
                    out.write(dataEnc.getData(), 0, dataEnc.getDataLen());
                    IOUtil.closeIO(input, out, client);
                    return;
                }

            } catch (IOException e) {
                e.printStackTrace();
                IOUtil.closeIO(input, out, client);
                return;
            }

            mMessage = Message.obtain();
            mMessage.what = mCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = recordFileList;
            messageSend(mMessage);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (recvBuffer) {
                for (int i = 0; i < recordFileList.size(); i++) {
                    RecordFile recordFile = recordFileList.get(i);
                    File file = new File(Config.FILE_SAVE_PATH + getNameType(recordFile.getName()) + "/");

                    // 如果创建文件夹失败就关闭流退出
                    if (!file.exists() && !file.mkdirs()) {
                        IOUtil.closeIO(input, out, client);
                        return;
                    }

                    File outFile = new File(file, recordFile.getName());
                    if (outFile.exists()) {
                        for (int s = 1; s < 65535; s++) {
                            String str;
                            if (recordFile.getName().contains(".")) {
                                String prefix = recordFile.getName().substring(0, recordFile.getName().lastIndexOf(".")) + "(" + s + ")";
                                String suffix = recordFile.getName().substring(recordFile.getName().lastIndexOf("."));
                                str = prefix + suffix;
                            } else {
                                str = recordFile.getName() + "(" + s + ")";
                            }
                            outFile = new File(file, str);
                            if (!outFile.exists()) {
                                break;
                            }
                        }
                    }

                    OutputStream outFileStream = null;
                    try {
                        outFileStream = new FileOutputStream(outFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        IOUtil.closeIO(input, out, client);
                        return;
                    }

                    long totalRecv = 0;
                    int p = 0;
                    InputStream mInput = recordFile.getSocket().getInputStream();
                    DataDec dataDec = new DataDec(recvBuffer);
                    try {
                        while (true) {
                            if (IOUtil.read(mInput, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize()) break;
                            int cmd = dataDec.getByteCmd();
                            if (cmd == mCmd.FS_DATA) {          // 数据
                                int length = dataDec.getLength();
                                if (IOUtil.read(mInput, recvBuffer, DataEnc.getHeaderSize(), length) != length) break;
                                outFileStream.write(recvBuffer, DataEnc.getHeaderSize(), length);
                                totalRecv += length;
                                int progeress = (int) (totalRecv * 100.0F / recordFile.getLength());
                                if (progeress != p) {
                                    recordFile.setProgress(progeress);
                                    mMessage = Message.obtain();
                                    mMessage.what = mCmd.SERVICE_PROGRESS;
                                    mMessage.obj = recordFile;
                                    messageSend(mMessage);
                                    p = progeress;
                                }
                            } else if (cmd == mCmd.FS_END) {    // 传输完毕
                                break;
                            } else {  // 关闭传送
                                Log.d(TAG, "close");
                                totalRecv = 0;
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        totalRecv = 0;
                    }
                    if (totalRecv != recordFile.getLength()) {
                        outFile.delete();
                        recordFile.setSuccess(false);
                        recordFile.setMessage("接收失败");
                    } else {
                        recordFile.setPath(outFile.getPath());
                        recordFile.setSuccess(true);
                        recordFile.setMessage("接收成功");
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mMessage = Message.obtain();
                    mMessage.what = mCmd.SERVICE_CLOSE_PROGRESS;
                    mMessage.obj = recordFile;
                    messageSend(mMessage);

                    IOUtil.closeIO(outFileStream);

                }
                IOUtil.closeIO(input, out, client);
            }


        });
    }


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
     * 发送文件同时接收指令 此线程用于接收端取消接收文件
     *
     * @param recordFileList
     * @param input
     */
    public void startRecvCmd(List<RecordFile> recordFileList, InputStream input) {
        ViewUpdate.runThread(() -> {
            DataDec dataDec = new DataDec();
            try {
                while (true) {
                    int read = IOUtil.read(input, dataDec.getData(), 0, dataDec.getByteLen());
                    if (read > 0) {
                        if (read == dataDec.getByteLen()) {
                            int cmd = dataDec.getByteCmd();
                            if (cmd == mCmd.FS_CLOSE) {
                                int index = dataDec.getCount();
                                Log.d(TAG, "取消:" + index);
                                for (RecordFile recordFile : recordFileList) {
                                    if (recordFile.getIndex() == index) {
                                        recordFile.getSocket().mClose();
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
                Log.d(TAG, "startRecvCmd:" + e.getMessage());
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
            if (!socket.isConnected()) {
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
        List<RecordFile> recordFileList = new ArrayList<>();
        synchronized (sendBuffer) {
            try {
                DataEnc dataEnc = new DataEnc(sendBuffer);
                dataEnc.setCmd(mCmd.FS_SHARE_FILE);
                dataEnc.setCount(fileList.size());
                dataEnc.putInt(mDevice.getDevPort());
                dataEnc.putString(mDevice.getDevIP());
                dataEnc.putString(mDevice.getDevName());

                out.write(dataEnc.getData(), 0, dataEnc.getDataLen());
                for (int i = 0; i < fileList.size(); i++) {
                    FileInfo fileInfo = fileList.get(i);
                    RecordFile recordFile = new RecordFile();
                    recordFile.setName(fileInfo.getName());
                    recordFile.setLength(fileInfo.getLength());
                    recordFile.setPath(fileInfo.getPath());
                    recordFile.setSocket(new mSocket(input, out));
                    recordFile.setIndex(i);
                    recordFileList.add(recordFile);

                    dataEnc.reset();
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    out.write(dataEnc.getData(), 0, dataEnc.getDataLen());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            // 读取对方是否同意接收文件
            try {
                IOUtil.read(input, sendBuffer, 0, DataEnc.HEADER_LEN);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            DataDec dataDec = new DataDec(sendBuffer, DataEnc.HEADER_LEN);
            if (dataDec.getCmd() == mCmd.FS_NOT_AGREE) {
                T.s(device.getDevName() + " 取消接收文件");
                return;
            }

            Message mMessage;
            mMessage = Message.obtain();
            mMessage.what = mCmd.SERVICE_SHOW_PROGRESS;
            mMessage.obj = recordFileList;
            messageSend(mMessage);

            startRecvCmd(recordFileList, input);

            for (RecordFile recordFile : recordFileList) {

                InputStream fileIs = null;
                mOutputStream mOut = recordFile.getSocket().getOutputStream();

                try {
                    fileIs = new FileInputStream(recordFile.getPath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }

                // 发送文件
                Log.d(TAG, "发送文件:" + recordFile.getPath() + " 大小:" + recordFile.getLength());

                long totalSend = 0;
                int ten = 0;
                int p = 0;

                DataEnc dataEnc = new DataEnc(sendBuffer);
                dataEnc.setByteCmd(mCmd.FS_DATA);

                try {
                    while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), sendBuffer.length - DataEnc.getHeaderSize())) != -1) {
                        dataEnc.setDataIndex(ten);
                        mOut.write(dataEnc);
                        totalSend += ten;
                        int prngeress = (int) (totalSend * 100.0F / recordFile.getLength());
                        if (prngeress != p) {
                            recordFile.setProgress(prngeress);
                            mMessage = Message.obtain();
                            mMessage.what = mCmd.SERVICE_PROGRESS;
                            mMessage.obj = recordFile;
                            messageSend(mMessage);
                            p = prngeress;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    totalSend = 0;
                }

                try {
                    if (totalSend != recordFile.getLength()) {
                        recordFile.setSuccess(false);
                        recordFile.setMessage("发送失败");
                        dataEnc.reset();
                        dataEnc.setByteCmd(mCmd.FS_CLOSE);
                    } else {
                        recordFile.setSuccess(true);
                        recordFile.setMessage("发送成功");
                        dataEnc.reset();
                        dataEnc.setByteCmd(mCmd.FS_END);
                    }
                    out.write(dataEnc.getData(), 0, DataEnc.getHeaderSize());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mMessage = Message.obtain();
                mMessage.what = mCmd.SERVICE_CLOSE_PROGRESS;
                mMessage.obj = recordFile;
                messageSend(mMessage);

                Log.d(TAG, "发送成功:" + totalSend);

            }
        }

    }


    // 文件接收服务
    public void fileServer() {
        ViewUpdate.runThread(() -> {
            while (true) {
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
    public static Device getDevice() {
        Device device = new Device();
        device.setDevName(android.os.Build.MODEL);
        device.setDevIP(NetWorkUtil.getLocAddress(service));
        device.setDevPort(Config.FILE_SERVER_PORT);
        device.setDevMode(Device.ANDROID);
        return device;
    }

    // 通告设备我已上线
    public void noticeDeviceOnLineByIp(String ip) {
        byte[] bytes = new byte[2048];
        DataEnc dataEnc = new DataEnc(bytes);
        dataEnc.setCmd(mCmd.UDP_SET_DEVICES);
        dataEnc.putInt(mDevice.getDevPort());
        dataEnc.putString(mDevice.getDevIP());
        dataEnc.putString(mDevice.getDevName());
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

            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            // 某些设备UDP不能接收广播数据需要使用下面这几行代码才能正常使用
            WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock multicastLock = mWifiManager.createMulticastLock("test");
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
                DataDec dataDec = new DataDec(buf, packet.getLength());
                int cmd = dataDec.getCmd();
                if (cmd == mCmd.UDP_GET_DEVICES) {
                    String devIp = dataDec.getString();
                    // 排除自己发送的数据
                    if (devIp.equals(mDevice.getDevIP())) {
                        continue;
                    }
                    ViewUpdate.runThread(() -> noticeDeviceOnLineByIp(devIp));

                } else if (cmd == mCmd.UDP_SET_DEVICES) {
                    int devPort = dataDec.getInt();
                    String devIp = dataDec.getString();
                    String devName = dataDec.getString();
                    int devMode = dataDec.getInt();

                    // 排除自己发送的数据
                    if (devIp.equals(mDevice.getDevIP())) {
                        continue;
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

                  /*  Message mMessage = Message.obtain();
                    mMessage.what = mCmd.SERVICE_UPDATE_DEVICES;
                    mMessage.obj = devices;
                    messageSend(mMessage);*/

                }

            }

        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IOUtil.closeIO(ipGetSocket);
        //unregisterReceiver(netWorkReceiver);

    }

    // 局域网扫描设备
    public void scannDevice() {
        ViewUpdate.runThread(() -> {
            while (true) {
                try {
                    DataEnc dataEnc = new DataEnc(2048);
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
                        if (timeOut > (1000 * 6)) {
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


}



















