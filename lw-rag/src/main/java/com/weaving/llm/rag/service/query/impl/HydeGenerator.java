package com.weaving.llm.rag.service.query.impl;

import com.weaving.llm.rag.service.query.QueryOptimizationResult;
import com.weaving.llm.rag.service.query.QueryOptimizer;
import com.weaving.llm.rag.service.query.QueryRequest;
import com.weaving.llm.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HyDE (Hypothetical Document Embeddings) - 假设文档生成器
 * 为复杂问题生成假设性答案，用于提高检索效果
 */
@Slf4j
@Component
public class HydeGenerator implements QueryOptimizer {

    @Autowired(required = false)
    private dev.langchain4j.model.chat.ChatModel chatModel;

    @Override
    public QueryOptimizationResult optimize(QueryRequest request) {
        String query = request.getOriginalQuery();
        
        // 简单版本的 HyDE - 基于模板生成假设答案
        String hydeDoc = generateHydeDocument(query);
        
        log.info("HyDE 生成：{} -> {}", query, hydeDoc);
        
        return QueryOptimizationResult.builder()
                .originalQuery(query)
                .hydeDocument(hydeDoc)
                .build();
    }

    /**
     * 生成假设文档
     */
    private String generateHydeDocument(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        // 根据问题类型生成假设答案
        if (query.contains("多少")) {
            return query.replace("多少", "X 个/名/位");
        }
        
        if (query.contains("为什么")) {
            return "关于" + query.replace("为什么", "").trim() + "的原因有以下几点：第一，...；第二，...；第三，...";
        }
        
        if (query.contains("如何") || query.contains("怎么样")) {
            return query.replace(query.contains("如何") ? "如何" : "怎么样", "...的方法/步骤是") + "。首先...；其次...；最后...";
        }

        // 默认模板
        return "关于\"" + query + "\"的相关信息如下：...";
    }

    @Override
    public boolean supports(QueryType queryType) {
        return queryType == QueryType.COMPLEX || queryType == QueryType.FACTUAL;
    }
}
