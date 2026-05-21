package com.weaving.llm.rag.service.query;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 查询优化结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryOptimizationResult {
    private String originalQuery;           // 原始查询
    private String optimizedQuery;          // 优化后的查询
    private String queryType;               // 查询类型
    private List<String> keywords;          // 关键词列表
    private List<String> expandedQueries;   // 扩展查询列表
    private String hydeDocument;            // HyDE 假设文档
    private boolean needRewrite;            // 是否需要改写
}
