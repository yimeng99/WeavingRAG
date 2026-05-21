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
@TableName("document_chunk")
public class DocumentChunk {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    @TableField("doc_id")
    private String docId;
    
    private String content;
    
    @TableField("chunk_index")
    private Integer chunkIndex;
    
    @TableField("vector_id")
    private String vectorId;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
