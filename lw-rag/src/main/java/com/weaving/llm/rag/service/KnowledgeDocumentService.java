package com.weaving.llm.rag.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.KnowledgeDocument;

import java.util.List;
import java.util.Map;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 知识库文档服务接口
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 根据知识库ID查询文档列表
     * @param knowledgeBaseId 知识库ID
     * @return 文档列表
     */
    List<KnowledgeDocument> getDocumentsByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 分页查询文档列表（支持动态条件）
     * @param knowledgeDocument 查询条件对象
     * @return 文档列表
     */
    List<KnowledgeDocument> pageList(KnowledgeDocument knowledgeDocument);

    Page<KnowledgeDocument> getDocumentsPageList(Map<String, Object> params);

    /**
     * 根据知识库ID分页查询文档列表（支持动态条件，分页参数从请求中获取）
     * @param params 查询参数，key包括: knowledgeBaseId(必填), status, title, type, userId
     * @return 分页文档列表
     */
    Page<KnowledgeDocument> getDocumentsByKnowledgeBaseIdPaged(Map<String, Object> params);

    /**
     * 根据用户ID查询文档列表
     * @param userId 用户ID
     * @return 文档列表
     */
    List<KnowledgeDocument> getDocumentsByUserId(Long userId);

    /**
     * 保存文档
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @param title 标题
     * @param content 内容
     * @param type 类型
     * @param source 来源
     * @return 文档对象
     */
    KnowledgeDocument saveDocument(Long userId, String knowledgeBaseId, String title, String content, String type, String source);
}
