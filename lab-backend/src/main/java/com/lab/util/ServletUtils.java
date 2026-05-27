package com.lab.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet 工具类
 */
public class ServletUtils {

    /**
     * 获取客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，第一个 IP 为真实 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        // 将 IPv6 转换为 IPv4 格式
        if (ip != null && ("0:0:0:0:0:0:0:1".equals(ip) || "0:0:0:0:0:0:0:01".equals(ip))) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * 获取请求参数
     */
    public static String getRequestParams(HttpServletRequest request) {
        try {
            ContentCachingRequestWrapper wrappedRequest = (ContentCachingRequestWrapper) request;
            byte[] content = wrappedRequest.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
            
            // GET 请求参数
            Map<String, String[]> paramMap = request.getParameterMap();
            if (paramMap != null && !paramMap.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    sb.append(entry.getKey()).append("=");
                    String[] values = entry.getValue();
                    if (values != null && values.length > 0) {
                        sb.append(String.join(",", values));
                    }
                    sb.append("&");
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "{}";
        }
        return "{}";
    }
}
