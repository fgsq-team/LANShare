package com.fgsqw.lanshare.utils;

import com.fgsqw.utils.IOUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author fgsq
 * @comments http请求工具类
 * @date 2024/4/28 16:03
 */
public class HttpClientUtils {

    private final static String END_FLAG = "\r\n";

    /**
     * @return String
     * @author fgsq
     * @comments 发送GET请求
     * @date 2024/4/28 16:01
     */
    public static String sendGetRequest(String urlString, Map<String, String> paramsMap) {
        try {
            String connector;
            if (urlString.contains("?")) {
                connector = "&";
            } else {
                connector = "?";
            }
            StringBuilder sb = new StringBuilder(connector);
            int i = 0;
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                i++;
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (i < paramsMap.size()) {
                    sb.append("&");
                }
            }
            URL url = new URL(urlString + sb);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            return response.toString();
        } catch (IOException e) {
            LLog.error(e);
        }
        return null;
    }

    /**
     * @return String
     * @author fgsq
     * @comments 发送POST请求
     * @date 2024/4/28 16:01
     */
    public static String sendPostRequest(String urlString, String payload) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = payload.getBytes(FileUtil.UTF_8);
                outputStream.write(input, 0, input.length);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            return response.toString();
        } catch (IOException e) {
            LLog.error(e);
        }
        return null;
    }

    /**
     * @return String
     * @author fgsq
     * @comments 上传文件
     * @date 2024/4/28 16:00
     */
    public static String uploadFiles(String urlString, File[] files) {
        try {
            String boundary = StringUtils.getUUID();
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=---" + boundary);
            // 允许输出
            connection.setDoOutput(true);
            // 用于写入数据
            OutputStream os = connection.getOutputStream();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String sb = "-----" + boundary + END_FLAG +
                        "Content-Disposition: form-data; name=\"file\";filename=\"" +
                        file.getName() + "\""
                        + END_FLAG
                        + "Content-Type: " + HttpURLConnection.guessContentTypeFromName(file.getName())
                        + END_FLAG
                        + END_FLAG;
                // 添加文件
                os.write(sb.getBytes(com.fgsqw.utils.FileUtil.UTF_8));
                FileInputStream fis = new FileInputStream(file);
                // 写入文件
                IOUtil.transfer(fis, os);
                os.flush();
                fis.close();
                // 发送结束标记
                os.write(END_FLAG.getBytes(FileUtil.UTF_8));
                os.write(("-----" + boundary).getBytes(com.fgsqw.utils.FileUtil.UTF_8));
                if (i == files.length - 1) {
                    os.write(("--").getBytes(com.fgsqw.utils.FileUtil.UTF_8));
                }
                os.write(END_FLAG.getBytes(FileUtil.UTF_8));
                os.flush();
            }
            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            connection.disconnect();
            return response.toString();
        } catch (IOException e) {
            LLog.error(e);
        }
        return "";
    }

    /**
     * @author fgsq
     * @comments 下载文件
     * @date 2024/4/28 16:01
     */
    public static void downloadFile(String fileUrl, String saveFilePath, DownloadProgress downloadProgress) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(saveFilePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long fileSize = connection.getContentLength();
                long total = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    total += bytesRead;
                    int progress = (int) (total * 100F / fileSize);
                    if (downloadProgress != null) {
                        downloadProgress.progress(progress);
                    }
                }
                if (downloadProgress != null) {
                    downloadProgress.complete(saveFilePath, fileSize);
                }
            }
            connection.disconnect();
        } catch (IOException e) {
            LLog.error(e);
        }
    }

    public interface DownloadProgress {
        void progress(int p);

        void complete(String path, long fileSilze);
    }
}
