package com.weaving.llm.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 聊天消息 Mapper 接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
