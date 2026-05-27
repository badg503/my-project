package com.lab.util;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码工具类
 * 生成和验证 6 位数字验证码
 */
public class EmailCodeUtil {
    
    // 验证码缓存：key=email, value=CodeInfo
    private static final ConcurrentHashMap<String, CodeInfo> codeCache = new ConcurrentHashMap<>();
    
    // 验证码有效期（5 分钟）
    private static final long CODE_EXPIRE_MINUTES = 5;
    
    // 发送间隔（60 秒）
    private static final long SEND_INTERVAL_SECONDS = 60;
    
    /**
     * 生成 6 位数字验证码
     */
    public static String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /**
     * 保存验证码到缓存
     */
    public static void saveCode(String email, String code) {
        CodeInfo codeInfo = new CodeInfo(code, System.currentTimeMillis());
        codeCache.put(email, codeInfo);
    }
    
    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 验证码
     * @return true-验证成功，false-验证失败
     */
    public static boolean verifyCode(String email, String code) {
        CodeInfo codeInfo = codeCache.get(email);
        if (codeInfo == null) {
            return false;
        }
        
        // 检查是否过期
        long currentTime = System.currentTimeMillis();
        long expireTime = codeInfo.getCreateTime() + TimeUnit.MINUTES.toMillis(CODE_EXPIRE_MINUTES);
        if (currentTime > expireTime) {
            codeCache.remove(email);
            return false;
        }
        
        // 检查验证码是否正确
        if (!codeInfo.getCode().equals(code)) {
            // 验证失败次数 +1
            codeInfo.incrementFailCount();
            if (codeInfo.getFailCount() >= 5) {
                // 超过 5 次，验证码失效
                codeCache.remove(email);
            }
            return false;
        }
        
        // 验证成功，清除验证码
        codeCache.remove(email);
        return true;
    }
    
    /**
     * 检查是否可以发送验证码（距离上次发送是否超过 60 秒）
     */
    public static boolean canSendCode(String email) {
        CodeInfo codeInfo = codeCache.get(email);
        if (codeInfo == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastSendTime = codeInfo.getCreateTime();
        long interval = TimeUnit.SECONDS.toMillis(SEND_INTERVAL_SECONDS);
        
        return (currentTime - lastSendTime) >= interval;
    }
    
    /**
     * 清除验证码
     */
    public static void clearCode(String email) {
        codeCache.remove(email);
    }
    
    /**
     * 验证码信息内部类
     */
    private static class CodeInfo {
        private final String code;
        private final long createTime;
        private int failCount = 0;
        
        public CodeInfo(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }
        
        public String getCode() {
            return code;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public int getFailCount() {
            return failCount;
        }
        
        public void incrementFailCount() {
            this.failCount++;
        }
    }
}
