package com.lab.utils;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP客户端工具类
 */
@Component
public class HttpClientUtil {
    
    /**
     * 发送GET请求
     */
    public static String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoInput(true);
        
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } finally {
            conn.disconnect();
        }
        
        return result.toString();
    }
    
    /**
     * 发送 POST 请求
     */
    public static String post(String urlStr, String data) throws Exception {
        System.out.println("开始发送 POST 请求：" + urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000); // 增加到 30 秒，给 AI 模型加载留足时间
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes("UTF-8"));
            os.flush();
            System.out.println("数据已发送");
        }
        
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP POST 响应码：" + responseCode);
        
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            System.out.println("响应内容长度：" + result.length());
        } catch (Exception e) {
            System.err.println("读取响应失败：" + e.getMessage());
            // 尝试读取错误流
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                StringBuilder errorResult = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResult.append(line);
                }
                System.err.println("错误响应：" + errorResult.toString());
            }
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
            conn.disconnect();
        }
        
        return result.toString();
    }
}
