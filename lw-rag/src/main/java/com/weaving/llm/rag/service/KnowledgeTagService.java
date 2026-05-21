package com.weaving.llm.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.KnowledgeTag;

import java.util.List;

public interface KnowledgeTagService extends IService<KnowledgeTag> {
    
    List<KnowledgeTag> getTagsByBaseId(String knowledgeBaseId);
    
    KnowledgeTag createTag(String knowledgeBaseId, String name, String color);
    
    boolean updateTag(String id, String name, String color);
    
    boolean deleteTag(String id);
    
    List<String> getTagIdsByDocId(String docId);
    
    void setDocTags(String docId, List<String> tagIds);
}
