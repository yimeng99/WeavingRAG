package com.weaving.llm.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.KnowledgeBase;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseCreateRequest;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseUpdateRequest;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {
    
    List<KnowledgeBase> getBasesByUserId(Long userId);
    
    KnowledgeBase createBase(Long userId, KnowledgeBaseCreateRequest request);

    boolean updateBase(String id, KnowledgeBaseUpdateRequest request);
    
    boolean deleteBase(String id);
    
    void incrementDocCount(String baseId);
    
    void decrementDocCount(String baseId);

    List<KnowledgeBase> pageList(KnowledgeBase knowledgeBase);
}
