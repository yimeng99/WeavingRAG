package com.weaving.llm.common.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.ai.domain.entity.AIProvider;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 服务商 Mapper
 */
@Mapper
public interface AIProviderMapper extends BaseMapper<AIProvider> {
}