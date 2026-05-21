package com.weaving.llm.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.KnowledgeDocument;

import java.util.List;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 知识库文档服务接�? */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {
    
    /**
     * 根据知识�?ID 查询文档列表
     * @param knowledgeBaseId 知识�?ID
     * @return 文档列表
     */
    List<KnowledgeDocument> getDocumentsByKnowledgeBaseId(String knowledgeBaseId);
    
    /**
     * 根据用户 ID 查询文档列表
     * @param userId 用户 ID
     * @return 文档列表
     */
    List<KnowledgeDocument> getDocumentsByUserId(Long userId);
    
    /**
     * 保存文档
     * @param userId 用户 ID
     * @param knowledgeBaseId 知识�?ID
     * @param title 标题
     * @param content 内容
     * @param type 类型
     * @param source 来源
     * @return 文档对象
     */
    KnowledgeDocument saveDocument(Long userId, String knowledgeBaseId, String title, String content, String type, String source);
}
