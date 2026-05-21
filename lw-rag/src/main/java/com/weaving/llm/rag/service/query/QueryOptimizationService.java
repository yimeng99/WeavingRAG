package com.weaving.llm.rag.service.query;

import com.weaving.llm.rag.service.query.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * 查询优化服务
 */
@Slf4j
@Service
public class QueryOptimizationService {

    @Autowired
    private List<QueryOptimizer> optimizers;

    /**
     * 优化查询
     * @param request 查询请求
     * @return 优化后的查询
     */
    public String optimizeQuery(QueryRequest request) {
        log.info("开始查询优化：{}", request.getOriginalQuery());

        QueryOptimizationResult result = QueryOptimizationResult.builder()
                .originalQuery(request.getOriginalQuery())
                .optimizedQuery(request.getOriginalQuery())
                .build();

        // 1. 意图识别
        for (QueryOptimizer optimizer : optimizers) {
            if (optimizer instanceof QueryIntentRecognizer) {
                QueryOptimizationResult intentResult = optimizer.optimize(request);
                result.setQueryType(intentResult.getQueryType());
                result.setNeedRewrite(intentResult.isNeedRewrite());
                break;
            }
        }

        // 2. 查询改写（如果需要）
        if (result.isNeedRewrite()) {
            for (QueryOptimizer optimizer : optimizers) {
                if (optimizer instanceof QueryRewriter && optimizer.supports(QueryType.valueOf(result.getQueryType()))) {
                    QueryOptimizationResult rewriteResult = optimizer.optimize(request);
                    result.setOptimizedQuery(rewriteResult.getOptimizedQuery());
                    break;
                }
            }
        }

        // 3. 关键词提取
        for (QueryOptimizer optimizer : optimizers) {
            if (optimizer instanceof KeywordExtractor) {
                QueryOptimizationResult keywordResult = optimizer.optimize(request);
                result.setKeywords(keywordResult.getKeywords());
                break;
            }
        }

        // 4. HyDE 生成（复杂查询）
        if ("COMPLEX".equals(result.getQueryType()) || "FACTUAL".equals(result.getQueryType())) {
            for (QueryOptimizer optimizer : optimizers) {
                if (optimizer instanceof HydeGenerator) {
                    QueryOptimizationResult hydeResult = optimizer.optimize(
                        QueryRequest.builder()
                            .originalQuery(result.getOptimizedQuery())
                            .build()
                    );
                    result.setHydeDocument(hydeResult.getHydeDocument());
                    break;
                }
            }
        }

        log.info("查询优化完成：{} -> {}, 类型：{}, 关键词：{}", 
                result.getOriginalQuery(), 
                result.getOptimizedQuery(),
                result.getQueryType(),
                result.getKeywords());

        // 返回优化后的查询（如果有 HyDE 文档，也可以用于检索）
        return result.getOptimizedQuery();
    }

    /**
     * 获取完整的优化结果
     */
    public QueryOptimizationResult getFullOptimizationResult(QueryRequest request) {
        String optimizedQuery = optimizeQuery(request);
        
        return QueryOptimizationResult.builder()
                .originalQuery(request.getOriginalQuery())
                .optimizedQuery(optimizedQuery)
                .build();
    }
}
