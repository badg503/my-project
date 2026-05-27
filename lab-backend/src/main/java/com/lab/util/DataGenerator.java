package com.lab.util;

import com.lab.entity.*;
import com.lab.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class DataGenerator {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ClassService classService;

    private final Random random = new Random();

    private final String[] departments = {
        "计算机科学与技术学院", "电子信息工程学院", "机械工程学院",
        "土木工程学院", "化学化工学院", "生命科学学院",
        "经济管理学院", "人文学院", "外国语学院", "艺术学院"
    };

    private final String[] majors = {
        "计算机科学与技术", "软件工程", "人工智能",
        "电子信息工程", "通信工程", "自动化",
        "机械设计制造及其自动化", "土木工程", "化学工程与工艺",
        "生物工程", "工商管理", "会计学", "汉语言文学", "英语", "艺术设计"
    };

    private final String[] classNames = {
        "计科1班", "计科2班", "软件1班", "软件2班",
        "电子1班", "电子2班", "机械1班", "机械2班",
        "土木1班", "土木2班", "化工1班", "化工2班",
        "生工1班", "生工2班", "工商1班", "工商2班"
    };

    private final String[] phonePrefixes = {
        "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
        "150", "151", "152", "153", "155", "156", "157", "158", "159",
        "180", "181", "182", "183", "184", "185", "186", "187", "188", "189"
    };

    private final String[] emailSuffixes = {
        "@qq.com", "@163.com", "@126.com", "@gmail.com", "@outlook.com", "@edu.cn"
    };

    private final String[] genders = {"男", "女"};

    public void generateRandomData() {
        generateDepartments();
        generateClasses();
        generateUserData();
    }

    private void generateDepartments() {
        List<Department> existingDepartments = departmentService.list();
        if (existingDepartments.isEmpty()) {
            for (String deptName : departments) {
                Department dept = new Department();
                dept.setDepartmentName(deptName);
                departmentService.save(dept);
            }
        }
    }

    private void generateClasses() {
        List<Department> depts = departmentService.list();
        List<ClassInfo> existingClasses = classService.list();
        if (existingClasses.isEmpty() && !depts.isEmpty()) {
            for (String className : classNames) {
                ClassInfo cls = new ClassInfo();
                cls.setClassName(className);
                cls.setDepartmentId(depts.get(random.nextInt(depts.size())).getId());
                classService.save(cls);
            }
        }
    }

    private void generateUserData() {
        List<SysUser> users = sysUserService.list();
        
        for (SysUser user : users) {
            if (user.getPhone() == null || user.getPhone().isEmpty()) {
                user.setPhone(generateRandomPhone());
            }
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                user.setEmail(generateRandomEmail(user.getUsername()));
            }
            sysUserService.updateById(user);
        }
    }

    private String generateRandomPhone() {
        String prefix = phonePrefixes[random.nextInt(phonePrefixes.length)];
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            suffix.append(random.nextInt(10));
        }
        return prefix + suffix.toString();
    }

    private String generateRandomEmail(String username) {
        String suffix = emailSuffixes[random.nextInt(emailSuffixes.length)];
        return username + suffix;
    }
}
