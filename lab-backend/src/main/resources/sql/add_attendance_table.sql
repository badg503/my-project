-- 添加考勤表
DROP TABLE IF EXISTS lab_attendance;
CREATE TABLE lab_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '实验任务ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    status VARCHAR(20) NOT NULL COMMENT '考勤状态: ATTENDANCE/ABSENCE/LATE',
    check_in_time DATETIME COMMENT '签到时间',
    remark VARCHAR(500) COMMENT '备注',
    score INT COMMENT '成绩',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) COMMENT '实验考勤表';
ALTER TABLE lab_attendance ADD COLUMN score INT COMMENT '成绩';