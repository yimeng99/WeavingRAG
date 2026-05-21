package com.weaving.llm.rag.service.search;

import com.weaving.llm.rag.service.search.impl.*;
import com.weaving.llm.rag.service.search.impl.ResultFusionService.FusionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 高级检索服务 - 整合所有检索阶段
 * 
 * Phase 1: 查询理解与优化
 * Phase 2: 权限与过滤条件构建
 * Phase 3: 多路召回（向量 + 关键词）
 * Phase 4: 结果融合与排序
 * Phase 5: 结果优化与重排序
 * Phase 6: 响应构建与缓存
 */
@Slf4j
@Service
public class AdvancedSearchService {

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private KeywordSearchService keywordSearchService;

    @Autowired
    private ResultFusionService fusionService;

    @Autowired
    private RerankService rerankService;

    @Autowired
    private SearchCacheService cacheService;

    /**
     * 执行高级检索
     */
    public List<SearchResult> search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始高级检索 ==========");
        log.info("查询：{}, topK={}, maxResults={}", request.getQuery(), request.getTopK(), request.getMaxResults());

        try {
            // Phase 6: 尝试从缓存获取
            List<SearchResult> cachedResults = cacheService.getFromCache(request);
            if (cachedResults != null) {
                log.info("缓存命中，耗时：{}ms", System.currentTimeMillis() - startTime);
                return cachedResults;
            }

            // Phase 3: 多路召回
            List<SearchResult> vectorResults = Collections.emptyList();
            List<SearchResult> keywordResults = Collections.emptyList();

            if (request.isEnableVectorSearch()) {
                vectorResults = vectorSearchService.search(request);
            }

            if (request.isEnableKeywordSearch()) {
                keywordResults = keywordSearchService.search(request);
            }

            log.info("多路召回完成：向量={}, 关键词={}", vectorResults.size(), keywordResults.size());

            // Phase 4: 结果融合
            List<SearchResult> fusedResults = fusionService.fuse(
                    vectorResults,
                    keywordResults,
                    FusionStrategy.RRF // 使用 RRF 融合
            );

            // Phase 5: 重排序和优化
            List<SearchResult> finalResults = rerankService.rerank(fusedResults, request);

            // Phase 6: 写入缓存
            cacheService.putToCache(request, finalResults);

            long endTime = System.currentTimeMillis();
            log.info("========== 检索完成，耗时：{}ms ==========", (endTime - startTime));

            return finalResults;

        } catch (Exception e) {
            log.error("高级检索失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 创建默认检索请求
     */
    public SearchRequest createSearchRequest(String query, String knowledgeBaseId, Long userId) {
        return SearchRequest.builder()
                .query(query)
                .userId(userId)
                .knowledgeBaseIds(knowledgeBaseId != null ? Collections.singletonList(knowledgeBaseId) : null)
                .topK(20)
                .maxResults(10)
                .scoreThreshold(0.3)  // 降低默认阈值
                .enableVectorSearch(true)
                .enableKeywordSearch(true)
                .enableRerank(true)
                .build();
    }

    /**
     * 获取缓存统计
     */
    public SearchCacheService.CacheStats getCacheStats() {
        return cacheService.getCacheStats();
    }
}
