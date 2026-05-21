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
@TableName("knowledge_base")
public class KnowledgeBase {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    private Long userId;
    
    private String name;
    
    private String description;
    
    private String icon;
    
    private String color;
    
    @TableField("doc_count")
    private Integer docCount;
    
    private Integer status;
    
    @TableField("is_public")
    private Integer isPublic;
    
    @TableField("sort_order")
    private Integer sortOrder;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
