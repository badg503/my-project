package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.PredictionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 故障预测任务 Mapper
 */
@Mapper
public interface PredictionTaskMapper extends BaseMapper<PredictionTask> {
    
    /**
     * 根据任务 ID 查询
     */
    default PredictionTask selectByTaskId(@Param("taskId") String taskId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PredictionTask>()
            .eq(PredictionTask::getTaskId, taskId));
    }
    
    /**
     * 更新任务进度
     */
    @Update("UPDATE prediction_task SET processed_devices = processed_devices + 1, updated_at = #{endTime} WHERE task_id = #{taskId}")
    int incrementProgress(@Param("taskId") String taskId, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 更新任务状态
     */
    default int updateTaskStatus(@Param("taskId") String taskId, 
                                 @Param("status") String status,
                                 @Param("endTime") LocalDateTime endTime) {
        PredictionTask task = new PredictionTask();
        task.setStatus(status);
        task.setEndTime(endTime);
        task.setUpdatedAt(endTime);
        return update(task, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PredictionTask>()
            .eq(PredictionTask::getTaskId, taskId));
    }
}
