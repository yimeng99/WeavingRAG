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
 * @Description: 聊天会话实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_session")
public class ChatSession {
    
    /**
     * 会话 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String sessionId;
    
    /**
     * 用户 ID (关联 user �?
     */
    private Long userId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 使用的模�?     */
    private String model;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 最后一条消息的预览
     */
    private String preview;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 最后更新时�?     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标识 (0:未删除，1:已删�?
     */
    @TableLogic
    private Integer deleted;
}
