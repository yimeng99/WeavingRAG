package com.weaving.llm.common.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 知识库文档实�? */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_document")
public class KnowledgeDocument {
    
    /**
     * 文档 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String docId;
    
    /**
     * 用户 ID (关联 user )
     */
    private Long userId;
    
    /**
     * 知识库ID (用于分组)
     */
    private String knowledgeBaseId;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档内容
     */
    @TableField(typeHandler = org.apache.ibatis.type.StringTypeHandler.class)
    private String content;
    
    /**
     * 文档类型 (text, markdown, pdf, url...)
     */
    private String type;
    
    /**
     * 来源 URL 或文件文件路径 */
    private String source;
    
    /**
     * 关键词标(逗号分隔)
     */
    private String tags;
    
    /**
     * 向量 ID (Milvus 中的向量 ID)
     */
    private String vectorId;
    
    /**
     * 分块序号
     */
    private Integer chunkIndex;
    
    /**
     * 分块大小
     */
    private Integer chunkSize;
    
    /**
     * 切片策略 (intelligent, char, page, heading, regex, separator)
     */
    private String chunkingStrategy;
    
    /**
     * 切片数量
     */
    private Integer chunkCount;
    
    /**
     * 状态(0:处理中，1:已完成，2:失败)
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标识 (0:未删除，1:已删)
     */
    @TableLogic
    private Integer deleted;
}
