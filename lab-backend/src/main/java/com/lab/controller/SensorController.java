package com.lab.controller;

import com.lab.common.Result;
import com.lab.entity.SensorConfig;
import com.lab.entity.SensorReading;
import com.lab.service.SensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 传感器管理控制器
 */
@RestController
@RequestMapping("/sensor")
public class SensorController {
    
    @Autowired
    private SensorService sensorService;
    
    /**
     * 获取设备的传感器配置
     */
    @GetMapping("/config/{deviceId}")
    public Result<SensorConfig> getDeviceConfig(@PathVariable Long deviceId) {
        SensorConfig config = sensorService.getDeviceSensorConfig(deviceId);
        return config != null ? Result.ok(config) : Result.fail("未找到传感器配置");
    }
    
    /**
     * 获取实验室的传感器配置列表
     */
    @GetMapping("/config/lab/{labId}")
    public Result<List<SensorConfig>> getLabConfigs(@PathVariable Long labId) {
        List<SensorConfig> configs = sensorService.getLabSensorConfigs(labId);
        return Result.ok(configs);
    }
    
    /**
     * 更新传感器配置
     */
    @PostMapping("/config/update")
    public Result<Boolean> updateConfig(@RequestBody SensorConfig config) {
        boolean success = sensorService.updateSensorConfig(config);
        return success ? Result.ok(true) : Result.fail("更新失败");
    }
    
    /**
     * 添加传感器配置
     */
    @PostMapping("/config/add")
    public Result<Boolean> addConfig(@RequestBody SensorConfig config) {
        boolean success = sensorService.addSensorConfig(config);
        return success ? Result.ok(true) : Result.fail("添加失败");
    }
    
    /**
     * 删除传感器配置
     */
    @DeleteMapping("/config/delete/{id}")
    public Result<Boolean> deleteConfig(@PathVariable Long id) {
        boolean success = sensorService.deleteSensorConfig(id);
        return success ? Result.ok(true) : Result.fail("删除失败");
    }
    
    /**
     * 获取设备的传感器读数历史
     */
    @GetMapping("/reading/{deviceId}")
    public Result<List<SensorReading>> getReadingHistory(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "30") Integer days) {
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        List<SensorReading> readings = sensorService.getDeviceSensorHistory(deviceId, startTime, endTime);
        return Result.ok(readings);
    }
    
    /**
     * 保存传感器读数（供数据采集服务调用）
     */
    @PostMapping("/reading/save")
    public Result<Boolean> saveReading(@RequestBody SensorReading reading) {
        sensorService.saveSensorReading(reading);
        return Result.ok(true);
    }
    
    /**
     * 批量保存传感器读数
     */
    @PostMapping("/reading/saveBatch")
    public Result<Boolean> saveReadings(@RequestBody List<SensorReading> readings) {
        sensorService.saveSensorReadings(readings);
        return Result.ok(true);
    }
    
    /**
     * 获取设备的传感器配置（Python 服务调用）
     */
    @GetMapping("/config/python/{deviceId}")
    public Result<Map<String, Object>> getConfigForPython(@PathVariable Long deviceId) {
        Map<String, Object> config = sensorService.getSensorConfigForPython(deviceId);
        return Result.ok(config);
    }
}
