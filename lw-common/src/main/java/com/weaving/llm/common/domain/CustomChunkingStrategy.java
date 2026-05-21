package com.weaving.llm.common.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("custom_chunking_strategy")
public class CustomChunkingStrategy {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    private Long userId;
    
    private String name;
    
    private String description;
    
    private String type;
    
    private Integer chunkSize;
    
    private Integer overlap;
    
    private String separators;
    
    private Integer maxChars;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
