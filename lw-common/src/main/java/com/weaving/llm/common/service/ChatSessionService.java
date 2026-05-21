package com.weaving.llm.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.ChatSession;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 聊天会话服务接口
 */
public interface ChatSessionService extends IService<ChatSession> {
    /**
     * 根据用户 ID 查询会话列表
     * @param userId 用户 ID
     * @return 会话列表
     */
    List<ChatSession> getSessionsByUserId(Long userId);
    
    /**
     * 创建或更新会话
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @param title 标题
     * @param model 模型
     * @param preview 预览
     * @return 会话对象
     */
    ChatSession createOrUpdateSession(String sessionId, Long userId, String title, String model, String preview);
    
    /**
     * 更新会话消息数量
     * @param sessionId 会话 ID
     * @param messageCount 消息数量
     * @param preview 预览
     */
    void updateSessionStats(String sessionId, Integer messageCount, String preview);
}
