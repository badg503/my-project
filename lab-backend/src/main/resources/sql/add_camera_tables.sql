-- 摄像头配置表
DROP TABLE IF EXISTS lab_camera;
CREATE TABLE lab_camera (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '摄像头ID',
    camera_code VARCHAR(50) NOT NULL UNIQUE COMMENT '摄像头编码，如: lab_a, entrance',
    camera_name VARCHAR(100) NOT NULL COMMENT '摄像头名称',
    camera_type VARCHAR(20) NOT NULL COMMENT '类型: rtsp/http/usb',
    camera_url VARCHAR(500) NOT NULL COMMENT '连接URL',
    lab_id BIGINT COMMENT '关联实验室ID',
    location VARCHAR(200) COMMENT '具体位置描述',
    description VARCHAR(500) COMMENT '用途说明',
    
    -- 检测配置
    danger_classes VARCHAR(500) COMMENT '监控的危险类别，JSON格式: ["person","fire","knife"]',
    alert_threshold DECIMAL(3,2) DEFAULT 0.50 COMMENT '告警阈值 0-1',
    confidence DECIMAL(3,2) DEFAULT 0.40 COMMENT '检测置信度',
    
    -- 状态控制
    enabled TINYINT DEFAULT 1 COMMENT '是否启用: 0禁用 1启用',
    status VARCHAR(20) DEFAULT 'ONLINE' COMMENT '状态: ONLINE/OFFLINE/ERROR',
    
    -- 扩展配置
    snapshot_enabled TINYINT DEFAULT 1 COMMENT '是否保存截图',
    snapshot_interval INT DEFAULT 300 COMMENT '截图间隔(秒)',
    alert_cooldown INT DEFAULT 60 COMMENT '告警冷却时间(秒)',
    extra_config JSON COMMENT '额外配置JSON',
    
    -- 时间戳
    last_check_time DATETIME COMMENT '最后检测时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_lab_id (lab_id),
    INDEX idx_enabled (enabled),
    INDEX idx_status (status)
) COMMENT '实验室摄像头配置表';

-- 摄像头检测记录表
DROP TABLE IF EXISTS lab_camera_log;
CREATE TABLE lab_camera_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    camera_id BIGINT NOT NULL COMMENT '摄像头ID',
    detect_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    
    -- 检测结果
    detect_objects JSON COMMENT '检测到的物体列表',
    danger_objects JSON COMMENT '危险物体列表',
    safety_score INT COMMENT '安全评分 0-100',
    
    -- 告警信息
    has_alert TINYINT DEFAULT 0 COMMENT '是否触发告警',
    alert_level VARCHAR(20) COMMENT '告警级别: LOW/MEDIUM/HIGH',
    alert_message VARCHAR(500) COMMENT '告警内容',
    
    -- 截图
    snapshot_path VARCHAR(500) COMMENT '截图文件路径',
    
    -- 处理状态
    processed TINYINT DEFAULT 0 COMMENT '是否已处理',
    process_user_id BIGINT COMMENT '处理人ID',
    process_time DATETIME COMMENT '处理时间',
    process_remark VARCHAR(500) COMMENT '处理备注',
    
    INDEX idx_camera_id (camera_id),
    INDEX idx_detect_time (detect_time),
    INDEX idx_has_alert (has_alert)
) COMMENT '摄像头检测记录表';

-- 插入示例数据（包含本地USB摄像头）
INSERT INTO lab_camera (
    camera_code, camera_name, camera_type, camera_url, 
    lab_id, location, description, danger_classes, alert_threshold
) VALUES 
('local_usb', '本地测试摄像头', 'usb', '0', 
 NULL, '本地测试', 'USB摄像头测试', '["person","fire","knife"]', 0.50),

('entrance', '入口摄像头', 'rtsp', 'rtsp://admin:password@192.168.1.101:554/Streaming/Channels/101', 
 NULL, '实验室主入口', '人员进出监控', '["person","fire","knife"]', 0.50),

('lab_a', 'A实验室摄像头', 'rtsp', 'rtsp://admin:password@192.168.1.102:554/cam/realmonitor?channel=1&subtype=0', 
 1, 'A实验室内部', '实验操作监控', '["person","fire","chemical"]', 0.60);
