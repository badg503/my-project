package com.lab.task;

import com.lab.entity.LabCheck;
import com.lab.entity.LabReserve;
import com.lab.service.LabCheckService;
import com.lab.service.LabReserveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 考勤自动签退定时任务
 * 预约结束后自动检查并标记未签退的记录为早退
 */
@Slf4j
@Component
public class AttendanceAutoCheckOutTask {

    private final LabCheckService checkService;
    private final LabReserveService reserveService;

    public AttendanceAutoCheckOutTask(LabCheckService checkService, LabReserveService reserveService) {
        this.checkService = checkService;
        this.reserveService = reserveService;
    }

    /**
     * 每 5 分钟执行一次，检查并自动签退未签退的记录
     */
    @Scheduled(fixedRate = 300000)
    public void autoCheckOutTask() {
        log.info("开始执行考勤自动签退任务...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        try {
            // 查询所有已审核通过的预约（包括今天和之前的）
            List<LabReserve> reserves = reserveService.lambdaQuery()
                    .eq(LabReserve::getStatus, "APPROVED")
                    .le(LabReserve::getReserveDate, today)
                    .list();
            
            int autoCheckOutCount = 0;
            int emailSentCount = 0;
            
            for (LabReserve reserve : reserves) {
                // 解析预约结束时间
                LocalTime endTime = LocalTime.parse(reserve.getTimeSlotEnd());
                LocalDateTime reserveEndTime = LocalDateTime.of(today, endTime);
                
                // 只处理已经结束的预约（当前时间 > 预约结束时间）
                if (now.isAfter(reserveEndTime)) {
                    // 检查该预约是否有考勤记录
                    LabCheck check = checkService.lambdaQuery()
                            .eq(LabCheck::getReserveId, reserve.getId())
                            .one();
                    
                    if (check != null) {
                        // 情况 1：已签到但未签退 → 标记为早退
                        if ("PRESENT".equals(check.getStatus()) && check.getCheckOutTime() == null) {
                            // 自动填写签退时间
                            check.setCheckOutTime(now);
                            // 标记为早退
                            check.setCheckOutStatus("EARLY_LEAVE");
                            
                            // 检查设备电源状态
                            boolean deviceStillRunning = checkDevicePowerStatus(reserve.getLabId());
                            
                            if (deviceStillRunning) {
                                // 设备未关闭，发送邮件提醒
                                sendDeviceNotClosedEmail(check.getUserId(), reserve);
                                emailSentCount++;
                                log.info("自动签退 + 邮件提醒：用户 ID={}, 预约 ID={}, 实验室 ID={}, 原因：预约结束未签退且设备未关", 
                                        check.getUserId(), reserve.getId(), reserve.getLabId());
                            } else {
                                log.info("自动签退：用户 ID={}, 预约 ID={}, 实验室 ID={}, 原因：预约结束未签退但设备已关", 
                                        check.getUserId(), reserve.getId(), reserve.getLabId());
                            }
                            
                            checkService.updateById(check);
                            autoCheckOutCount++;
                        }
                        // 情况 2：未签到也未签退 → 标记为缺勤
                        else if ("ABSENT".equals(check.getStatus()) && check.getCheckInTime() == null && check.getCheckOutTime() == null) {
                            // 标记为缺勤
                            check.setCheckInStatus("ABSENCE");
                            check.setCheckOutStatus("ABSENCE");
                            
                            log.info("自动标记缺勤：用户 ID={}, 预约 ID={}, 实验室 ID={}, 原因：预约结束未签到且未签退", 
                                    check.getUserId(), reserve.getId(), reserve.getLabId());
                            
                            checkService.updateById(check);
                            autoCheckOutCount++;
                        }
                    }
                }
            }
            
            log.info("考勤自动签退任务执行完成，自动签退记录数：{}, 发送邮件数：{}", autoCheckOutCount, emailSentCount);
            
        } catch (Exception e) {
            log.error("考勤自动签退任务执行失败", e);
        }
    }
    
    /**
     * 检查实验室设备电源状态
     * @param labId 实验室 ID
     * @return true-设备仍在运转，false-设备已关闭
     */
    private boolean checkDevicePowerStatus(Long labId) {
        // TODO: 调用物联网接口检查设备电源状态
        // 这里暂时返回 false，实际应该调用 IoT 设备接口
        return false;
    }
    
    /**
     * 发送设备未关闭提醒邮件
     * @param userId 用户 ID
     * @param reserve 预约记录
     */
    private void sendDeviceNotClosedEmail(Long userId, LabReserve reserve) {
        // TODO: 实现邮件发送逻辑
        log.warn("发送设备未关提醒邮件：用户 ID={}, 预约 ID={}, 实验室 ID={}", userId, reserve.getId(), reserve.getLabId());
    }
}
