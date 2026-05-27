-- ================================================================
-- 开放实验室管理系统 - 完整数据库表结构（带外键约束）
-- 数据库：lab_management
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci
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
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(20) DEFAULT NULL COMMENT '角色：SYS_ADMIN/LAB_ADMIN/TEACHER/STUDENT',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',  
  `class_id` bigint(20) DEFAULT NULL COMMENT '班级 ID',
  `major` varchar(100) DEFAULT NULL COMMENT '专业',
  `department` varchar(100) DEFAULT NULL COMMENT '院系',
  `status` int(11) DEFAULT 1 COMMENT '状态：0 禁用 1 正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_class_id` (`class_id`),
  CONSTRAINT `fk_user_class` FOREIGN KEY (`class_id`) REFERENCES `class` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色代码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 3. 实验室信息表
-- ----------------------------
DROP TABLE IF EXISTS `lab_info`;
CREATE TABLE `lab_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '实验室 ID',
  `name` varchar(100) NOT NULL COMMENT '实验室名称',
  `location` varchar(200) DEFAULT NULL COMMENT '位置',
  `capacity` int(11) DEFAULT NULL COMMENT '容量',
  `status` varchar(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/UNAVAILABLE/MAINTENANCE',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室信息表';

-- ----------------------------
-- 4. 班级表
-- ----------------------------
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '班级 ID',
  `class_name` varchar(100) NOT NULL COMMENT '班级名称',
  `grade` varchar(20) DEFAULT NULL COMMENT '年级',
  `major` varchar(100) DEFAULT NULL COMMENT '专业',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_grade` (`grade`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ----------------------------
-- 5. 预约记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_reserve`;
CREATE TABLE `lab_reserve` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预约 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `lab_id` bigint(20) NOT NULL COMMENT '实验室 ID',
  `reserve_date` date NOT NULL COMMENT '预约日期',
  `reserve_time` varchar(50) NOT NULL COMMENT '预约时间段',
  `duration` int(11) DEFAULT NULL COMMENT '时长 (分钟)',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELLED',
  `reason` varchar(500) DEFAULT NULL COMMENT '预约原因',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_status` (`status`),
  KEY `idx_reserve_date` (`reserve_date`),
  CONSTRAINT `fk_reserve_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reserve_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- ----------------------------
-- 6. 设备信息表
-- ----------------------------
DROP TABLE IF EXISTS `lab_device`;
CREATE TABLE `lab_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '设备 ID',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '实验室 ID',
  `name` varchar(100) NOT NULL COMMENT '设备名称',
  `model` varchar(100) DEFAULT NULL COMMENT '型号',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `purchase_date` date DEFAULT NULL COMMENT '购买日期',
  `status` varchar(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/IN_USE/MAINTENANCE/SCRAPPED',
  `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_device_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- ----------------------------
-- 7. 设备借用记录表
-- ----------------------------
DROP TABLE IF EXISTS `device_borrow`;
CREATE TABLE `device_borrow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '借用 ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `borrow_time` datetime DEFAULT NULL COMMENT '借用时间',
  `return_time` datetime DEFAULT NULL COMMENT '归还时间',
  `status` varchar(20) DEFAULT 'BORROWED' COMMENT '状态：BORROWED/RETURNED/OVERDUE',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_borrow_device` FOREIGN KEY (`device_id`) REFERENCES `lab_device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备借用记录表';

-- ----------------------------
-- 8. 设备维修记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_repair`;
CREATE TABLE `lab_repair` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维修 ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备 ID',
  `description` varchar(500) NOT NULL COMMENT '故障描述',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/COMPLETED',
  `result` varchar(500) DEFAULT NULL COMMENT '维修结果',
  `report_time` datetime DEFAULT NULL COMMENT '报修时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_repair_device` FOREIGN KEY (`device_id`) REFERENCES `lab_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维修记录表';

-- ----------------------------
-- 9. 实验任务表
-- ----------------------------
DROP TABLE IF EXISTS `lab_task`;
CREATE TABLE `lab_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '实验室 ID',
  `teacher_id` bigint(20) DEFAULT NULL COMMENT '教师 ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `requirement` text COMMENT '要求',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  CONSTRAINT `fk_task_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_task_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验任务表';

-- ----------------------------
-- 10. 实验报告表
-- ----------------------------
DROP TABLE IF EXISTS `lab_report`;
CREATE TABLE `lab_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '报告 ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务 ID',
  `user_id` bigint(20) NOT NULL COMMENT '学生 ID',
  `content` text COMMENT '报告内容',
  `score` decimal(5,2) DEFAULT NULL COMMENT '成绩',
  `remark` varchar(500) DEFAULT NULL COMMENT '评语',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `grade_time` datetime DEFAULT NULL COMMENT '评分时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_report_task` FOREIGN KEY (`task_id`) REFERENCES `lab_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验报告表';

-- ----------------------------
-- 11. 考勤记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_attendance`;
CREATE TABLE `lab_attendance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '考勤 ID',
  `task_id` bigint(20) NOT NULL COMMENT '实验任务 ID',
  `user_id` bigint(20) NOT NULL COMMENT '学生 ID',
  `status` varchar(20) NOT NULL COMMENT '状态：ATTENDANCE/ABSENCE/LATE',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '签退时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `score` int(11) DEFAULT NULL COMMENT '成绩',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_attendance_task` FOREIGN KEY (`task_id`) REFERENCES `lab_task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attendance_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- ----------------------------
-- 12. 摄像头配置表
-- ----------------------------
DROP TABLE IF EXISTS `lab_camera`;
CREATE TABLE `lab_camera` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '摄像头 ID',
  `camera_code` varchar(50) NOT NULL UNIQUE COMMENT '摄像头编码',
  `camera_name` varchar(100) NOT NULL COMMENT '摄像头名称',
  `camera_type` varchar(20) NOT NULL COMMENT '类型：rtsp/http/usb',
  `camera_url` varchar(500) NOT NULL COMMENT '连接 URL',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '关联实验室 ID',
  `location` varchar(200) DEFAULT NULL COMMENT '位置',
  `description` varchar(500) DEFAULT NULL COMMENT '用途说明',
  `danger_classes` varchar(500) DEFAULT NULL COMMENT '监控的危险类别 JSON',
  `alert_threshold` decimal(3,2) DEFAULT 0.50 COMMENT '告警阈值',
  `confidence` decimal(3,2) DEFAULT 0.40 COMMENT '检测置信度',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `status` varchar(20) DEFAULT 'ONLINE' COMMENT '状态：ONLINE/OFFLINE/ERROR',
  `snapshot_enabled` tinyint(1) DEFAULT 1 COMMENT '是否保存截图',
  `snapshot_interval` int(11) DEFAULT 300 COMMENT '截图间隔 (秒)',
  `alert_cooldown` int(11) DEFAULT 60 COMMENT '告警冷却时间 (秒)',
  `extra_config` json DEFAULT NULL COMMENT '额外配置 JSON',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后检测时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_camera_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄像头配置表';

-- ----------------------------
-- 13. 摄像头检测记录表
-- ----------------------------
DROP TABLE IF EXISTS `lab_camera_log`;
CREATE TABLE `lab_camera_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
  `camera_id` bigint(20) NOT NULL COMMENT '摄像头 ID',
  `detect_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `detect_objects` json DEFAULT NULL COMMENT '检测到的物体列表',
  `danger_objects` json DEFAULT NULL COMMENT '危险物体列表',
  `safety_score` int(11) DEFAULT NULL COMMENT '安全评分 0-100',
  `has_alert` tinyint(1) DEFAULT 0 COMMENT '是否触发告警',
  `alert_level` varchar(20) DEFAULT NULL COMMENT '告警级别',
  `alert_message` varchar(500) DEFAULT NULL COMMENT '告警内容',
  `snapshot_path` varchar(500) DEFAULT NULL COMMENT '截图路径',
  `processed` tinyint(1) DEFAULT 0 COMMENT '是否已处理',
  `process_user_id` bigint(20) DEFAULT NULL COMMENT '处理人 ID',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `process_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  PRIMARY KEY (`id`),
  KEY `idx_camera_id` (`camera_id`),
  KEY `idx_detect_time` (`detect_time`),
  KEY `idx_has_alert` (`has_alert`),
  CONSTRAINT `fk_camera_log_camera` FOREIGN KEY (`camera_id`) REFERENCES `lab_camera` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄像头检测记录表';

-- ----------------------------
-- 14. 传感器配置表
-- ----------------------------
DROP TABLE IF EXISTS `sensor_config`;
CREATE TABLE `sensor_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '传感器 ID',
  `lab_id` bigint(20) DEFAULT NULL COMMENT '实验室 ID',
  `sensor_type` varchar(50) NOT NULL COMMENT '传感器类型',
  `threshold` decimal(10,2) DEFAULT NULL COMMENT '阈值',
  `status` varchar(20) DEFAULT 'ONLINE' COMMENT '状态：ONLINE/OFFLINE/ERROR',
  `last_read_time` datetime DEFAULT NULL COMMENT '最后读数时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_sensor_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器配置表';

-- ----------------------------
-- 15. 传感器读数表
-- ----------------------------
DROP TABLE IF EXISTS `sensor_reading`;
CREATE TABLE `sensor_reading` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '读数 ID',
  `sensor_id` bigint(20) NOT NULL COMMENT '传感器 ID',
  `value` decimal(10,2) NOT NULL COMMENT '数值',
  `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '读数时间',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT '状态：NORMAL/WARNING/ERROR',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sensor_id` (`sensor_id`),
  KEY `idx_read_time` (`read_time`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_reading_sensor` FOREIGN KEY (`sensor_id`) REFERENCES `sensor_config` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器读数表';

-- ----------------------------
-- 16. 故障预测记录表
-- ----------------------------
DROP TABLE IF EXISTS `fault_prediction`;
CREATE TABLE `fault_prediction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预测 ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备 ID',
  `probability` decimal(5,4) DEFAULT NULL COMMENT '故障概率',
  `predict_time` datetime DEFAULT NULL COMMENT '预测时间',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/FALSE_ALARM',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_prediction_device` FOREIGN KEY (`device_id`) REFERENCES `lab_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测记录表';

-- ----------------------------
-- 17. 安全告警表
-- ----------------------------
DROP TABLE IF EXISTS `safety_alert`;
CREATE TABLE `safety_alert` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '告警 ID',
  `camera_id` bigint(20) NOT NULL COMMENT '摄像头 ID',
  `alert_type` varchar(50) NOT NULL COMMENT '告警类型',
  `alert_time` datetime DEFAULT NULL COMMENT '告警时间',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSED/IGNORED',
  `handle_result` varchar(500) DEFAULT NULL COMMENT '处理结果',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_camera_id` (`camera_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_alert_camera` FOREIGN KEY (`camera_id`) REFERENCES `lab_camera` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全告警表';

-- ----------------------------
-- 18. AI 模型配置表
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置 ID',
  `model_type` varchar(50) NOT NULL COMMENT '模型类型',
  `param_key` varchar(100) NOT NULL COMMENT '参数键',
  `param_value` varchar(500) DEFAULT NULL COMMENT '参数值',
  `threshold` decimal(5,4) DEFAULT NULL COMMENT '阈值',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_key` (`model_type`, `param_key`),
  KEY `idx_model_type` (`model_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型配置表';

-- ----------------------------
-- 19. AI 知识库表
-- ----------------------------
DROP TABLE IF EXISTS `ai_knowledge`;
CREATE TABLE `ai_knowledge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '知识 ID',
  `category` varchar(50) DEFAULT NULL COMMENT '类别',
  `question` text NOT NULL COMMENT '问题',
  `answer` text NOT NULL COMMENT '答案',
  `similarity` decimal(5,4) DEFAULT NULL COMMENT '相似度',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库表';

-- ----------------------------
-- 20. 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置 ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----------------------------
-- 21. 公告表
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告 ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `publisher` varchar(50) DEFAULT NULL COMMENT '发布者',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `status` varchar(20) DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/DRAFT/ARCHIVED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ----------------------------
-- 22. 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `operation` varchar(500) DEFAULT NULL COMMENT '操作内容',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP 地址',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 23. 数据库备份记录表
-- ----------------------------
DROP TABLE IF EXISTS `database_backup`;
CREATE TABLE `database_backup` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '备份 ID',
  `backup_path` varchar(500) NOT NULL COMMENT '备份路径',
  `backup_time` datetime DEFAULT NULL COMMENT '备份时间',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小 (字节)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_backup_time` (`backup_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库备份记录表';

-- ----------------------------
-- 24. 师生关联表
-- ----------------------------
DROP TABLE IF EXISTS `teacher_student`;
CREATE TABLE `teacher_student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师 ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生 ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_student` (`teacher_id`, `student_id`),
  KEY `idx_student_id` (`student_id`),
  CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='师生关联表';

-- ----------------------------
-- 初始化基础数据
-- ----------------------------

-- 初始化角色数据
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`) VALUES 
('系统管理员', 'SYS_ADMIN', '系统超级管理员'),
('实验室管理员', 'LAB_ADMIN', '实验室管理员'),
('教师', 'TEACHER', '教师角色'),
('学生', 'STUDENT', '学生角色');

-- 初始化系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('reserve.advance.days', '30', '允许提前预约天数'),
('reserve.max.duration', '240', '单次预约最大时长 (分钟)'),
('data.retention.days', '365', '数据保留天数'),
('safety.alert.enabled', 'true', '安全告警开关');

-- 初始化 AI 模型配置
INSERT INTO `ai_model_config` (`model_type`, `param_key`, `param_value`, `threshold`, `description`) VALUES
('FAULT_PREDICT', 'fault_threshold', '0.6', 0.6000, '故障预测预警阈值'),
('SAFETY_DETECT', 'detection_interval_working', '20', NULL, '工作时间检测间隔 (秒)'),
('SAFETY_DETECT', 'confidence_threshold_working', '0.8', 0.8000, '工作时间置信度阈值'),
('QA', 'bert_similarity_threshold', '0.70', 0.7000, 'BERT 语义相似度阈值');

SET FOREIGN_KEY_CHECKS = 1;

-- ================================================================
-- 数据库表结构完成
-- 共计 24 个表，包含完整的外键约束
-- ================================================================
