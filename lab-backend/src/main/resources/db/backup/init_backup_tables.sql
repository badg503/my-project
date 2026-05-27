-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作人 ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
  `operation_type` VARCHAR(20) DEFAULT NULL COMMENT '操作类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求 URL',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '操作状态',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作 IP',
  `cost_time` BIGINT DEFAULT NULL COMMENT '操作耗时（毫秒）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 数据库备份表
CREATE TABLE IF NOT EXISTS `database_backup` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '备份 ID',
  `backup_file` VARCHAR(500) NOT NULL COMMENT '备份文件路径',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `backup_time` DATETIME NOT NULL COMMENT '备份时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_backup_time` (`backup_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库备份表';

-- 系统参数表
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '参数 ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '参数键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '参数值',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '参数描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- 初始化系统参数
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('backup.retention.days', '1', '备份文件保留天数'),
('data.retention.days', '365', '数据保留天数（日志、记录等）'),
('reserve.advance.days', '7', '可提前预约天数'),
('reserve.max.hours', '4', '单次最大预约时长（小时）'),
('alert.threshold', '80', '预警阈值（百分比）'),
('auto.backup.enabled', 'true', '是否启用自动备份'),
('auto.backup.time', '03:00', '自动备份时间（HH:mm）')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);
