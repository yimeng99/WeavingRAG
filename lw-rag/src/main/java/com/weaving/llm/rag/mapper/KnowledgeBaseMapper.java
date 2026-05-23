package com.weaving.llm.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weaving.llm.common.domain.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    List<KnowledgeBase> pageList(@Param("params") KnowledgeBase knowledgeBase);
}
