package com.lab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.SysUser;
import com.lab.mapper.SysUserMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {
    
    @Cacheable(value = "user", key = "#username")
    public SysUser getByUsername(String username) {
        return lambdaQuery().eq(SysUser::getUsername, username).one();
    }
    
    @Cacheable(value = "user", key = "#email")
    public SysUser getByEmail(String email) {
        return lambdaQuery().eq(SysUser::getEmail, email).one();
    }
    
    @CacheEvict(value = "user", key = "#user.username")
    public boolean createUser(SysUser user) {
        return save(user);
    }
    
    @CacheEvict(value = "user", key = "#user.username")
    public boolean updateUser(SysUser user) {
        return updateById(user);
    }
    
    @CacheEvict(value = "user", key = "#id")
    public boolean deleteUser(Long id) {
        return removeById(id);
    }
    
    @CacheEvict(value = "user", allEntries = true)
    public void clearUserCache() {
    }
    
    /**
     * 检查学生的预约记录数量
     */
    public long countReserveRecords(Long userId) {
        return getBaseMapper().countReserveRecords(userId);
    }
    
    /**
     * 检查学生的考勤记录数量
     */
    public long countAttendanceRecords(Long userId) {
        return getBaseMapper().countAttendanceRecords(userId);
    }
    
    /**
     * 检查学生的实验报告数量
     */
    public long countReportRecords(Long userId) {
        return getBaseMapper().countReportRecords(userId);
    }
    
    /**
     * 检查教师的师生关联数量
     */
    public long countTeacherStudentRecords(Long teacherId) {
        return getBaseMapper().countTeacherStudentRecords(teacherId);
    }
    
    /**
     * 检查教师的实验任务数量
     */
    public long countTaskRecords(Long teacherId) {
        return getBaseMapper().countTaskRecords(teacherId);
    }
}
