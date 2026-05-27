package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.annotation.LogOperation;
import com.lab.common.Result;
import com.lab.entity.ClassInfo;
import com.lab.entity.Department;
import com.lab.entity.SysUser;
import com.lab.entity.TeacherStudent;
import com.lab.service.ClassService;
import com.lab.service.DepartmentService;
import com.lab.service.SysUserService;
import com.lab.service.TeacherStudentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    private final SysUserService sysUserService;
    private final TeacherStudentService teacherStudentService;
    private final ClassService classService;
    private final DepartmentService departmentService;

    public UserController(SysUserService sysUserService, TeacherStudentService teacherStudentService,
                         ClassService classService, DepartmentService departmentService) {
        this.sysUserService = sysUserService;
        this.teacherStudentService = teacherStudentService;
        this.classService = classService;
        this.departmentService = departmentService;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String role,
                                            @RequestParam(required = false) String keyword) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> result = sysUserService.page(page, wrapper);
        
        // 处理班级、专业、院系信息
        List<Map<String, Object>> recordsWithInfo = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("realName", user.getRealName());
            userMap.put("role", user.getRole());
            userMap.put("phone", user.getPhone());
            userMap.put("email", user.getEmail());
            userMap.put("gender", user.getGender());
            userMap.put("status", user.getStatus());
            userMap.put("createTime", user.getCreateTime());
            
            // 处理学生的班级、专业、院系信息
            if ("STUDENT".equals(user.getRole()) && user.getClassId() != null) {
                ClassInfo classInfo = classService.getById(user.getClassId());
                if (classInfo != null) {
                    userMap.put("className", classInfo.getClassName());
                    if (classInfo.getDepartmentId() != null) {
                        Department department = departmentService.getById(classInfo.getDepartmentId());
                        if (department != null) {
                            userMap.put("departmentName", department.getDepartmentName());
                        }
                    }
                }
            }
            
            recordsWithInfo.add(userMap);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", recordsWithInfo);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        
        return Result.ok(data);
    }

    @PostMapping
    @LogOperation(module = "用户管理", type = "新增", value = "新增用户")
    public Result<Void> add(@RequestBody SysUser user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return Result.fail("账号不能为空");
        }
        
        // 检查用户名是否已存在（包括已删除的）- 使用 wrapper 关闭逻辑删除过滤
        SysUser existing = sysUserService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                        .eq("username", user.getUsername())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return Result.fail("账号已存在");
        }
        
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        
        sysUserService.save(user);
        return Result.ok();
    }

    @PutMapping
    @LogOperation(module = "用户管理", type = "修改", value = "修改用户信息")
    public Result<Void> update(@RequestBody SysUser user) {
        if (user.getId() == null) {
            return Result.fail("用户ID不能为空");
        }
        
        SysUser existing = sysUserService.getById(user.getId());
        if (existing == null) {
            return Result.fail("用户不存在");
        }
        
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setAvatar(user.getAvatar());
        existing.setGender(user.getGender());
        existing.setMajor(user.getMajor());
        existing.setDepartment(user.getDepartment());
        if (user.getClassId() != null) {
            existing.setClassId(user.getClassId());
        }
        if (user.getStatus() != null) {
            existing.setStatus(user.getStatus());
        }
        
        sysUserService.updateById(existing);
        return Result.ok();
    }

    @PutMapping("/profile")
    @LogOperation(module = "用户管理", type = "修改", value = "修改个人信息")
    public Result<Void> updateProfile(@RequestBody SysUser user, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        
        SysUser existing = sysUserService.getById(userId);
        if (existing == null) {
            return Result.fail("用户不存在");
        }
        
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setGender(user.getGender());
        existing.setMajor(user.getMajor());
        existing.setDepartment(user.getDepartment());
        if (user.getClassId() != null) {
            existing.setClassId(user.getClassId());
        }
        
        sysUserService.updateById(existing);
        return Result.ok();
    }

    @PutMapping("/change-password")
    @LogOperation(module = "用户管理", type = "修改", value = "修改密码")
    public Result<Void> changePassword(@RequestBody Map<String, String> params, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.fail("原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.fail("新密码不能为空");
        }
        
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 验证原密码
        if (!user.getPassword().equals(oldPassword)) {
            return Result.fail("原密码错误");
        }
        
        user.setPassword(newPassword);
        sysUserService.updateById(user);
        
        // 清除用户缓存，确保新密码立即生效
        sysUserService.clearUserCache();
        
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "用户管理", type = "删除", value = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 通过 user 的 role 判断需要检查哪些表
        String role = user.getRole();
        
        if ("STUDENT".equals(role)) {
            // 学生：检查预约记录
            long studentReserveCount = sysUserService.countReserveRecords(id);
            if (studentReserveCount > 0) {
                return Result.fail("该用户存在" + studentReserveCount + "条预约记录，无法删除！");
            }
            
            // 检查考勤记录
            long attendanceCount = sysUserService.countAttendanceRecords(id);
            if (attendanceCount > 0) {
                return Result.fail("该用户存在" + attendanceCount + "条考勤记录，无法删除！");
            }
            
            // 检查实验报告
            long reportCount = sysUserService.countReportRecords(id);
            if (reportCount > 0) {
                return Result.fail("该用户存在" + reportCount + "条实验报告，无法删除！");
            }
        } else if ("TEACHER".equals(role)) {
            // 教师：检查师生关联
            long teacherStudentCount = sysUserService.countTeacherStudentRecords(id);
            if (teacherStudentCount > 0) {
                return Result.fail("该教师存在" + teacherStudentCount + "个关联学生，无法删除！");
            }
            
            // 检查实验任务
            long taskCount = sysUserService.countTaskRecords(id);
            if (taskCount > 0) {
                return Result.fail("该教师存在" + taskCount + "个实验任务，无法删除！");
            }
        }
        
        // 通过所有检查，执行删除
        sysUserService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam Long userId, @RequestParam String newPassword) {
        log.info("重置密码请求: userId={}", userId);
        
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("未找到用户: userId={}", userId);
            return Result.fail("用户不存在");
        }
        
        user.setPassword(newPassword);
        boolean result = sysUserService.updateById(user);
        log.info("密码更新结果: {}", result);
        return Result.ok();
    }

    @GetMapping("/teachers")
    public Result<List<SysUser>> getTeachers() {
        List<SysUser> list = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "TEACHER")
                .list();
        return Result.ok(list);
    }

    @GetMapping("/students")
    public Result<List<Map<String, Object>>> getStudents() {
        List<SysUser> students = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "STUDENT")
                .orderByAsc(SysUser::getId)
                .list();
        
        // 添加班级名称
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (SysUser student : students) {
            Map<String, Object> studentMap = new java.util.HashMap<>();
            studentMap.put("id", student.getId());
            studentMap.put("username", student.getUsername());
            studentMap.put("realName", student.getRealName());
            studentMap.put("phone", student.getPhone());
            studentMap.put("email", student.getEmail());
            studentMap.put("gender", student.getGender());
            studentMap.put("classId", student.getClassId());
            studentMap.put("status", student.getStatus());
            studentMap.put("createTime", student.getCreateTime());
            
            if (student.getClassId() != null) {
                ClassInfo classInfo = classService.getById(student.getClassId());
                if (classInfo != null) {
                    studentMap.put("className", classInfo.getClassName());
                }
            }
            
            result.add(studentMap);
        }
        
        return Result.ok(result);
    }

    @GetMapping("/classes")
    public Result<List<ClassInfo>> getClasses() {
        List<ClassInfo> list = classService.list();
        return Result.ok(list);
    }

    @GetMapping("/departments")
    public Result<List<Department>> getDepartments() {
        List<Department> list = departmentService.list();
        return Result.ok(list);
    }

    @PostMapping("/assign-students")
    public Result<Void> assignStudents(@RequestBody Map<String, Object> data) {
        Long teacherId = Long.valueOf(data.get("teacherId").toString());
        @SuppressWarnings("unchecked")
        List<Object> studentIdsObj = (List<Object>) data.get("studentIds");
        List<Long> studentIds = studentIdsObj.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());

        for (Long sid : studentIds) {
            TeacherStudent existing = teacherStudentService.lambdaQuery()
                    .eq(TeacherStudent::getTeacherId, teacherId)
                    .eq(TeacherStudent::getStudentId, sid)
                    .one();
            if (existing != null) {
                continue;
            }
            
            TeacherStudent ts = new TeacherStudent();
            ts.setTeacherId(teacherId);
            ts.setStudentId(sid);
            teacherStudentService.save(ts);
        }
        return Result.ok();
    }

    @PostMapping("/unassign-students")
    public Result<Void> unassignStudents(@RequestBody Map<String, Object> data) {
        Long teacherId = Long.valueOf(data.get("teacherId").toString());
        @SuppressWarnings("unchecked")
        List<Object> studentIdsObj = (List<Object>) data.get("studentIds");
        List<Long> studentIds = studentIdsObj.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());

        for (Long sid : studentIds) {
            TeacherStudent ts = teacherStudentService.lambdaQuery()
                    .eq(TeacherStudent::getTeacherId, teacherId)
                    .eq(TeacherStudent::getStudentId, sid)
                    .one();
            if (ts != null) {
                teacherStudentService.removeById(ts.getId());
            }
        }
        return Result.ok();
    }

    @GetMapping("/assigned-students")
    public Result<List<Map<String, Object>>> getAssignedStudents(@RequestParam Long teacherId) {
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        
        if (relations.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        
        List<Long> studentIds = relations.stream()
                .map(TeacherStudent::getStudentId)
                .collect(Collectors.toList());
        
        List<SysUser> students = sysUserService.lambdaQuery()
                .in(SysUser::getId, studentIds)
                .eq(SysUser::getRole, "STUDENT")
                .list();
        
        // 添加班级名称
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (SysUser student : students) {
            Map<String, Object> studentMap = new java.util.HashMap<>();
            studentMap.put("id", student.getId());
            studentMap.put("username", student.getUsername());
            studentMap.put("realName", student.getRealName());
            studentMap.put("phone", student.getPhone());
            studentMap.put("email", student.getEmail());
            studentMap.put("gender", student.getGender());
            studentMap.put("classId", student.getClassId());
            
            if (student.getClassId() != null) {
                ClassInfo classInfo = classService.getById(student.getClassId());
                if (classInfo != null) {
                    studentMap.put("className", classInfo.getClassName());
                }
            }
            
            result.add(studentMap);
        }
        
        return Result.ok(result);
    }

    @GetMapping("/unassigned-students")
    public Result<List<SysUser>> getUnassignedStudents(@RequestParam Long teacherId) {
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        
        List<Long> assignedIds = relations.stream()
                .map(TeacherStudent::getStudentId)
                .collect(Collectors.toList());
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, "STUDENT");
        if (!assignedIds.isEmpty()) {
            wrapper.notIn(SysUser::getId, assignedIds);
        }
        
        List<SysUser> students = sysUserService.list(wrapper);
        return Result.ok(students);
    }

    @GetMapping("/role/{role}")
    public Result<List<SysUser>> getByRole(@PathVariable String role) {
        List<SysUser> list = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, role)
                .list();
        list.forEach(u -> u.setPassword(null));
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setPassword(null);
        return Result.ok(user);
    }
}
