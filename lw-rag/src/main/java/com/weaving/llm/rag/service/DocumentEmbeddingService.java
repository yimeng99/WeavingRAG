package com.weaving.llm.rag.service;

import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.rag.service.query.QueryOptimizationService;
import com.weaving.llm.rag.service.query.QueryRequest;
import com.weaving.llm.rag.service.search.AdvancedSearchService;
import com.weaving.llm.rag.service.search.SearchRequest;
import com.weaving.llm.rag.service.search.SearchResult;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author: 渚濇ⅵ
 * @Date: 2025/10/27
 * @Description: 鏂囨。鍚戦噺鍖栨湇鍔? */
@Slf4j
@Service
public class DocumentEmbeddingService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;
    
    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private QueryOptimizationService queryOptimizationService;

    @Autowired
    private AdvancedSearchService advancedSearchService;

    /**
     * 设置分块参数
     * @param segmentSizeInTokens 每个片段的最大 token 数
     * @param maxOverlapInTokens 片段之间的重叠 token 数
     */
    public void configureSplitter(int segmentSizeInTokens, int maxOverlapInTokens) {
        vectorStoreService.configureSplitter(segmentSizeInTokens, maxOverlapInTokens);
        log.info("文档分块器配置更新：segmentSize={}, overlap={}", segmentSizeInTokens, maxOverlapInTokens);
    }
    
    /**
     * 瀵瑰崟涓枃妗ｈ繘琛屽垎鍓插拰鍚戦噺鍖?     * @param docId 鏂囨。 ID
     * @return 澶勭悊缁撴灉
     */
    public Map<String, Object> embedDocument(String docId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 浠庢暟鎹簱鑾峰彇鏂囨。
            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc == null) {
                result.put("success", false);
                result.put("message", "鏂囨。涓嶅瓨鍦?");
                return result;
            }
            
            log.info("寮�濮嬪鐞嗘枃妗ｏ細{} - {}", doc.getDocId(), doc.getTitle());
            
            // 2. 使用向量存储服务进行向量化
            result = vectorStoreService.embedDocument(doc);
            
            if ((Boolean) result.get("success")) {
                // 3. 更新文档状态
                doc.setStatus(1); // 已完成
                doc.setChunkIndex((Integer) result.get("chunkCount"));
                knowledgeDocumentService.updateById(doc);
                
                log.info("文档向量化完成：{} - {}, 共 {} 个片段", doc.getDocId(), doc.getTitle(), result.get("chunkCount"));
            } else {
                // 更新文档状态为失败
                doc.setStatus(2); // 失败
                knowledgeDocumentService.updateById(doc);
            }
            
        } catch (Exception e) {
            log.error("文档向量化失败：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "向量化失败：" + e.getMessage());
            
            // 更新文档状态为失败
            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc != null) {
                doc.setStatus(2); // 失败
                knowledgeDocumentService.updateById(doc);
            }
        }
        
        return result;
    }
    
    /**
     * 批量处理多个文档
     * @param docIds 文档 ID 列表
     * @return 处理统计
     */
    public Map<String, Object> embedDocuments(List<String> docIds) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int totalChunks = 0;
        
        log.info("开始批量处理 {} 个文档", docIds.size());
        
        // 从数据库获取所有文档
        List<KnowledgeDocument> documents = new ArrayList<>();
        for (String docId : docIds) {
            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc != null) {
                documents.add(doc);
            } else {
                log.warn("文档 {} 不存在，跳过", docId);
            }
        }
        
        // 使用向量存储服务批量处理
        if (!documents.isEmpty()) {
            Map<String, Object> batchResult = vectorStoreService.embedDocuments(documents);
            successCount = (Integer) batchResult.get("successCount");
            failCount = (Integer) batchResult.get("failCount");
            totalChunks = (Integer) batchResult.get("totalChunks");
            
            // 更新成功处理的文档状态
            for (KnowledgeDocument doc : documents) {
                doc.setStatus(1);
                knowledgeDocumentService.updateById(doc);
            }
        }
        
        result.put("success", true);
        result.put("totalDocs", documents.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalChunks", totalChunks);
        
        log.info("批量处理完成：成功{}, 失败 {}, 总片段数 {}", successCount, failCount, totalChunks);
        
        return result;
    }
    
    /**
     * 检索相关文档片段 (支持分页和过滤)
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param userId 用户 ID(可选)
     * @param knowledgeBaseId 知识库 ID(可选)
     * @param page 页码
     * @return 相关的文档片段
     */
    public List<Map<String, Object>> searchRelevantSegments(String query, int maxResults,
                                                           Long userId, String knowledgeBaseId, int page) {
        log.info("开始向量检索，查询：{}, 最大结果数：{}, 用户 ID: {}, 知识库 ID: {}, 页码：{}", 
                query, maxResults, userId, knowledgeBaseId, page);
        
        // 1. 查询优化
        QueryRequest queryRequest = QueryRequest.builder()
                .originalQuery(query)
                .knowledgeBaseId(knowledgeBaseId)
                .userId(userId)
                .maxResults(maxResults)
                .build();
        
        String optimizedQuery = queryOptimizationService.optimizeQuery(queryRequest);
        
        // 2. 使用高级检索服务（包含多路召回、融合、重排序）
        SearchRequest searchRequest = advancedSearchService.createSearchRequest(
                optimizedQuery, knowledgeBaseId, userId);
        searchRequest.setMaxResults(maxResults);
        
        List<SearchResult> searchResults = advancedSearchService.search(searchRequest);
        
        // 3. 转换为原有格式
        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchResult result : searchResults) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", result.getId());
            item.put("docId", result.getDocId());
            item.put("knowledgeBaseId", result.getKnowledgeBaseId());
            item.put("title", result.getTitle());
            item.put("content", result.getContent());
            item.put("chunkIndex", result.getChunkIndex());
            item.put("score", result.getFinalScore());
            item.put("rank", result.getRank());
            results.add(item);
        }
        
        return results;
    }
    
    /**
     * 简单检索 (无过滤)
     */
    public List<Map<String, Object>> searchRelevantSegments(String query, int maxResults) {
        return searchRelevantSegments(query, maxResults, null, null, 1);
    }
    
    /**
     * 根据文档 ID 删除对应的向量
     * @param docId 文档 ID
     */
    public void deleteVectorsByDocId(String docId) {
        log.info("删除文档 {} 的向量数据", docId);
        vectorStoreService.deleteVectorsByDocId(docId);
    }
}
