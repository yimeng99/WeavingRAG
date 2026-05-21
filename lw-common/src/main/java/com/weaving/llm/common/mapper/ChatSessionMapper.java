package com.weaving.llm.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.domain.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 聊天会话 Mapper 接口
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
