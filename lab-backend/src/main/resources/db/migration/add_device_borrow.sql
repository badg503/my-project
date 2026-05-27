-- 创建设备借用表
CREATE TABLE IF NOT EXISTS `device_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reserve_id` bigint DEFAULT NULL COMMENT '预约ID',
  `device_id` bigint DEFAULT NULL COMMENT '设备ID',
  `device_name` varchar(255) DEFAULT NULL COMMENT '设备名称',
  `lab_id` bigint DEFAULT NULL COMMENT '实验室ID',
  `lab_name` varchar(255) DEFAULT NULL COMMENT '实验室名称',
  `user_id` bigint DEFAULT NULL COMMENT '借用用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '借用用户姓名',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核，APPROVED-已通过，RETURNED-已归还',
  `borrow_remark` text COMMENT '借用备注',
  `return_remark` text COMMENT '归还备注',
  `deleted` int DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `borrow_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '借用时间',
  `return_time` datetime DEFAULT NULL COMMENT '归还时间',
  PRIMARY KEY (`id`),
  KEY `idx_reserve_id` (`reserve_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备借用表';

-- 为预约表添加设备借用相关字段
ALTER TABLE `lab_reserve` 
ADD COLUMN `device_ids` text COMMENT '借用设备ID列表，逗号分隔',
ADD COLUMN `borrow_remark` text COMMENT '借用备注',
ADD COLUMN `lab_audit_user_id` bigint DEFAULT NULL COMMENT '实验室管理员审核人ID',
ADD COLUMN `lab_audit_remark` text COMMENT '实验室管理员审核备注',
ADD COLUMN `lab_audit_time` datetime DEFAULT NULL COMMENT '实验室管理员审核时间';
