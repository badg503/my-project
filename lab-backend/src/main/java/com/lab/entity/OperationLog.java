package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作人 ID
     */
    private Long userId;
    
    /**
     * 操作人姓名
     */
    private String userName;
    
    /**
     * 操作模块（如：实验室管理、设备管理、预约管理等）
     */
    private String module;
    
    /**
     * 操作类型（如：新增、修改、删除、查询、导出等）
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 请求方法（GET、POST、PUT、DELETE 等）
     */
    private String requestMethod;
    
    /**
     * 请求 URL
     */
    private String requestUrl;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 响应状态（SUCCESS、FAIL）
     */
    private String status;
    
    /**
     * 操作 IP 地址
     */
    private String ip;
    
    /**
     * 操作耗时（毫秒）
     */
    private Long costTime;
    
    /**
     * 错误信息（如果操作失败）
     */
    private String errorMsg;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
