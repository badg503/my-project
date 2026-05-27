package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.LabReserve;
import com.lab.entity.SensorReading;
import com.lab.entity.SysUser;
import com.lab.mapper.SensorReadingMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备电源检测服务
 */
@Service
public class DevicePowerCheckService {

    @Autowired
    private LabReserveService labReserveService;

    @Autowired
    private SensorReadingMapper sensorReadingMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private EmailService emailService;

    @Value("${device.power.check.threshold:0.1}")
    private Double currentThreshold; // 电流阈值，默认 0.1A

    @Value("${device.power.check.minutes:5}")
    private Integer checkMinutes; // 查询最近 N 分钟的读数

    /**
     * 检查预约设备的电源状态
     * @param reserveId 预约 ID
     * @return 未关闭的设备列表
     */
    public List<DevicePowerStatus> checkDevicePower(Long reserveId) {
        List<DevicePowerStatus> unclosedDevices = new ArrayList<>();

        // 1. 查询预约记录
        LabReserve reserve = labReserveService.getById(reserveId);
        if (reserve == null || reserve.getDeviceIds() == null) {
            return unclosedDevices;
        }

        // 2. 解析设备 ID 列表
        String[] deviceIdArray = reserve.getDeviceIds().split(",");
        
        // 3. 查询每个设备的电流读数
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkTime = now.minusMinutes(checkMinutes);

        for (String deviceIdStr : deviceIdArray) {
            Long deviceId = Long.parseLong(deviceIdStr.trim());
            
            // 查询最近 N 分钟内的最新读数
            LambdaQueryWrapper<SensorReading> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SensorReading::getDeviceId, deviceId)
                   .ge(SensorReading::getReadingTime, checkTime)
                   .orderByDesc(SensorReading::getReadingTime)
                   .last("LIMIT 1");
            
            SensorReading reading = sensorReadingMapper.selectOne(wrapper);
            
            // 4. 判断电流是否超过阈值
            boolean isPoweredOn = false;
            Double current = null;
            
            if (reading != null && reading.getCurrent() != null) {
                current = reading.getCurrent();
                isPoweredOn = current > currentThreshold;
            } else {
                // 没有读数，默认认为已关闭（避免误报）
                isPoweredOn = false;
            }
            
            if (isPoweredOn) {
                DevicePowerStatus status = new DevicePowerStatus();
                status.setDeviceId(deviceId);
                status.setCurrent(current);
                status.setReadingTime(reading != null ? reading.getReadingTime() : null);
                unclosedDevices.add(status);
            }
        }

        return unclosedDevices;
    }

    /**
     * 发送警告邮件给相关方
     * @param reserveId 预约 ID
     * @param unclosedDevices 未关闭的设备列表
     */
    public void sendWarningEmails(Long reserveId, List<DevicePowerStatus> unclosedDevices) {
        if (unclosedDevices.isEmpty()) {
            return;
        }

        // 查询预约信息
        LabReserve reserve = labReserveService.getById(reserveId);
        if (reserve == null) {
            return;
        }

        // 查询学生信息
        SysUser student = sysUserService.getById(reserve.getUserId());
        
        // 查询审核教师信息
        SysUser teacher = null;
        if (reserve.getAuditUserId() != null) {
            teacher = sysUserService.getById(reserve.getAuditUserId());
        }

        // 查询管理员（角色为 SYS_ADMIN 或 LAB_ADMIN）
        List<SysUser> admins = sysUserService.list().stream()
            .filter(u -> "SYS_ADMIN".equals(u.getRole()) || "LAB_ADMIN".equals(u.getRole()))
            .collect(Collectors.toList());

        // 构建邮件内容
        String deviceList = unclosedDevices.stream()
            .map(d -> "设备 ID: " + d.getDeviceId() + " (当前电流：" + String.format("%.2f", d.getCurrent()) + "A)")
            .collect(Collectors.joining("\n"));

        LocalDateTime checkTime = LocalDateTime.now();

        // 1. 发送给学生
        if (student != null && student.getEmail() != null) {
            String studentSubject = "【重要】实验室设备未关闭电源提醒";
            String studentContent = String.format(
                "亲爱的 %s 同学：\n\n" +
                "您好！\n\n" +
                "系统检测到您在 %s 的签退过程中，有以下设备未关闭电源：\n\n" +
                "%s\n\n" +
                "电流阈值标准：≤ %.2fA 视为关闭\n\n" +
                "为了实验室安全和节约能源，请您：\n" +
                "1. 立即返回实验室检查并关闭上述设备\n" +
                "2. 确保所有设备电源开关已关闭\n" +
                "3. 养成良好的实验习惯\n\n" +
                "感谢您的配合！\n\n" +
                "开放实验室管理系统\n" +
                "%s",
                student.getRealName(),
                checkTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                deviceList,
                currentThreshold,
                checkTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            emailService.sendEmail(student.getEmail(), studentSubject, studentContent);
        }

        // 2. 发送给教师
        if (teacher != null && teacher.getEmail() != null) {
            String teacherSubject = "【学生教育】您的学生签退时设备未关闭电源";
            String teacherContent = String.format(
                "尊敬的 %s 老师：\n\n" +
                "您好！\n\n" +
                "您的学生 %s（%s）在 %s 进行签退时，系统检测到有以下设备未关闭电源：\n\n" +
                "%s\n\n" +
                "电流阈值标准：≤ %.2fA 视为关闭\n\n" +
                "请您对该学生进行安全教育，提醒其：\n" +
                "1. 实验结束后必须关闭所有设备电源\n" +
                "2. 培养良好的实验室安全习惯\n" +
                "3. 注意实验室安全和能源节约\n\n" +
                "感谢您的配合！\n\n" +
                "开放实验室管理系统\n" +
                "%s",
                teacher.getRealName(),
                student.getRealName(),
                student.getUsername(),
                checkTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                deviceList,
                currentThreshold,
                checkTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            emailService.sendEmail(teacher.getEmail(), teacherSubject, teacherContent);
        }

        // 3. 发送给管理员
        for (SysUser admin : admins) {
            if (admin.getEmail() != null) {
                String adminSubject = "【设备检查】学生签退后设备未关闭，请查看";
                String adminContent = String.format(
                    "尊敬的管理员：\n\n" +
                    "您好！\n\n" +
                    "学生 %s（%s）在 %s 签退时，系统检测到有以下设备未关闭电源：\n\n" +
                    "%s\n\n" +
                    "电流阈值标准：≤ %.2fA 视为关闭\n\n" +
                    "学生信息：\n" +
                    "- 姓名：%s\n" +
                    "- 学号：%s\n" +
                    "- 预约 ID: %d\n\n" +
                    "系统已发送邮件提醒学生和教师。\n" +
                    "请您关注该学生是否返回实验室关闭设备电源。\n\n" +
                    "开放实验室管理系统\n" +
                    "%s",
                    student.getRealName(),
                    student.getUsername(),
                    checkTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    deviceList,
                    currentThreshold,
                    student.getRealName(),
                    student.getUsername(),
                    reserveId,
                    checkTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
                emailService.sendEmail(admin.getEmail(), adminSubject, adminContent);
            }
        }
    }

    /**
     * 设备电源状态
     */
    public static class DevicePowerStatus {
        private Long deviceId;
        private Double current;
        private LocalDateTime readingTime;

        public Long getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(Long deviceId) {
            this.deviceId = deviceId;
        }

        public Double getCurrent() {
            return current;
        }

        public void setCurrent(Double current) {
            this.current = current;
        }

        public LocalDateTime getReadingTime() {
            return readingTime;
        }

        public void setReadingTime(LocalDateTime readingTime) {
            this.readingTime = readingTime;
        }
    }
}
