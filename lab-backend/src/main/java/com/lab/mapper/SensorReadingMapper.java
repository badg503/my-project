package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.SensorReading;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器读数 Mapper
 */
@Mapper
public interface SensorReadingMapper extends BaseMapper<SensorReading> {
    
    /**
     * 查询设备的历史读数
     */
    List<SensorReading> selectByDeviceIdAndTimeRange(
        @Param("deviceId") Long deviceId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
