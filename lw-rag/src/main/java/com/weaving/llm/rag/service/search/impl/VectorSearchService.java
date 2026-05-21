package com.weaving.llm.rag.service.search.impl;

import com.weaving.llm.rag.service.search.SearchRequest;
import com.weaving.llm.rag.service.search.SearchResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 向量检索服务 - Phase 3.1
 */
@Slf4j
@Component
public class VectorSearchService {

    @Autowired(required = false)
    private EmbeddingStore embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private SearchFilterBuilder filterBuilder;

    private static final int VECTOR_DIMENSION = 1024;

    /**
     * 检查 Chroma 是否可用
     */
    public boolean isAvailable() {
        return embeddingStore != null;
    }

    /**
     * 执行向量检索
     */
    public List<SearchResult> search(SearchRequest request) {
        // 检查 Chroma 是否可用
        if (!isAvailable()) {
            log.warn("Chroma 不可用，返回空搜索结果");
            return Collections.emptyList();
        }

        try {
            log.info("开始向量检索，query={}, topK={}", request.getQuery(), request.getTopK());

            // 1. 查询向量化
            Embedding queryEmbedding = embeddingModel.embed(request.getQuery()).content();

            // 2. 构建过滤条件
            Filter filter = filterBuilder.buildFullFilter(request);

            // 3. 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
//                    .queryEmbedding(queryEmbedding.vector())
                    .maxResults(request.getTopK())
                    .filter(filter)
                    .build();

            // 4. 执行搜索
            EmbeddingSearchResult searchResult = embeddingStore.search(searchRequest);

            // 5. 解析结果
            List<SearchResult> results = parseSearchResults(searchResult, request.getScoreThreshold());

            log.info("向量检索完成，召回 {} 条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("向量检索失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析搜索结果
     */
    private List<SearchResult> parseSearchResults(EmbeddingSearchResult searchResult, double scoreThreshold) {
        List<SearchResult> results = new ArrayList<>();

        for (Object matchObj : searchResult.matches()) {
            if (matchObj instanceof EmbeddingMatch) {
                EmbeddingMatch<?> match = (EmbeddingMatch<?>) matchObj;
                double score = match.score() != null ? match.score() : 0.0;

                // 阈值过滤
                if (score < scoreThreshold) {
                    continue;
                }

                SearchResult result = null;
                if (match.embedded() != null && match.embedded() instanceof TextSegment) {
                    TextSegment segment = (TextSegment) match.embedded();
//                    result = SearchResult.builder()
//                            .id(match.embeddingId())
//                            .docId(segment.metadata("docId"))
//                            .knowledgeBaseId(segment.metadata("knowledgeBaseId"))
//                            .title(segment.metadata("title"))
//                            .content(segment.text())
//                            .chunkIndex(Integer.valueOf(segment.metadata("chunkIndex")))
//                            .vectorScore(score)
//                            .source("vector")
//                            .build();
                }

                if (result != null) {
                    results.add(result);
                }
            }
        }

        return results;
    }
}
