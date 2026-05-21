package com.weaving.llm.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.weaving.llm.common.domain.ChatMessage;
import com.weaving.llm.common.mapper.ChatMessageMapper;
import com.weaving.llm.common.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 聊天消息服务实现类
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public List<ChatMessage> getMessagesBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public ChatMessage saveMessage(String sessionId, Long userId, String role, String content, String model) {
        ChatMessage message = ChatMessage.builder()
                .messageId(null) // 使用雪花算法生成
                .sessionId(sessionId)
                .userId(userId)
                .role(role)
                .content(content)
                .model(model)
                .build();
        this.save(message);
        return message;
    }
}
