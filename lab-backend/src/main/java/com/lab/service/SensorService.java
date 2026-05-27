package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.SensorConfig;
import com.lab.entity.SensorReading;
import com.lab.mapper.SensorConfigMapper;
import com.lab.mapper.SensorReadingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感器服务
 */
@Service
public class SensorService {
    
    @Autowired
    private SensorConfigMapper sensorConfigMapper;
    
    @Autowired
    private SensorReadingMapper sensorReadingMapper;
    
    /**
     * 获取设备的传感器配置
     */
    public SensorConfig getDeviceSensorConfig(Long deviceId) {
        LambdaQueryWrapper<SensorConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensorConfig::getDeviceId, deviceId)
               .eq(SensorConfig::getEnabled, 1);
        return sensorConfigMapper.selectOne(wrapper);
    }
    
    /**
     * 获取实验室的所有传感器配置
     */
    public List<SensorConfig> getLabSensorConfigs(Long labId) {
        LambdaQueryWrapper<SensorConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensorConfig::getLabId, labId)
               .eq(SensorConfig::getEnabled, 1);
        return sensorConfigMapper.selectList(wrapper);
    }
    
    /**
     * 获取设备的传感器读数历史
     */
    public List<SensorReading> getDeviceSensorHistory(
            Long deviceId, 
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        return sensorReadingMapper.selectByDeviceIdAndTimeRange(deviceId, startTime, endTime);
    }
    
    /**
     * 保存传感器读数
     */
    public void saveSensorReading(SensorReading reading) {
        sensorReadingMapper.insert(reading);
    }
    
    /**
     * 批量保存传感器读数
     */
    public void saveSensorReadings(List<SensorReading> readings) {
        for (SensorReading reading : readings) {
            sensorReadingMapper.insert(reading);
        }
    }
    
    /**
     * 更新传感器配置
     */
    public boolean updateSensorConfig(SensorConfig config) {
        return sensorConfigMapper.updateById(config) > 0;
    }
    
    /**
     * 添加传感器配置
     */
    public boolean addSensorConfig(SensorConfig config) {
        return sensorConfigMapper.insert(config) > 0;
    }
    
    /**
     * 删除传感器配置
     */
    public boolean deleteSensorConfig(Long id) {
        return sensorConfigMapper.deleteById(id) > 0;
    }
    
    /**
     * 获取设备的传感器配置（转换为 Python 服务需要的格式）
     */
    public Map<String, Object> getSensorConfigForPython(Long deviceId) {
        SensorConfig config = getDeviceSensorConfig(deviceId);
        
        Map<String, Object> result = new HashMap<>();
        if (config == null) {
            result.put("mode", "virtual");
            result.put("sensor_config", null);
            return result;
        }
        
        result.put("mode", config.getMode());
        
        Map<String, Object> sensorConfig = new HashMap<>();
        sensorConfig.put("temp_sensor_ip", config.getTempSensorIp());
        sensorConfig.put("vibration_sensor_ip", config.getVibrationSensorIp());
        sensorConfig.put("current_sensor_ip", config.getCurrentSensorIp());
        sensorConfig.put("gateway_ip", config.getGatewayIp());
        sensorConfig.put("api_port", config.getApiPort());
        
        result.put("sensor_config", sensorConfig);
        return result;
    }
}
