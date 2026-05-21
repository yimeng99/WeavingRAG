package com.weaving.llm.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.domain.CustomChunkingStrategy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomChunkingStrategyMapper extends BaseMapper<CustomChunkingStrategy> {
}
