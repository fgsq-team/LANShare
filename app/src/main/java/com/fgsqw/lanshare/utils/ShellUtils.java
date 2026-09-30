package com.fgsqw.lanshare.utils;

import java.io.*;

public class ShellUtils {

    // 判断是否是root用户
    public static boolean isRoot() {
        return execCmd("echo root", true, false).result == 0;
    }

    // 执行一个shell命令，并返回结果
    public static CommandResult execCmd(String command, boolean isRoot, boolean isNeedResultMsg) {
        return execCmd(new String[]{command}, isRoot, isNeedResultMsg);
    }

    // 执行一组shell命令，并返回结果
    public static CommandResult execCmd(String[] commands, boolean isRoot, boolean isNeedResultMsg) {
        int result = -1;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;

        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        InputStream es = null;
        InputStreamReader esr = null;
        BufferedReader ber = null;

        try {
            // 获取Runtime对象
            Runtime runtime = Runtime.getRuntime();
            // 根据是否是root用户，执行不同的命令
            process = runtime.exec(isRoot ? "su" : "sh");
            // 获取进程的输出流
            os = new DataOutputStream(process.getOutputStream());
            // 遍历命令数组，逐个写入输出流
            for (String command : commands) {
                if (command == null) {
                    continue;
                }
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();
            }
            // 写入退出命令
            os.writeBytes("exit\n");
            os.flush();

            // 等待进程结束，并获取返回值
            result = process.waitFor();

            // 如果需要返回结果消息，就获取进程的输入流和错误流
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                is = process.getInputStream();
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                es = process.getErrorStream();
                esr = new InputStreamReader(es);
                ber = new BufferedReader(esr);

                String line;
                // 读取输入流中的内容，拼接成成功消息
                while ((line = br.readLine()) != null) {
                    successMsg.append(line);
                }
                // 读取错误流中的内容，拼接成错误消息
                while ((line = ber.readLine()) != null) {
                    errorMsg.append(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭所有的流
            try {
                if (os != null) {
                    os.close();
                }
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (is != null) {
                    is.close();
                }
                if (ber != null) {
                    ber.close();
                }
                if (esr != null) {
                    esr.close();
                }
                if (es != null) {
                    es.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 销毁进程
            if (process != null) {
                process.destroy();
            }
        }
        // 返回一个CommandResult对象，包含返回值和结果消息
        return new CommandResult(result, successMsg == null ? null : successMsg.toString(), errorMsg == null ? null : errorMsg.toString());
    }

    // 定义一个内部类，表示命令执行的结果
    public static class CommandResult {

        // 返回值，0表示成功，其他表示失败
        public int result;
        // 成功消息
        public String successMsg;
        // 错误消息
        public String errorMsg;

        public CommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        public int getResult() {
            return result;
        }

        public String getSuccessMsg() {
            return successMsg;
        }

        public String getErrorMsg() {
            return errorMsg;
        }
    }
}
