package com.lab.aspect;

import com.lab.annotation.LogOperation;
import com.lab.entity.OperationLog;
import com.lab.service.OperationLogService;
import com.lab.util.ServletUtils;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 */
@Aspect
@Component
public class LogOperationAspect {

    @Resource
    private OperationLogService operationLogService;

    @Around("@annotation(com.lab.annotation.LogOperation)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        LogOperation logOperation = method.getAnnotation(LogOperation.class);
        
        // 创建操作日志对象
        OperationLog log = new OperationLog();
        log.setModule(logOperation.module());
        log.setOperationType(logOperation.type());
        log.setDescription(logOperation.value());
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            log.setRequestMethod(attributes.getRequest().getMethod());
            log.setRequestUrl(attributes.getRequest().getRequestURI());
            log.setIp(ServletUtils.getClientIp(attributes.getRequest()));
            log.setRequestParams(ServletUtils.getRequestParams(attributes.getRequest()));
            
            // 从请求属性中获取用户信息（由 JwtAuthenticationFilter 设置）
            Object userId = attributes.getRequest().getAttribute("userId");
            Object userName = attributes.getRequest().getAttribute("userName");
            if (userId != null) {
                log.setUserId((Long) userId);
            }
            if (userName != null) {
                log.setUserName((String) userName);
            }
        }
        
        String status = "SUCCESS";
        String errorMsg = null;
        
        try {
            // 执行方法
            Object result = point.proceed();
            return result;
        } catch (Exception e) {
            status = "FAIL";
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            log.setCostTime(endTime - startTime);
            log.setStatus(status);
            log.setErrorMsg(errorMsg);
            log.setCreateTime(LocalDateTime.now());
            
            // 异步保存日志
            saveLogAsync(log);
        }
    }
    
    @Async("logExecutor")
    protected void saveLogAsync(OperationLog log) {
        try {
            operationLogService.save(log);
        } catch (Exception e) {
            System.err.println("保存操作日志失败：" + e.getMessage());
        }
    }
}
