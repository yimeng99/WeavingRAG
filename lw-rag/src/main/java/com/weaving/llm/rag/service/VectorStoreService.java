package com.weaving.llm.rag.service;

import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.common.domain.KnowledgeDocument;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务接口
 * 抽象向量存储操作，支持不同的向量数据库实现（Chroma、Milvus、PgVector 等）
 *
 * @Author: 依梦
 * @Date: 2025/10/27
 */
public interface VectorStoreService {

    /**
     * 检查向量存储服务是否可用
     * @return true 如果服务可用
     */
    boolean isAvailable();

    /**
     * 对单个文档进行分割和向量化存储
     * @param doc 知识库文档
     * @return 处理结果，包含 success、message、chunkCount、vectorIds 等
     */
    Map<String, Object> embedDocument(KnowledgeDocument doc);

    /**
     * 批量处理多个文档的向量化
     * @param documents 文档列表
     * @return 处理统计，包含 successCount、failCount、totalChunks 等
     */
    Map<String, Object> embedDocuments(List<KnowledgeDocument> documents);

    /**
     * 对文本列表进行向量化并存储
     * @param texts 文本列表
     * @param docId 文档 ID
     * @param knowledgeBaseId 知识库 ID
     * @param userId 用户 ID
     * @param title 文档标题
     * @return 向量 ID 列表
     */
    List<String> addVectors(List<String> texts, String docId, String knowledgeBaseId, Long userId, String title);

    /**
     * 简化版本的 addVectors，只需要 docId
     * @param texts 文本列表
     * @param docId 文档 ID
     * @return 向量 ID 列表
     */
    List<String> addVectors(List<String> texts, String docId);

    /**
     * 根据文档 ID 删除对应的向量
     * @param docId 文档 ID
     */
    void deleteVectorsByDocId(String docId);

    /**
     * 向量相似度检索（支持元数据过滤）
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param userId 用户 ID（可选）
     * @param knowledgeBaseId 知识库 ID（可选）
     * @param offset 偏移量（用于分页）
     * @return 相关结果列表
     */
    List<Map<String, Object>> search(String query, int maxResults, Long userId, String knowledgeBaseId, Integer offset);

    /**
     * 简单搜索（无过滤条件）
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @return 相关结果列表
     */
    List<Map<String, Object>> search(String query, int maxResults);

    /**
     * 带过滤条件的搜索
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param userId 用户 ID
     * @param knowledgeBaseId 知识库 ID
     * @return 相关结果列表
     */
    List<Map<String, Object>> searchWithFilter(String query, int maxResults, Long userId, String knowledgeBaseId);

    /**
     * 分页搜索
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param page 页码
     * @return 相关结果列表
     */
    List<Map<String, Object>> searchWithPagination(String query, int maxResults, int page);

    /**
     * 清空所有向量数据
     */
    void clearAll();

    /**
     * 获取统计信息
     * @return 统计信息
     */
    Map<String, Object> getStatistics();

    /**
     * 配置文档分块器参数
     * @param segmentSizeInTokens 每个片段的最大 token 数
     * @param maxOverlapInTokens 片段之间的重叠 token 数
     */
    void configureSplitter(int segmentSizeInTokens, int maxOverlapInTokens);

    /**
     * 获取向量维度
     * @return 向量维度
     */
    int getVectorDimension();

    void embedDocumentChunks(List<DocumentChunk> chunks);
}
