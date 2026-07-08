package org.quyq.gwsu.common.job.util;

import org.quyq.gwsu.common.job.context.XxlJobHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 脚本工具类
 */
public class ScriptUtil {

    /**
     * 生成脚本文件
     *
     * @param scriptFileName 脚本文件名
     * @param scriptContent  脚本内容
     */
    public static void markScriptFile(String scriptFileName, String scriptContent) throws IOException {
        Path path = Path.of(scriptFileName);
        Files.writeString(path, scriptContent);
    }

    /**
     * 脚本执行，日志文件实时输出
     *
     * @param command    命令
     * @param scriptFile 脚本文件
     * @param logFile    日志文件
     * @param params     参数
     * @return 退出码
     */
    public static int execToFile(String command, String scriptFile, String logFile, String... params) throws IOException {

        FileOutputStream fileOutputStream = null;
        Thread inputThread = null;
        Thread errorThread = null;
        Process process = null;
        try {
            // 1、构建文件输出流
            fileOutputStream = new FileOutputStream(logFile, true);

            // 2、构建命令
            List<String> cmdarray = new ArrayList<>();
            cmdarray.add(command);
            cmdarray.add(scriptFile);
            if (params != null && params.length > 0) {
                for (String param : params) {
                    cmdarray.add(param);
                }
            }
            String[] cmdarrayFinal = cmdarray.toArray(new String[0]);

            // 3、执行进程
            process = Runtime.getRuntime().exec(cmdarrayFinal);
            Process finalProcess = process;

            // 4、读取脚本日志
            final FileOutputStream finalFileOutputStream = fileOutputStream;
            inputThread = new Thread(() -> {
                try {
                    copyStream(finalProcess.getInputStream(), finalFileOutputStream, true);
                } catch (IOException e) {
                    XxlJobHelper.log(e);
                }
            });
            errorThread = new Thread(() -> {
                try {
                    copyStream(finalProcess.getErrorStream(), finalFileOutputStream, true);
                } catch (IOException e) {
                    XxlJobHelper.log(e);
                }
            });
            inputThread.start();
            errorThread.start();

            // 5、等待结果
            int exitValue = process.waitFor();

            // 6、等待日志线程完成
            inputThread.join();
            errorThread.join();

            return exitValue;
        } catch (Exception e) {
            XxlJobHelper.log(e);
            return -1;
        } finally {
            // 7、关闭输出流
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    XxlJobHelper.log(e);
                }
            }
            // 8、中断线程
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
            }
            if (errorThread != null && errorThread.isAlive()) {
                errorThread.interrupt();
            }
            // 9、销毁进程
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 复制输入流到输出流
     */
    private static void copyStream(InputStream inputStream, FileOutputStream outputStream, boolean closeInput) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        } finally {
            if (closeInput) {
                inputStream.close();
            }
        }
    }

}
