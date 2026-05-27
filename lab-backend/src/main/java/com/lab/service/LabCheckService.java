package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabCheck;
import com.lab.entity.LabInfo;
import com.lab.entity.LabReserve;
import com.lab.mapper.LabCheckMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabCheckService extends ServiceImpl<LabCheckMapper, LabCheck> {

    @Resource
    private LabInfoService labInfoService;

    @Resource
    private LabReserveService labReserveService;

    @Resource
    private DevicePowerCheckService devicePowerCheckService;

    public Page<LabCheck> pageByUser(Page<LabCheck> page, Long userId, Long labId) {
        LambdaQueryWrapper<LabCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, LabCheck::getUserId, userId)
               .eq(labId != null, LabCheck::getLabId, labId)
               .orderByDesc(LabCheck::getCreateTime);
        Page<LabCheck> result = page(page, wrapper);
        fillExtraInfo(result.getRecords());
        return result;
    }

    public Page<LabCheck> pageAll(Page<LabCheck> page, Long labId, Long userId) {
        LambdaQueryWrapper<LabCheck> q = new LambdaQueryWrapper<>();
        q.eq(labId != null, LabCheck::getLabId, labId).eq(userId != null, LabCheck::getUserId, userId).orderByDesc(LabCheck::getCreateTime);
        Page<LabCheck> result = page(page, q);
        fillExtraInfo(result.getRecords());
        return result;
    }

    private void fillExtraInfo(List<LabCheck> checks) {
        if (checks.isEmpty()) return;

        List<Long> labIds = checks.stream().map(LabCheck::getLabId).distinct().collect(Collectors.toList());
        List<Long> reserveIds = checks.stream().map(LabCheck::getReserveId).filter(id -> id != null).distinct().collect(Collectors.toList());

        if (!labIds.isEmpty()) {
            List<LabInfo> labs = labInfoService.listByIds(labIds);
            Map<Long, String> labNameMap = labs.stream().collect(Collectors.toMap(LabInfo::getId, LabInfo::getName));
            for (LabCheck check : checks) {
                check.setLabName(labNameMap.get(check.getLabId()));
            }
        }

        if (!reserveIds.isEmpty()) {
            List<LabReserve> reserves = labReserveService.listByIds(reserveIds);
            Map<Long, LabReserve> reserveMap = reserves.stream().collect(Collectors.toMap(LabReserve::getId, r -> r));
            for (LabCheck check : checks) {
                LabReserve reserve = reserveMap.get(check.getReserveId());
                if (reserve != null) {
                    String time = reserve.getReserveDate() + " " + reserve.getTimeSlotStart() + "-" + reserve.getTimeSlotEnd();
                    check.setReserveTime(time);
                }
            }
        }
    }

    /**
     * 签退时检查设备电源
     * @param checkId 考勤记录 ID
     * @return 检查结果
     */
    public DevicePowerCheckResult checkOutWithPowerCheck(Long checkId) {
        DevicePowerCheckResult result = new DevicePowerCheckResult();
        result.setSuccess(true);

        // 查询考勤记录
        LabCheck check = getById(checkId);
        if (check == null || check.getReserveId() == null) {
            result.setSuccess(false);
            result.setMessage("考勤记录不存在");
            return result;
        }

        // 检查设备电源
        List<DevicePowerCheckService.DevicePowerStatus> unclosedDevices = 
            devicePowerCheckService.checkDevicePower(check.getReserveId());

        if (unclosedDevices.isEmpty()) {
            result.setMessage("所有设备已关闭，可以签退");
        } else {
            result.setSuccess(false);
            result.setHasUnclosedDevices(true);
            result.setUnclosedDevices(unclosedDevices);
            
            StringBuilder sb = new StringBuilder();
            sb.append("检测到以下设备未关闭电源：\n");
            for (DevicePowerCheckService.DevicePowerStatus device : unclosedDevices) {
                sb.append("设备 ID: ").append(device.getDeviceId())
                  .append(" (当前电流：").append(String.format("%.2f", device.getCurrent())).append("A)\n");
            }
            sb.append("\n请现场检查并关闭设备后再签退！");
            result.setMessage(sb.toString());
        }

        return result;
    }

    /**
     * 二次签退（设备未关闭但学生确认已关闭）
     * @param checkId 考勤记录 ID
     * @param reserve 预约记录
     */
    public void sendDeviceNotClosedEmail(Long userId, LabReserve reserve) {
        // 检查设备电源
        List<DevicePowerCheckService.DevicePowerStatus> unclosedDevices = 
            devicePowerCheckService.checkDevicePower(reserve.getId());

        if (!unclosedDevices.isEmpty()) {
            // 发送邮件提醒
            devicePowerCheckService.sendWarningEmails(reserve.getId(), unclosedDevices);
        }
    }

    /**
     * 设备电源检查结果
     */
    public static class DevicePowerCheckResult {
        private boolean success;
        private String message;
        private boolean hasUnclosedDevices;
        private List<DevicePowerCheckService.DevicePowerStatus> unclosedDevices;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isHasUnclosedDevices() {
            return hasUnclosedDevices;
        }

        public void setHasUnclosedDevices(boolean hasUnclosedDevices) {
            this.hasUnclosedDevices = hasUnclosedDevices;
        }

        public List<DevicePowerCheckService.DevicePowerStatus> getUnclosedDevices() {
            return unclosedDevices;
        }

        public void setUnclosedDevices(List<DevicePowerCheckService.DevicePowerStatus> unclosedDevices) {
            this.unclosedDevices = unclosedDevices;
        }
        
        /**
         * 设备是否仍在运转（用于 Controller 判断）
         */
        public boolean isDeviceRunning() {
            return hasUnclosedDevices && unclosedDevices != null && !unclosedDevices.isEmpty();
        }
    }
}
