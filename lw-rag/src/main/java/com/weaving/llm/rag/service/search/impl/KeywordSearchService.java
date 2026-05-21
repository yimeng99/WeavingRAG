package com.weaving.llm.rag.service.search.impl;

import com.weaving.llm.rag.service.search.SearchRequest;
import com.weaving.llm.rag.service.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 关键词检索服务 - Phase 3.2
 * 基于 BM25 算法的关键词检索
 */
@Slf4j
@Component
public class KeywordSearchService {

    @Autowired
    private SearchFilterBuilder filterBuilder;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
        "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
        "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "这"
    ));

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * 执行关键词检索
     * 注意：实际生产环境应使用 Elasticsearch/OpenSearch 等搜索引擎
     * 这里实现简化版本，基于内存匹配
     */
    public List<SearchResult> search(SearchRequest request) {
        try {
            log.info("开始关键词检索，query={}, topK={}", request.getQuery(), request.getTopK());

            // 1. 提取关键词
            List<String> keywords = extractKeywords(request.getQuery());
            if (keywords.isEmpty()) {
                log.warn("未提取到关键词");
                return Collections.emptyList();
            }

            log.info("提取关键词：{}", keywords);

            // 2. 构建过滤条件
//            String filter = filterBuilder.buildFullFilter(request);

            // 3. 执行检索（简化实现，实际应调用搜索引擎）
            List<SearchResult> results = new ArrayList<>(); // searchByKeywords(keywords, request.getTopK(), filter);

            log.info("关键词检索完成，召回 {} 条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("关键词检索失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();

        // 提取中文词汇
        var chineseMatcher = CHINESE_PATTERN.matcher(query);
        while (chineseMatcher.find()) {
            String word = chineseMatcher.group();
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }

        // 提取英文词汇
        var englishMatcher = ENGLISH_PATTERN.matcher(query);
        while (englishMatcher.find()) {
            String word = englishMatcher.group();
            if (word.length() >= 2 && !STOP_WORDS.contains(word.toLowerCase())) {
                keywords.add(word.toLowerCase());
            }
        }

        return keywords;
    }

    /**
     * 基于关键词搜索（简化实现）
     * 实际生产环境应替换为 Elasticsearch/OpenSearch 调用
     */
    private List<SearchResult> searchByKeywords(List<String> keywords, int topK, String filter) {
        // 简化实现：返回空列表
        // 实际应调用搜索引擎的 REST API
        log.debug("关键词检索：keywords={}, filter={}", keywords, filter);

        // TODO: 集成 Elasticsearch/OpenSearch
        // 伪代码示例:
        // SearchRequest esRequest = buildElasticsearchRequest(keywords, topK, filter);
        // SearchResponse esResponse = elasticsearchClient.search(esRequest);
        // return parseElasticsearchResults(esResponse);

        return Collections.emptyList();
    }

    /**
     * 计算 BM25 分数（简化版）
     */
    private double calculateBM25Score(String content, List<String> keywords) {
        if (content == null || keywords.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        int matchedKeywords = 0;

        for (String keyword : keywords) {
            if (content.toLowerCase().contains(keyword.toLowerCase())) {
                matchedKeywords++;
            }
        }

        // 简化的 BM25 分数计算
        score = (double) matchedKeywords / keywords.size();
        return score;
    }
}
