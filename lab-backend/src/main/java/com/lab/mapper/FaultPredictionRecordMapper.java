package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.FaultPredictionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故障预测记录 Mapper
 */
@Mapper
public interface FaultPredictionRecordMapper extends BaseMapper<FaultPredictionRecord> {
}
