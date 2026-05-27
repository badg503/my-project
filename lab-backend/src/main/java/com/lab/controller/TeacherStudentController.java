package com.lab.controller;

import com.lab.common.Result;
import com.lab.entity.ClassInfo;
import com.lab.entity.SysUser;
import com.lab.entity.TeacherStudent;
import com.lab.annotation.LogOperation;
import com.lab.service.ClassService;
import com.lab.service.SysUserService;
import com.lab.service.TeacherStudentService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher-student")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;
    private final SysUserService sysUserService;
    private final ClassService classService;

    public TeacherStudentController(TeacherStudentService teacherStudentService, 
                                   SysUserService sysUserService,
                                   ClassService classService) {
        this.teacherStudentService = teacherStudentService;
        this.sysUserService = sysUserService;
        this.classService = classService;
    }

    @GetMapping("/my-students")
    public Result<List<Map<String, Object>>> getMyStudents(HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (TeacherStudent relation : relations) {
            SysUser student = sysUserService.getById(relation.getStudentId());
            if (student != null && "STUDENT".equals(student.getRole())) {
                Map<String, Object> studentMap = new java.util.HashMap<>();
                studentMap.put("id", relation.getId());
                studentMap.put("studentId", student.getId());
                studentMap.put("username", student.getUsername());
                studentMap.put("realName", student.getRealName());
                studentMap.put("phone", student.getPhone());
                studentMap.put("email", student.getEmail());
                studentMap.put("gender", student.getGender());
                result.add(studentMap);
            }
        }
        return Result.ok(result);
    }

    @GetMapping("/available-students")
    public Result<List<Map<String, Object>>> getAvailableStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        List<Long> existingStudentIds = relations.stream().map(TeacherStudent::getStudentId).collect(Collectors.toList());
        List<SysUser> allStudents = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "STUDENT")
                .list();
        List<SysUser> availableStudents = allStudents.stream()
                .filter(s -> !existingStudentIds.contains(s.getId()))
                .collect(Collectors.toList());
        
        // 添加班级查询和返回班级名称
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (SysUser student : availableStudents) {
            // 关键词过滤
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                String realName = student.getRealName() != null ? student.getRealName().toLowerCase() : "";
                if (!realName.contains(lowerKeyword)) {
                    continue;
                }
            }
            
            // 班级过滤
            if (classId != null && !classId.equals(student.getClassId())) {
                continue;
            }
            
            Map<String, Object> studentMap = new java.util.HashMap<>();
            studentMap.put("id", student.getId());
            studentMap.put("username", student.getUsername());
            studentMap.put("realName", student.getRealName());
            studentMap.put("phone", student.getPhone());
            studentMap.put("email", student.getEmail());
            studentMap.put("gender", student.getGender());
            studentMap.put("classId", student.getClassId());
            
            // 添加班级名称
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

    @PostMapping("/assign")
    @LogOperation(module = "师生管理", type = "新增", value = "教师分配学生")
    public Result<Void> assignStudent(@RequestBody Map<String, Long> params, HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        Long studentId = params.get("studentId");
        
        TeacherStudent existing = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .eq(TeacherStudent::getStudentId, studentId)
                .one();
        if (existing != null) {
            return Result.fail("该学生已在分配列表中");
        }
        
        TeacherStudent ts = new TeacherStudent();
        ts.setTeacherId(teacherId);
        ts.setStudentId(studentId);
        teacherStudentService.save(ts);
        
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "师生管理", type = "删除", value = "移除学生")
    public Result<Void> removeStudent(@PathVariable Long id) {
        boolean success = teacherStudentService.removeById(id);
        if (!success) {
            return Result.fail("移除失败");
        }
        return Result.ok();
    }

    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> pageStudents(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            HttpServletRequest req) {
        System.out.println("===== 查询参数 =====");
        System.out.println("keyword: " + keyword);
        System.out.println("classId: " + classId);
        
        Long teacherId = (Long) req.getAttribute("userId");
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        
        List<Map<String, Object>> allStudents = new java.util.ArrayList<>();
        for (TeacherStudent relation : relations) {
            SysUser student = sysUserService.getById(relation.getStudentId());
            if (student != null && "STUDENT".equals(student.getRole())) {
                Map<String, Object> studentMap = new java.util.HashMap<>();
                studentMap.put("id", relation.getId());
                studentMap.put("studentId", student.getId());
                studentMap.put("username", student.getUsername());
                studentMap.put("realName", student.getRealName());
                studentMap.put("phone", student.getPhone());
                studentMap.put("email", student.getEmail());
                studentMap.put("gender", student.getGender());
                studentMap.put("classId", student.getClassId());
                
                // 添加班级名称
                if (student.getClassId() != null) {
                    ClassInfo classInfo = classService.getById(student.getClassId());
                    if (classInfo != null) {
                        studentMap.put("className", classInfo.getClassName());
                    }
                }
                
                allStudents.add(studentMap);
            }
        }
        
        // 过滤
        List<Map<String, Object>> filteredStudents = allStudents;
        
        // 关键词过滤
        if (keyword != null && !keyword.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            filteredStudents = filteredStudents.stream()
                    .filter(s -> {
                        String username = (String) s.get("username");
                        String realName = (String) s.get("realName");
                        return (username != null && username.toLowerCase().contains(lowerKeyword)) ||
                               (realName != null && realName.toLowerCase().contains(lowerKeyword));
                    })
                    .collect(Collectors.toList());
        }
        
        // 班级过滤
        if (classId != null) {
            System.out.println("执行班级过滤，classId: " + classId);
            System.out.println("过滤前人数：" + filteredStudents.size());
            filteredStudents = filteredStudents.stream()
                    .filter(s -> {
                        Object studentClassId = s.get("classId");
                        System.out.println("学生 classId: " + studentClassId + ", 比较：" + classId);
                        return studentClassId != null && classId.equals(studentClassId);
                    })
                    .collect(Collectors.toList());
            System.out.println("过滤后人数：" + filteredStudents.size());
        }
        
        // 手动分页
        int total = filteredStudents.size();
        int start = (int) ((current - 1) * size);
        int end = Math.min(start + (int) size, total);
        List<Map<String, Object>> pageData = start < total ? filteredStudents.subList(start, end) : new java.util.ArrayList<>();
        
        Page<Map<String, Object>> page = new Page<>(current, size);
        page.setRecords(pageData);
        page.setTotal(total);
        
        return Result.ok(page);
    }

    @GetMapping("/admin/teachers")
    public Result<List<SysUser>> getAllTeachers() {
        List<SysUser> teachers = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "TEACHER")
                .list();
        return Result.ok(teachers);
    }

    @GetMapping("/admin/students")
    public Result<List<SysUser>> getAllStudentsForAdmin() {
        List<SysUser> students = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "STUDENT")
                .list();
        return Result.ok(students);
    }

    @GetMapping("/admin/teacher/{teacherId}/students")
    public Result<List<SysUser>> getTeacherStudents(@PathVariable Long teacherId) {
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        List<Long> studentIds = relations.stream().map(TeacherStudent::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<SysUser> students = sysUserService.listByIds(studentIds);
        students = students.stream()
                .filter(s -> "STUDENT".equals(s.getRole()))
                .collect(Collectors.toList());
        return Result.ok(students);
    }

    @PostMapping("/admin/assign")
    @LogOperation(module = "师生管理", type = "新增", value = "管理员分配学生")
    public Result<Void> adminAssignStudent(@RequestBody Map<String, Long> params) {
        Long teacherId = params.get("teacherId");
        Long studentId = params.get("studentId");
        
        TeacherStudent existing = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .eq(TeacherStudent::getStudentId, studentId)
                .one();
        if (existing != null) {
            return Result.fail("该学生已在分配列表中");
        }
        
        TeacherStudent ts = new TeacherStudent();
        ts.setTeacherId(teacherId);
        ts.setStudentId(studentId);
        teacherStudentService.save(ts);
        
        return Result.ok();
    }

    @DeleteMapping("/admin/{id}")
    @LogOperation(module = "师生管理", type = "删除", value = "管理员移除学生")
    public Result<Void> adminRemoveStudent(@PathVariable Long id) {
        boolean success = teacherStudentService.removeById(id);
        if (!success) {
            return Result.fail("移除失败");
        }
        return Result.ok();
    }
}
