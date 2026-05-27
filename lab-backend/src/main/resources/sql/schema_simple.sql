-- ================================================================
-- 开放实验室管理系统 - 极简版数据库表结构
-- 数据库：lab_management
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci
-- 共计 17 个表，每个表仅保留 2 个核心属性
-- ================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 2. 班级表
-- ----------------------------
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '班级 ID',
  `class_name` varchar(100) NOT NULL COMMENT '班级名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ----------------------------
-- 3. 实验室信息表
-- ----------------------------
DROP TABLE IF EXISTS `lab_info`;
CREATE TABLE `lab_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '实验室 ID',
  `name` varchar(100) NOT NULL COMMENT '实验室名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室信息表';

-- ----------------------------
-- 4. 预约记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_reserve`;
CREATE TABLE `lab_reserve` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预约 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `lab_id` bigint(20) NOT NULL COMMENT '实验室 ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_lab_id` (`lab_id`),
  CONSTRAINT `fk_reserve_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reserve_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- ----------------------------
-- 5. 设备信息表
-- ----------------------------
DROP TABLE IF EXISTS `lab_device`;
CREATE TABLE `lab_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '设备 ID',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '实验室 ID',
  `name` varchar(100) NOT NULL COMMENT '设备名称',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  CONSTRAINT `fk_device_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- ----------------------------
-- 6. 设备借用记录表
-- ----------------------------
DROP TABLE IF EXISTS `device_borrow`;
CREATE TABLE `device_borrow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '借用 ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_borrow_device` FOREIGN KEY (`device_id`) REFERENCES `lab_device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备借用记录表';

-- ----------------------------
-- 7. 设备维修记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_repair`;
CREATE TABLE `lab_repair` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维修 ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备 ID',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  CONSTRAINT `fk_repair_device` FOREIGN KEY (`device_id`) REFERENCES `lab_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维修记录表';

-- ----------------------------
-- 8. 实验任务表
-- ----------------------------
DROP TABLE IF EXISTS `lab_task`;
CREATE TABLE `lab_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '实验室 ID',
  `teacher_id` bigint(20) DEFAULT NULL COMMENT '教师 ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  CONSTRAINT `fk_task_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_task_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验任务表';

-- ----------------------------
-- 9. 实验报告表
-- ----------------------------
DROP TABLE IF EXISTS `lab_report`;
CREATE TABLE `lab_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '报告 ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务 ID',
  `user_id` bigint(20) NOT NULL COMMENT '学生 ID',
  `content` text COMMENT '报告内容',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_report_task` FOREIGN KEY (`task_id`) REFERENCES `lab_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验报告表';

-- ----------------------------
-- 10. 考勤记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_attendance`;
CREATE TABLE `lab_attendance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '考勤 ID',
  `task_id` bigint(20) NOT NULL COMMENT '实验任务 ID',
  `user_id` bigint(20) NOT NULL COMMENT '学生 ID',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_attendance_task` FOREIGN KEY (`task_id`) REFERENCES `lab_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attendance_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- ----------------------------
-- 11. 摄像头配置表
-- ----------------------------
DROP TABLE IF EXISTS `lab_camera`;
CREATE TABLE `lab_camera` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '摄像头 ID',
  `camera_name` varchar(100) NOT NULL COMMENT '摄像头名称',
  `camera_url` varchar(500) NOT NULL COMMENT '连接 URL',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '关联实验室 ID',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  CONSTRAINT `fk_camera_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄像头配置表';

-- ----------------------------
-- 12. 摄像头检测记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_camera_log`;
CREATE TABLE `lab_camera_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
  `camera_id` bigint(20) NOT NULL COMMENT '摄像头 ID',
  `detect_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_camera_id` (`camera_id`),
  CONSTRAINT `fk_camera_log_camera` FOREIGN KEY (`camera_id`) REFERENCES `lab_camera` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄像头检测记录表';

-- ----------------------------
-- 13. 传感器配置表
-- ----------------------------
DROP TABLE IF EXISTS `sensor_config`;
CREATE TABLE `sensor_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '传感器 ID',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '实验室 ID',
  `sensor_type` varchar(50) NOT NULL COMMENT '传感器类型',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  CONSTRAINT `fk_sensor_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器配置表';

-- ----------------------------
-- 14. 传感器读数表
-- ----------------------------
DROP TABLE IF EXISTS `sensor_reading`;
CREATE TABLE `sensor_reading` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '读数 ID',
  `sensor_id` bigint(20) NOT NULL COMMENT '传感器 ID',
  `value` decimal(10,2) NOT NULL COMMENT '数值',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_id` (`sensor_id`),
  CONSTRAINT `fk_reading_sensor` FOREIGN KEY (`sensor_id`) REFERENCES `sensor_config` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器读数表';

-- ----------------------------
-- 15. 故障预测记录表
-- ----------------------------
DROP TABLE IF EXISTS `fault_prediction`;
CREATE TABLE `fault_prediction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预测 ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备 ID',
  `probability` decimal(5,4) DEFAULT NULL COMMENT '故障概率',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  CONSTRAINT `fk_prediction_device` FOREIGN KEY (`device_id`) REFERENCES `lab_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测记录表';

-- ----------------------------
-- 16. 安全告警表
-- ----------------------------
DROP TABLE IF EXISTS `safety_alert`;
CREATE TABLE `safety_alert` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '告警 ID',
  `camera_id` bigint(20) NOT NULL COMMENT '摄像头 ID',
  `alert_type` varchar(50) NOT NULL COMMENT '告警类型',
  PRIMARY KEY (`id`),
  KEY `idx_camera_id` (`camera_id`),
  CONSTRAINT `fk_alert_camera` FOREIGN KEY (`camera_id`) REFERENCES `lab_camera` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全告警表';

-- ----------------------------
-- 17. 师生关联表
-- ----------------------------
DROP TABLE IF EXISTS `teacher_student`;
CREATE TABLE `teacher_student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师 ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_student` (`teacher_id`, `student_id`),
  CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='师生关联表';

SET FOREIGN_KEY_CHECKS = 1;

-- ================================================================
-- 数据库表结构完成（极简版）
-- 共计 17 个表，每个表保留 2 个核心属性
-- ================================================================
