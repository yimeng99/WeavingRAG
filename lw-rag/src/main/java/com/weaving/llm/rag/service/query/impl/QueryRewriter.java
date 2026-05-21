package com.weaving.llm.rag.service.query.impl;

import com.weaving.llm.rag.service.query.QueryOptimizationResult;
import com.weaving.llm.rag.service.query.QueryOptimizer;
import com.weaving.llm.rag.service.query.QueryRequest;
import com.weaving.llm.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询改写器 - 将口语化查询转为规范查询
 */
@Slf4j
@Component
public class QueryRewriter implements QueryOptimizer {

    @Autowired(required = false)
    private dev.langchain4j.model.chat.ChatModel chatModel;

    @Override
    public QueryOptimizationResult optimize(QueryRequest request) {
        String query = request.getOriginalQuery();
        
        // 简单规则改写
        String rewritten = rewriteWithRules(query);
        
        log.info("查询改写：{} -> {}", query, rewritten);
        
        return QueryOptimizationResult.builder()
                .originalQuery(query)
                .optimizedQuery(rewritten)
                .needRewrite(true)
                .build();
    }

    /**
     * 基于规则改写查询
     */
    private String rewriteWithRules(String query) {
        if (query == null) return query;

        String rewritten = query;

        // 替换指代词
        rewritten = rewritten.replaceAll("这个", "该")
                           .replaceAll("那个", "该")
                           .replaceAll("它", "该")
                           .replaceAll("他们", "该群体")
                           .replaceAll("它们", "该对象");

        // 补充疑问词
        if (!rewritten.contains("?") && !rewritten.contains("?")) {
            // 如果是疑问句但没有问号，补充问号
            if (rewritten.startsWith("多少") || rewritten.startsWith("为什么") ||
                rewritten.startsWith("如何") || rewritten.startsWith("怎么样")) {
                rewritten = rewritten + "?";
            }
        }

        // 去除口语化词汇
        rewritten = rewritten.replaceAll("(帮我 | 请问 | 问一下 | 我想问)", "")
                           .trim();

        return rewritten;
    }

    @Override
    public boolean supports(QueryType queryType) {
        return queryType == QueryType.CONVERSATIONAL || 
               queryType == QueryType.AMBIGUOUS;
    }
}
