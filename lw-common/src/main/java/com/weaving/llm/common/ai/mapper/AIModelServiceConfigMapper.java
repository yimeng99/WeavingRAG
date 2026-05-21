package com.weaving.llm.common.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.ai.domain.entity.AIModelServiceConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型服务配置 Mapper
 */
@Mapper
public interface AIModelServiceConfigMapper extends BaseMapper<AIModelServiceConfig> {
}