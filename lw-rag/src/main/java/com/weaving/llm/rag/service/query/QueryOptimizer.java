package com.weaving.llm.rag.service.query;

/**
 * 查询优化器接口
 */
public interface QueryOptimizer {
    
    /**
     * 优化查询
     * @param request 查询请求
     * @return 优化结果
     */
    QueryOptimizationResult optimize(QueryRequest request);
    
    /**
     * 是否支持该类型的查询优化
     * @param queryType 查询类型
     * @return 是否支持
     */
    boolean supports(QueryType queryType);
}
