package com.weaving.llm.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.weaving.llm.common.domain.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /**
     * 分页查询知识库下的文档列表（PageHelper 自动分页）
     * @param page 分页对象（PageHelper 会自动设置分页和总数）
     * @param params 查询参数，包含 knowledgeBaseId, status, title, type, userId
     * @return 分页文档列表
     */
    List<KnowledgeDocument> selectDocumentsByKnowledgeBaseId(Page<KnowledgeDocument> page, Map<String, Object> params);
}
