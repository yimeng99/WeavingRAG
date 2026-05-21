package com.weaving.llm.rag.service.query.impl;

import com.weaving.llm.rag.service.query.QueryOptimizationResult;
import com.weaving.llm.rag.service.query.QueryOptimizer;
import com.weaving.llm.rag.service.query.QueryRequest;
import com.weaving.llm.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 关键词提取器
 */
@Slf4j
@Component
public class KeywordExtractor implements QueryOptimizer {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
        "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
        "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "这"
    ));

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    @Override
    public QueryOptimizationResult optimize(QueryRequest request) {
        String query = request.getOriginalQuery();
        List<String> keywords = extractKeywords(query);
        
        log.info("关键词提取：{} -> {}", query, keywords);
        
        return QueryOptimizationResult.builder()
                .originalQuery(query)
                .keywords(keywords)
                .build();
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

    @Override
    public boolean supports(QueryType queryType) {
        return queryType == QueryType.KEYWORD || 
               queryType == QueryType.FACTUAL ||
               queryType == QueryType.COMPLEX;
    }
}
