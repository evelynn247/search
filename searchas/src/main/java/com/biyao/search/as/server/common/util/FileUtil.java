package com.biyao.search.as.server.common.util;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static boolean download(String urlStr, String fileName) {
        File file = new File(fileName + ".tmp");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error("[严重异常][文件异常]下载文件时创建本地文件失败！！url:{}, fileName:{}", urlStr, fileName, e);
                return false;
            }
        }
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[10240];

        try {
            url = new URL(urlStr);
            httpUrl = (HttpURLConnection) url.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            fos = new FileOutputStream(file);

            int size1;
            while ((size1 = bis.read(buf)) != -1) {
                fos.write(buf, 0, size1);
            }

            fos.close();
            bis.close();
            httpUrl.disconnect();

            logger.info("[操作日志]*********远程下载文件正常，url:{}, filename: {} *********", urlStr, fileName);
            file.renameTo(new File(fileName));
            return true;
        } catch (Exception e) {
            logger.error("[严重异常][文件异常]下载文件时发生异常！！url:{}, fileName:{}", urlStr, fileName, e);
            return false;
        }
    }

    public static String remoteRead(String urlStr) {
        StringBuilder result = new StringBuilder();
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        try {
            url = new URL(urlStr);
            httpUrl = (HttpURLConnection) url.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bis));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line).append(System.getProperty("line.separator"));
            }
        } catch (Exception e) {
            logger.error("[严重异常][文件异常]远程读取文件时发生异常！！url:{},异常：" ,e);
            return "";
        }

        return result.toString();
    }

    /**
     * 从网络读取文件
     *
     * @param destUrl
     * @return
     */
    public static List<String> getRemoteFile(String destUrl) throws Exception {
        List<String> lines = new ArrayList<>();
        File tmpDir = Files.createTempDir();
        String fileName = tmpDir + File.separator + String.valueOf(Math.random()) + (new Date()).getTime() + ".conf";
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[10240];
        int size = 0;
        try {
            // 建立链接
            url = new URL(destUrl);
            httpUrl = (HttpURLConnection) url.openConnection();
            // 连接指定的资源
            httpUrl.connect();
            // 获取网络输入流
            bis = new BufferedInputStream(httpUrl.getInputStream());
            // 建立文件
            fos = new FileOutputStream(fileName);

            // 保存文件
            while ((size = bis.read(buf)) != -1) {
                fos.write(buf, 0, size);
            }
            fos.close();
            bis.close();
            httpUrl.disconnect();

            lines = getFileFromLocal(fileName);
        } catch (Exception e) {
            throw new Exception("读取网络文件出错", e);
        }finally {
            if (tmpDir.exists()){
                try {
                    boolean deleted = tmpDir.delete();
                }catch (Exception e){
                    logger.error("[严重异常][文件异常]临时文件删除失败:tmpDir={}", tmpDir.getAbsolutePath(), e);
                }
            }
        }

        return lines;
    }

    /**
     * 从本地文件读取配置
     *
     * @param filePath
     * @return
     */
    public static List<String> getFileFromLocal(String filePath) throws Exception {
        List<String> lines = new ArrayList<>();

        File file = new File(filePath);

        try {
            lines = Files.readLines(file, Charsets.UTF_8);
        } catch (IOException e) {
            throw new Exception("从本地文件读取配置出错", e);
        }

        return lines;
    }
}
