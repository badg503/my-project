-- 故障预测记录表
CREATE TABLE IF NOT EXISTS `fault_prediction_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `lab_id` BIGINT NOT NULL COMMENT '实验室 ID',
    `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发类型 (OPEN:开门检测，CLOSE:关门检测，MANUAL:手动检测)',
    `total_devices` INT DEFAULT 0 COMMENT '检测的设备总数',
    `faulty_devices` INT DEFAULT 0 COMMENT '疑似故障设备数',
    `normal_devices` INT DEFAULT 0 COMMENT '正常设备数',
    `prediction_result` TEXT COMMENT '详细预测结果 (JSON 格式)',
    `status` VARCHAR(20) DEFAULT 'COMPLETED' COMMENT '状态 (COMPLETED:完成，FAILED:失败)',
    `message` VARCHAR(500) COMMENT '检测结果说明',
    `operator_id` BIGINT COMMENT '操作人 ID（手动检测时）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_lab (`lab_id`),
    INDEX idx_trigger (`trigger_type`),
    INDEX idx_time (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测记录表';

-- 故障预测详情表
CREATE TABLE IF NOT EXISTS `fault_prediction_detail` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    `record_id` BIGINT NOT NULL COMMENT '预测记录 ID',
    `device_id` BIGINT NOT NULL COMMENT '设备 ID',
    `device_name` VARCHAR(200) COMMENT '设备名称',
    `fault_prob` DECIMAL(5,4) COMMENT '故障概率 (0-1)',
    `threshold` DECIMAL(5,4) COMMENT '预警阈值',
    `is_faulty` TINYINT DEFAULT 0 COMMENT '是否疑似故障 (1:是，0:否)',
    `message` VARCHAR(500) COMMENT '分析结果',
    `suggestion` VARCHAR(500) COMMENT '建议',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_record (`record_id`),
    INDEX idx_device (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障预测详情表';

-- 初始化数据示例
INSERT INTO `fault_prediction_record` (`lab_id`, `trigger_type`, `total_devices`, `faulty_devices`, `normal_devices`, `message`) VALUES
(3, 'OPEN', 10, 0, 10, '实验室开门检测：所有设备正常'),
(3, 'CLOSE', 10, 1, 9, '实验室关门检测：发现 1 台疑似故障设备');
