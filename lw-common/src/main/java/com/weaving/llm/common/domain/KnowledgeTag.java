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
@TableName("knowledge_tag")
public class KnowledgeTag {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    @TableField("knowledge_base_id")
    private String knowledgeBaseId;
    
    private String name;
    
    private String color;
    
    @TableField("sort_order")
    private Integer sortOrder;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
