package com.lab.service;

import com.lab.util.EmailCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 邮件服务类
 * 用于发送验证码邮件
 */
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * 发送重置密码验证码（异步）
     * @param email 收件人邮箱
     * @param code 验证码
     */
    @Async("emailExecutor")
    public void sendResetPasswordCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("开放实验室管理系统 <" + fromEmail + ">");
        message.setTo(email);
        message.setSubject("【开放实验室管理系统】重置密码验证码");
        message.setText(String.format(
            "尊敬的用戶：\n\n" +
            "您正在申请重置密码，验证码为：%s\n\n" +
            "验证码有效期为 5 分钟，请尽快使用。\n\n" +
            "如非本人操作，请忽略此邮件。\n\n" +
            "开放实验室管理系统\n" +
            "%s",
            code,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
        
        mailSender.send(message);
    }
    
    /**
     * 生成并发送验证码
     * @param email 收件人邮箱
     * @return 验证码
     */
    public String generateAndSendCode(String email) {
        // 生成验证码
        String code = EmailCodeUtil.generateCode();
        
        // 保存到缓存
        EmailCodeUtil.saveCode(email, code);
        
        // 发送邮件
        sendResetPasswordCode(email, code);
        
        return code;
    }
    
    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 验证码
     * @return true-验证成功，false-验证失败
     */
    public boolean verifyCode(String email, String code) {
        return EmailCodeUtil.verifyCode(email, code);
    }
    
    /**
     * 检查是否可以发送验证码
     * @param email 邮箱
     * @return true-可以发送，false-发送过于频繁
     */
    public boolean canSendCode(String email) {
        return EmailCodeUtil.canSendCode(email);
    }
    
    /**
     * 发送自定义邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendEmail(String toEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("开放实验室管理系统 <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
    
    /**
     * 发送安全告警邮件（异步）
     * @param toEmail 收件人邮箱
     * @param labName 实验室名称
     * @param alertType 告警类型
     * @param description 告警描述
     * @param alertTime 告警时间
     */
    @Async("emailExecutor")
    public void sendSafetyAlertEmail(String toEmail, String labName, String alertType, 
                                     String description, LocalDateTime alertTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("开放实验室管理系统 <" + fromEmail + ">");
        message.setTo(toEmail);
        message.setSubject("【安全告警】开放实验室管理系统检测到危险行为");
        message.setText(String.format(
            "尊敬的管理员：\n\n" +
            "系统检测到以下危险行为，请立即处理！\n\n" +
            "📍 实验室：%s\n" +
            "⚠️ 告警类型：%s\n" +
            "📝 详细描述：%s\n" +
            "⏰ 检测时间：%s\n\n" +
            "请及时登录系统查看详细信息并处理。\n\n" +
            "开放实验室管理系统\n" +
            "%s",
            labName != null ? labName : "未知实验室",
            alertType,
            description,
            alertTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
        
        mailSender.send(message);
    }
}
