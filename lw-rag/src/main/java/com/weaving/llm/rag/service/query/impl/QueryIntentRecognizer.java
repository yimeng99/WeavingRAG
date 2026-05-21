package com.weaving.llm.rag.service.query.impl;

import com.weaving.llm.rag.service.query.QueryOptimizationResult;
import com.weaving.llm.rag.service.query.QueryOptimizer;
import com.weaving.llm.rag.service.query.QueryRequest;
import com.weaving.llm.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 查询意图识别器
 */
@Slf4j
@Component
public class QueryIntentRecognizer implements QueryOptimizer {

    private static final Pattern FACTUAL_PATTERN = Pattern.compile(
        "(是什么 | 为什么 | 怎么样 | 如何 | 多少 | 哪里 | 何时 | 谁|什么|定义 | 概念|解释)"
    );

    private static final Pattern CONVERSATIONAL_PATTERN = Pattern.compile(
        "(这个 | 那个 | 它 | 他 | 她|它们 | 咱们 | 我们 | 你|我)"
    );

    @Override
    public QueryOptimizationResult optimize(QueryRequest request) {
        String query = request.getOriginalQuery();
        QueryType queryType = recognizeIntent(query);
        
        log.info("查询意图识别：{} -> {}", query, queryType.getName());
        
        return QueryOptimizationResult.builder()
                .originalQuery(query)
                .optimizedQuery(query)
                .queryType(queryType.name())
                .needRewrite(shouldRewrite(queryType))
                .build();
    }

    /**
     * 识别查询意图
     */
    private QueryType recognizeIntent(String query) {
        if (query == null || query.trim().isEmpty()) {
            return QueryType.AMBIGUOUS;
        }

        query = query.trim();

        // 关键词搜索（很短，没有疑问词）
        if (query.length() < 5 && !FACTUAL_PATTERN.matcher(query).find()) {
            return QueryType.KEYWORD;
        }

        // 口语化查询
        if (CONVERSATIONAL_PATTERN.matcher(query).find()) {
            return QueryType.CONVERSATIONAL;
        }

        // 事实型查询
        if (FACTUAL_PATTERN.matcher(query).find()) {
            return QueryType.FACTUAL;
        }

        // 复杂查询（包含多个问题或较长）
        if (query.length() > 30 || query.contains("，") || query.contains(";")) {
            return QueryType.COMPLEX;
        }

        return QueryType.AMBIGUOUS;
    }

    /**
     * 判断是否需要改写
     */
    private boolean shouldRewrite(QueryType queryType) {
        return queryType == QueryType.CONVERSATIONAL || 
               queryType == QueryType.AMBIGUOUS ||
               queryType == QueryType.COMPLEX;
    }

    @Override
    public boolean supports(QueryType queryType) {
        return true; // 支持所有类型的意图识别
    }
}
