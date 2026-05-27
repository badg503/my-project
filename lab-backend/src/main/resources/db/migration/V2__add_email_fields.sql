-- 邮箱验证码功能数据库修改
-- 执行时间：2024-01-XX

-- 1. 为 sys_user 表添加邮箱相关字段
ALTER TABLE sys_user 
ADD COLUMN email VARCHAR(100) COMMENT '邮箱',
ADD COLUMN email_verified TINYINT DEFAULT 0 COMMENT '邮箱是否验证：0-未验证，1-已验证';

-- 2. 为已存在的用户添加默认邮箱（如果需要）
-- UPDATE sys_user SET email = CONCAT(username, '@example.com') WHERE email IS NULL;

-- 3. 添加邮箱唯一索引（可选，防止重复邮箱）
-- ALTER TABLE sys_user ADD UNIQUE INDEX idx_email (email);

-- 说明：
-- - email 字段用于接收验证码
-- - email_verified 字段用于标记邮箱是否已验证（可选功能）
-- - 如果表中已有数据，执行 ALTER TABLE 前请确保不会影响现有数据
