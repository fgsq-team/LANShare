package com.fgsqw.lanshare.utils;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.FileShareDBUtil;
import com.fgsqw.lanshare.dialog.WebDialog;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.file.FileInfo;
import com.fgsqw.lanshare.pojo.network.NetInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class mUtil {

    /**
     * @author fgsq
     * @comments 复制文本到剪切板
     * @date 2024/4/27 14:27
     */
    public static void copyString(String content, Context context) {
        // 得到剪贴板管理器
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(content.trim());
    }

    /**
     * @return boolean
     * @author fgsq
     * @comments 判断安装包是否是debug版本
     * @date 2024/4/27 14:28
     */
    public static boolean isDebug(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return String
     * @author fgsq
     * @comments 获取设备信息
     * @date 2024/4/27 14:27
     */
    public static String getBuildInfo(Context context) {
        // 获取手机厂商
        String manufacturer = Build.MANUFACTURER;
        // 获取设备型号
        String model = Build.MODEL;
        // 获取安卓版本
        String androidVersion = Build.VERSION.RELEASE;
        // 获取内核版本
        String kernelVersion = System.getProperty("os.version");
        // 获取操作系统版本
        String osVersion = Build.VERSION.INCREMENTAL;
        // 获取操作系统 API 级别
        int apiLevel = Build.VERSION.SDK_INT;
        String deviceInfo = context.getString(R.string.smartphone_manufacturer) + ": " + manufacturer + "\n" +
                context.getString(R.string.phone_model) + ": " + model + "\n" +
                context.getString(R.string.android_version) + ": " + androidVersion + "\n" +
                context.getString(R.string.kernel_version) + ": " + kernelVersion + "\n" +
                context.getString(R.string.operating_system_version) + ": " + osVersion + "\n" +
                context.getString(R.string.unique_id) + ": " + Config.uniqueUUid + "\n" +
                context.getString(R.string.api_level) + ": " + apiLevel;
        return deviceInfo;
    }

    /**
     * @author fgsq
     * @comments 上传日志到服务器
     * @date 2024/4/27 14:23
     */
    public static void uploadLogs(String description, Context context) {
        ThreadUtils.runThread(() -> {
            String debug = isDebug(context) ? "debug_android" : "release_android";
            int appVersionCode = mUtil.getAppVersionCode(context);
            String appVersionName = getAppVersionName(context);
            // 把设备信息写入日志
            LLog.debug("Description: " + description);
            LLog.debug(getBuildInfo(context));
            LLog.debug(LANService.getInstance().getOnLineDevices().toString());
            LLog.debug(LANService.getInstance().getSelfDevices().toString());
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
            String day = dayFormat.format(new Date());
            File errorLog = new File(Config.SAVE_LOG_PATH + "/error_" + day + ".txt");
            File infoLog = new File(Config.SAVE_LOG_PATH + "/info_" + day + ".txt");
            String s = "";
            String url = Config.SERVER + "/uploadLogs?type="
                    + debug
                    + "&versionCode=" + appVersionCode
                    + "&versionName=" + appVersionName
                    + "&model=" + Build.MODEL;
            s += HttpClientUtils.uploadFiles(url, new File[]{errorLog, infoLog});
            if (!StringUtils.isEmpty(s) && s.contains("ok")) {
                T.s("上传成功");
            } else {
                T.s("上传失败");
            }
        });
    }

    /**
     * @author fgsq
     * @comments 版本更新检查
     * @date 2024/4/27 14:26
     */
    public static void checkUpdate(boolean toast, boolean forceUpdate, Activity activity) {
        PrefUtil prefUtil = new PrefUtil(activity);
        ThreadUtils.runThread(() -> {
            try {
                int appVersionCode = mUtil.getAppVersionCode(activity);
                Map<String, String> paramsMap = new HashMap<>();
                paramsMap.put("type", (isDebug(activity) ? "debug_android" : "release_android"));
                paramsMap.put("versionCode", String.valueOf(appVersionCode));
                paramsMap.put("versionName", getAppVersionName(activity));
                String s = HttpClientUtils.sendGetRequest(Config.SERVER + "/get_version", paramsMap);
                JSONObject jsonObject = JSON.parseObject(s);
                boolean isForceUpdate = jsonObject.getBoolean("isForceUpdate");
                int versionCode = jsonObject.getIntValue("versionCode");
                String fileUrl = jsonObject.getString("fileUrl");
                int forceUpdateMinVersion = jsonObject.getIntValue("forceUpdateMinVersion");
                int noUpdateVersion = prefUtil.getInt(PreConfig.NO_UPDATE_VERSION, -1);
                isForceUpdate = isForceUpdate && appVersionCode < forceUpdateMinVersion;
                if (versionCode > appVersionCode && (forceUpdate || versionCode > noUpdateVersion || isForceUpdate)) {
                    boolean finalIsForceUpdate = isForceUpdate;
                    ThreadUtils.threadUi(() -> {
                        WebDialog infoDialog = new WebDialog(activity);
                        infoDialog.setTitle(activity.getString(R.string.update_version));
                        infoDialog.setWebContent(jsonObject.getString("updateContent"));
                        infoDialog.setLeftButtonText(activity.getString(R.string.cancel));
                        infoDialog.setRightButtonText(activity.getString(R.string.update));
                        infoDialog.setCancelable(false);
                        infoDialog.setOnClickListener(agree -> {
                            if (agree) {
                                prefUtil.saveInt(PreConfig.NO_UPDATE_VERSION, -1);
                                ProgressDialog progressDialog = new ProgressDialog(activity);
                                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                progressDialog.setCancelable(false);
                                progressDialog.setCanceledOnTouchOutside(false);
                                progressDialog.setButton(activity.getString(R.string.hide), (p1, p2) -> {
                                    progressDialog.cancel();
                                });
                                progressDialog.setTitle(activity.getString(R.string.downloading));
                                progressDialog.show();
                                String url;
                                if (fileUrl.startsWith("http")) {
                                    url = fileUrl;
                                } else {
                                    url = Config.SERVER + fileUrl;
                                }
                                ThreadUtils.runThread(() -> HttpClientUtils.downloadFile(url + "?device=android&type=android",
                                        activity.getExternalCacheDir().getPath() + "/update.apk",
                                        new HttpClientUtils.DownloadProgress() {
                                            @Override
                                            public void progress(int p) {
                                                ThreadUtils.threadUi(() -> {
                                                    progressDialog.setProgress(p);
                                                });
                                            }

                                            @Override
                                            public void complete(String path, long fileSize) {
                                                progressDialog.dismiss();
                                                FileUtil.openFile(activity, new File(path));
                                            }
                                        }));
                            } else {
                                if (finalIsForceUpdate) {
                                    prefUtil.saveInt(PreConfig.NO_UPDATE_VERSION, -1);
                                    activity.finish();
                                } else {
                                    prefUtil.saveInt(PreConfig.NO_UPDATE_VERSION, versionCode);
                                }
                                infoDialog.dismiss();
                            }
                        });
                        infoDialog.show();
                    });
                } else if (toast) {
                    T.s(R.string.already_up_to_date);
                }
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * @author fgsq
     * @comments 生成分享链接
     * @date 2024/4/27 14:27
     */
    public static void shareFile(boolean isIPv4, FileInfo fileSource, Context context) {
        FileShareDBUtil fileShareDBUtil = new FileShareDBUtil(context);
        final String[] items = {context.getString(R.string.sure_one_time_download), context.getString(R.string.one_day), context.getString(R.string.three_days)};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_valid_days);
        // 设置列表项
        builder.setItems(items, (dialog, which) -> {
            // 用户点击列表项后的处理
            int days = 0;
            switch (which) {
                case 0: {
                    days = -1;
                    break;
                }
                case 1: {
                    days = 1;
                    break;
                }
                case 2: {
                    days = 3;
                    break;
                }
                default:
                    break;
            }
            String uuid = StringUtils.getUUID();
            fileShareDBUtil.addShare(uuid, fileSource.getName(), fileSource.getPath(), false, days);
            String ip;
            if (isIPv4) {
                Set<Device> localDevices = LANService.getInstance().localDevices;
                if (!localDevices.isEmpty()) {
                    ip = "http://" + localDevices.iterator().next().getDevIP() + ":" + Config.FILE_SERVER_PORT + "/sharefile/" + fileSource.getName() + "?uuid=" + uuid;
                } else {
                    T.s((R.string.no_available_ipv4_address_found));
                    return;
                }
            } else {
                List<NetInfo> ipv6NetInfoList = LANService.getInstance().ipv6NetInfoList;
                if (ipv6NetInfoList == null || ipv6NetInfoList.isEmpty()) {
                    T.s((R.string.no_available_ipv6_address_found));
                    return;
                }
                NetInfo netInfo = ipv6NetInfoList.get(0);
                ip = "http://[" + netInfo.getIp() + "]:" + Config.FILE_SERVER_PORT + "/sharefile/" + fileSource.getName() + "?uuid=" + uuid;
            }
            copyString(ip, context);
            T.s((R.string.link_has_been_copied_to_clipboard));
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    public static String stringSize(String str, int i) {
        String getstr = null;
        if (str.length() > i) {
            getstr = str.substring(0, i - 1);
            getstr += "..";
        } else {
            return str;
        }
        return getstr;
    }

    public static String addString(Object... obj) {
        if (obj != null && obj.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (Object o : obj) {
                sb.append(o);
            }
            return sb.toString();
        }
        return null;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int random(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static String generateRandomString(int length) {
        // 定义字符集，包含字母和数字
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            // 随机选择一个字符
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 获取程序版本
     */
    public static String getAppVersionName(Context context) {
        String versionName = null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception ignored) {
        }
        return versionName;
    }

    public static int getAppVersionCode(Context context) {
        int versionCode = -1;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception ignored) {
        }
        return versionCode;
    }

    /**
     * 获取自身apk路径
     **/
    public static String getSelfApkPath(Context context) {
        String appDir = null;
        try {
            //通过包名获取程序源文件路径
            appDir = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appDir;
    }

    /****************
     *
     * 发起添加群流程。群号：LANShare交流/反馈群(538809905) 的 key 为： xJNzrothp7Dj3U3GDRxEjJaH78y4TSrF
     * 调用 joinQQGroup(xJNzrothp7Dj3U3GDRxEjJaH78y4TSrF) 即可发起手Q客户端申请加群 LANShare交流/反馈群(538809905)
     *
     * @param key 由官网生成的key
     * @return 返回true表示呼起手Q成功，返回false表示呼起失败
     ******************/
    public static boolean joinQQGroup(String key, Context context) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            // 未安装手Q或安装的版本不支持
            return false;
        }
    }

    public static void openUrlInBrowser(Context context, String url) {
        // 创建Intent
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        // 检查是否有应用可以处理这个Intent
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            // 打开链接
            context.startActivity(intent);
        } else {
            // 如果没有应用可以处理这个Intent，则显示错误消息或采取其他操作
            // 在这里，你可以选择抛出一个异常、显示一个Toast消息或者执行其他逻辑
        }
    }

    public static <T> List<T> singletonArrayList(T... arr) {
        return new ArrayList<>(Arrays.asList(arr));
    }
}
