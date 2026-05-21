package com.weaving.llm.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.ChatMessage;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 聊天消息服务接口
 */
public interface ChatMessageService extends IService<ChatMessage> {
    
    /**
     * 根据会话 ID 查询消息列表
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> getMessagesBySessionId(String sessionId);
    
    /**
     * 保存消息
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @param role 角色
     * @param content 内容
     * @param model 模型
     * @return 消息对象
     */
    ChatMessage saveMessage(String sessionId, Long userId, String role, String content, String model);
}
