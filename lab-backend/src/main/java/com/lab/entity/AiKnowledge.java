package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge")
public class AiKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String answer;
    private String category;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
