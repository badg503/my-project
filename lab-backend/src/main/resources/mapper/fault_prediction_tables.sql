-- 预测任务表
CREATE TABLE IF NOT EXISTS `prediction_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务 ID（UUID）',
  `lab_id` BIGINT DEFAULT NULL COMMENT '实验室 ID',
  `total_devices` INT NOT NULL DEFAULT 0 COMMENT '总设备数',
  `processed_devices` INT NOT NULL DEFAULT 0 COMMENT '已完成设备数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING,RUNNING,COMPLETED,FAILED',
  `estimated_time` INT NOT NULL DEFAULT 0 COMMENT '预估时间（秒）',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `trigger_type` VARCHAR(20) DEFAULT NULL COMMENT '触发类型：SCHEDULED,MANUAL',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测任务表';

-- 故障预测记录表（已存在，确认结构）
CREATE TABLE IF NOT EXISTS `fault_prediction_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `lab_id` BIGINT NOT NULL COMMENT '实验室 ID',
  `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发类型：OPEN,CLOSE,MANUAL,SCHEDULED',
  `total_devices` INT NOT NULL DEFAULT 0 COMMENT '总设备数',
  `faulty_devices` INT NOT NULL DEFAULT 0 COMMENT '故障设备数',
  `normal_devices` INT NOT NULL DEFAULT 0 COMMENT '正常设备数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' COMMENT '状态',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人 ID',
  `prediction_result` JSON DEFAULT NULL COMMENT '预测结果（JSON 格式）',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '消息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测记录表';

-- 故障预测详情表（独立表，不需要关联记录表）
CREATE TABLE IF NOT EXISTS `fault_prediction_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `lab_id` BIGINT NOT NULL COMMENT '实验室 ID',
  `device_id` BIGINT NOT NULL COMMENT '设备 ID',
  `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  `trigger_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发类型：SCHEDULED(定时),MANUAL(实时)',
  `fault_prob` DOUBLE NOT NULL DEFAULT 0 COMMENT '故障概率',
  `threshold` DOUBLE NOT NULL DEFAULT 0.5 COMMENT '阈值',
  `is_faulty` INT NOT NULL DEFAULT 0 COMMENT '是否故障 0/1',
  `message` VARCHAR(200) DEFAULT NULL COMMENT '消息',
  `suggestion` VARCHAR(500) DEFAULT NULL COMMENT '建议',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_id` (`lab_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_trigger_type` (`trigger_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测详情表';

-- 传感器读数表（已存在，确认结构）
CREATE TABLE IF NOT EXISTS `sensor_reading` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `device_id` BIGINT NOT NULL COMMENT '设备 ID',
  `temp` DOUBLE DEFAULT NULL COMMENT '温度',
  `vibration` DOUBLE DEFAULT NULL COMMENT '振动',
  `current` DOUBLE DEFAULT NULL COMMENT '电流',
  `reading_time` DATETIME DEFAULT NULL COMMENT '读数时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_reading_time` (`reading_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器读数表';

-- 传感器配置表（已存在，确认结构）
CREATE TABLE IF NOT EXISTS `sensor_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `lab_id` BIGINT DEFAULT NULL COMMENT '实验室 ID',
  `device_id` BIGINT DEFAULT NULL COMMENT '设备 ID',
  `temp_sensor_ip` VARCHAR(50) DEFAULT NULL COMMENT '温度传感器 IP',
  `vibration_sensor_ip` VARCHAR(50) DEFAULT NULL COMMENT '振动传感器 IP',
  `current_sensor_ip` VARCHAR(50) DEFAULT NULL COMMENT '电流传感器 IP',
  `gateway_ip` VARCHAR(50) DEFAULT NULL COMMENT '网关 IP',
  `api_port` INT DEFAULT NULL COMMENT 'API 端口',
  `enabled` INT NOT NULL DEFAULT 1 COMMENT '是否启用 0/1',
  `mode` VARCHAR(20) DEFAULT 'virtual' COMMENT '模式：real,virtual',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_lab_id` (`lab_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器配置表';

-- 系统通知表
CREATE TABLE IF NOT EXISTS `system_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `type` VARCHAR(20) DEFAULT 'INFO' COMMENT '类型：INFO,WARNING,ERROR',
  `level` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '级别：LOW,NORMAL,HIGH,URGENT',
  `is_read` INT NOT NULL DEFAULT 0 COMMENT '是否已读 0/1',
  `receiver_id` BIGINT DEFAULT NULL COMMENT '接收人 ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_level` (`level`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';
