package com.weaving.llm.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.ChatSession;
import com.weaving.llm.common.mapper.ChatSessionMapper;
import com.weaving.llm.common.service.ChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 聊天会话服务实现类
 */
@Slf4j
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    @Override
    public List<ChatSession> getSessionsByUserId(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return this.list(wrapper);
    }

    @Override
    public ChatSession createOrUpdateSession(String sessionId, Long userId, String title, String model, String preview) {
        // 查询是否已存在
        ChatSession session = this.getById(sessionId);
        
        if (session != null) {
            // 更新现有会话
            session.setTitle(title);
            session.setModel(model);
            session.setPreview(preview);
            session.setUpdateTime(LocalDateTime.now());
            this.updateById(session);
            log.info("更新会话：{} - {}", sessionId, title);
        } else {
            // 创建新会话
            session = ChatSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .title(title)
                    .model(model)
                    .messageCount(0)
                    .preview(preview)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .deleted(0)
                    .build();
            this.save(session);
            log.info("创建新会话：{} - {}", sessionId, title);
        }
        
        return session;
    }

    @Override
    public void updateSessionStats(String sessionId, Integer messageCount, String preview) {
        ChatSession session = this.getById(sessionId);
        if (session != null) {
            session.setMessageCount(messageCount);
            session.setPreview(preview);
            session.setUpdateTime(LocalDateTime.now());
            this.updateById(session);
        }
    }
}
