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
 * @Description: 聊天消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage {
    
    /**
     * 消息 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String messageId;
    
    /**
     * 会话 ID (关联 chat_session �?
     */
    private String sessionId;
    
    /**
     * 用户 ID (关联 user �?
     */
    private Long userId;
    
    /**
     * 消息角色 (user/assistant/system)
     */
    private String role;
    
    /**
     * 消息内容
     */
    @TableField(typeHandler = org.apache.ibatis.type.StringTypeHandler.class)
    private String content;
    
    /**
     * 父消�?ID (用于引用回复)
     */
    private String parentMessageId;
    
    /**
     * 使用的模�?     */
    private String model;
    
    /**
     * token 使用�?     */
    private Integer tokenUsage;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 逻辑删除标识 (0:未删除，1:已删�?
     */
    @TableLogic
    private Integer deleted;
}
