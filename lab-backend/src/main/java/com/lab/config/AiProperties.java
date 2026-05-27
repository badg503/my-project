package com.lab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块外部服务配置，用于对接 Python 推理服务（LSTM/YOLOv8/BERT 等）。
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    /** 是否启用外部 AI 服务（不启用则使用内置知识库/占位逻辑） */
    private boolean enabled = true;
    /** Python 服务根地址，如 http://localhost:5000 */
    private String baseUrl = "http://localhost:5000";
    private String qaEndpoint = "/ai/qa";
    private String faultPredictEndpoint = "/ai/fault-predict";
    private String safetyDetectEndpoint = "/ai/safety-detect";
    private String scheduleEndpoint = "/ai/schedule";
    private String analysisEndpoint = "/ai/analysis";
    /** 调用超时毫秒 */
    private int timeoutMs = 10000;
    
    // Getter 方法
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getQaEndpoint() {
        return qaEndpoint;
    }
    
    public String getFaultPredictEndpoint() {
        return faultPredictEndpoint;
    }
    
    public String getSafetyDetectEndpoint() {
        return safetyDetectEndpoint;
    }
    
    public String getScheduleEndpoint() {
        return scheduleEndpoint;
    }
    
    public String getAnalysisEndpoint() {
        return analysisEndpoint;
    }
    
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    // Setter 方法
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public void setQaEndpoint(String qaEndpoint) {
        this.qaEndpoint = qaEndpoint;
    }
    
    public void setFaultPredictEndpoint(String faultPredictEndpoint) {
        this.faultPredictEndpoint = faultPredictEndpoint;
    }
    
    public void setSafetyDetectEndpoint(String safetyDetectEndpoint) {
        this.safetyDetectEndpoint = safetyDetectEndpoint;
    }
    
    public void setScheduleEndpoint(String scheduleEndpoint) {
        this.scheduleEndpoint = scheduleEndpoint;
    }
    
    public void setAnalysisEndpoint(String analysisEndpoint) {
        this.analysisEndpoint = analysisEndpoint;
    }
    
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}