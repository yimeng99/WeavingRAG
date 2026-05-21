package com.weaving.llm.common.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.ai.domain.entity.AIModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型 Mapper
 */
@Mapper
public interface AIModelMapper extends BaseMapper<AIModel> {
}