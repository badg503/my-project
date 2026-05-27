package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 统计学生的预约记录数量
     */
    @Select("SELECT COUNT(*) FROM lab_reserve WHERE user_id = #{userId}")
    long countReserveRecords(@Param("userId") Long userId);
    
    /**
     * 统计学生的考勤记录数量
     */
    @Select("SELECT COUNT(*) FROM lab_attendance WHERE user_id = #{userId}")
    long countAttendanceRecords(@Param("userId") Long userId);
    
    /**
     * 统计学生的实验报告数量
     */
    @Select("SELECT COUNT(*) FROM lab_report WHERE user_id = #{userId}")
    long countReportRecords(@Param("userId") Long userId);
    
    /**
     * 统计教师的师生关联数量
     */
    @Select("SELECT COUNT(*) FROM teacher_student WHERE teacher_id = #{teacherId}")
    long countTeacherStudentRecords(@Param("teacherId") Long teacherId);
    
    /**
     * 统计教师的实验任务数量
     */
    @Select("SELECT COUNT(*) FROM lab_task WHERE teacher_id = #{teacherId}")
    long countTaskRecords(@Param("teacherId") Long teacherId);
}