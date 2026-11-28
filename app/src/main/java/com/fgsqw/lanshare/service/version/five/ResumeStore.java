package com.fgsqw.lanshare.service.version.five;

import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.LLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * V5 断点续传存储工具
 * <p>以 {@code .part} 分段文件的形式保存未传输完成的字节,偏移量由分段文件当前长度推导,
 * 天然跨进程/重启持久,无需数据库。</p>
 * <p>目录结构:</p>
 * <pre>
 *   {FILE_SAVE_PATH}/.lanshare_resume/
 *       {fileId}.part                     // 单文件分段
 *       {folderFileId}/                    // 文件夹暂存目录
 *           .target                        // 记录该文件夹的最终落地目录
 *           {childFileId}.part            // 文件夹内子文件分段
 * </pre>
 *
 * @author fgsq
 * @version 5.0
 */
public final class ResumeStore {

    /** 续传根目录名 */
    private static final String RESUME_DIR = ".lanshare_resume";
    /** 分段文件后缀 */
    private static final String PART_SUFFIX = ".part";
    /** 文件夹最终目录标记文件名 */
    private static final String TARGET_MARKER = ".target";
    /** 分段文件过期时间(7 天) */
    private static final long PART_EXPIRE_MS = 7L * 24 * 60 * 60 * 1000;

    private ResumeStore() {
    }

    // ==================== 稳定 ID ====================

    /**
     * 基于种子生成稳定 fileId(跨重启一致),用于命名分段文件。
     *
     * @param seed 种子(发送设备 UUID + 路径 + 长度等)
     * @return 稳定的 32 位十六进制摘要
     */
    public static String stableId(String seed) {
        return md5("LRESUME:" + seed);
    }

    private static String md5(String source) {
        if (source == null) {
            source = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(source.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(source.hashCode());
        }
    }

    // ==================== 路径 ====================

    /**
     * @return 续传根目录(自动创建)
     */
    public static File partRoot() {
        File root = new File(Config.FILE_SAVE_PATH, RESUME_DIR);
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    /**
     * 单文件分段路径。
     *
     * @param fileId 稳定 fileId
     * @return 分段文件
     */
    public static File filePart(String fileId) {
        return new File(partRoot(), fileId + PART_SUFFIX);
    }

    /**
     * 文件夹暂存目录(自动创建)。
     *
     * @param folderFileId 文件夹稳定 fileId
     * @return 暂存目录
     */
    public static File folderStageDir(String folderFileId) {
        File dir = new File(partRoot(), folderFileId);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 文件夹内子文件分段路径。
     *
     * @param folderFileId 文件夹稳定 fileId
     * @param childFileId  子文件稳定 fileId
     * @return 子文件分段
     */
    public static File childPart(String folderFileId, String childFileId) {
        return new File(new File(partRoot(), folderFileId), childFileId + PART_SUFFIX);
    }

    // ==================== 偏移量 ====================

    /**
     * 读取分段文件当前已完成字节数;若分段文件损坏(超过总长度)则删除并返回 0。
     *
     * @param part   分段文件
     * @param total  目标文件总长度
     * @return 已完成字节数
     */
    public static long readOffset(File part, long total) {
        if (part == null || !part.exists()) {
            return 0;
        }
        long len = part.length();
        if (len > total || total <= 0) {
            deleteQuietly(part);
            return 0;
        }
        return len;
    }

    /**
     * 打开分段文件用于追加续传。
     *
     * @param part        分段文件
     * @param startOffset 期望续传起点(应等于分段文件当前长度)
     * @return 输出流
     * @throws IOException 打开失败
     */
    public static OutputStream openAppend(File part, long startOffset) throws IOException {
        File parent = part.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        // startOffset 由 part.length() 推导,追加写即可对齐
        return new FileOutputStream(part, startOffset > 0);
    }

    // ==================== 文件夹最终目录标记 ====================

    /**
     * 记录文件夹最终落地目录,保证续传时复用同一目录,避免拆分。
     *
     * @param stageDir    暂存目录
     * @param finalDirPath 最终目录绝对路径
     */
    public static void writeTarget(File stageDir, String finalDirPath) {
        File marker = new File(stageDir, TARGET_MARKER);
        OutputStream out = null;
        try {
            out = new FileOutputStream(marker);
            out.write(finalDirPath.getBytes("UTF-8"));
        } catch (IOException e) {
            LLog.error("writeTarget error", e);
        } finally {
            IOUtil.closeIO(out);
        }
    }

    /**
     * 读取文件夹最终落地目录。
     *
     * @param stageDir 暂存目录
     * @return 最终目录;不存在返回 null
     */
    public static File readTarget(File stageDir) {
        File marker = new File(stageDir, TARGET_MARKER);
        if (!marker.exists()) {
            return null;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(marker);
            byte[] buffer = new byte[(int) marker.length()];
            int read = in.read(buffer);
            if (read <= 0) {
                return null;
            }
            return new File(new String(buffer, 0, read, "UTF-8"));
        } catch (IOException e) {
            LLog.error("readTarget error", e);
            return null;
        } finally {
            IOUtil.closeIO(in);
        }
    }

    // ==================== 完成化 ====================

    /**
     * 将分段文件完成化为最终文件(优先重命名,失败回退复制删除)。
     *
     * @param part       分段文件
     * @param targetFile 目标文件(已去重命名)
     * @return 实际写入的目标文件
     */
    public static File finalizePart(File part, File targetFile) {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File finalFile = FileUtil.avoidDuplication(targetFile);
        if (part.exists()) {
            if (!part.renameTo(finalFile)) {
                copyAndDelete(part, finalFile);
            }
        }
        return finalFile;
    }

    /**
     * 将子文件分段移动到最终目录内的相对路径处。
     *
     * @param part       子文件分段
     * @param finalDir   最终目录
     * @param relativeName 子文件相对名称(可能含子目录)
     */
    public static void movePartToFinal(File part, File finalDir, String relativeName) {
        if (part == null || !part.exists()) {
            return;
        }
        File target = new File(finalDir, relativeName);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (target.exists() && target.length() == part.length()) {
            // 已存在且大小一致,直接丢弃分段
            deleteQuietly(part);
            return;
        }
        if (!part.renameTo(target)) {
            copyAndDelete(part, target);
        }
    }

    private static void copyAndDelete(File src, File dst) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
            deleteQuietly(src);
        } catch (IOException e) {
            LLog.error("copyAndDelete error", e);
        } finally {
            IOUtil.closeIO(out, in);
        }
    }

    // ==================== 跳过 & 清理 ====================

    /**
     * 从输入流跳过指定字节数(续传时跳过已发送部分)。
     *
     * @param in    输入流
     * @param count 要跳过的字节数
     * @throws IOException 跳过失败
     */
    public static void skipFully(InputStream in, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                // 某些流(如部分网络流)skip 返回 0,退化为读
                if (in.read() == -1) {
                    break;
                }
                remaining -= 1;
            } else {
                remaining -= skipped;
            }
        }
    }

    /**
     * 删除分段文件(含过期分段清理),静默失败。
     *
     * @param file 目标文件
     * @return 是否删除成功(或本就不存在)
     */
    public static boolean deleteQuietly(File file) {
        return file == null || !file.exists() || file.delete();
    }

    /**
     * 递归删除目录。
     *
     * @param dir 目录
     */
    public static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * 清理过期的分段文件与暂存目录。
     */
    public static void cleanupExpired() {
        File root = new File(Config.FILE_SAVE_PATH, RESUME_DIR);
        if (!root.exists()) {
            return;
        }
        long now = System.currentTimeMillis();
        File[] files = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (now - f.lastModified() > PART_EXPIRE_MS) {
                    if (f.isDirectory()) {
                        deleteDir(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
    }
}
