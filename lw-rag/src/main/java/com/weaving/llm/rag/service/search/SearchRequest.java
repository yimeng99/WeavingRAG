package com.weaving.llm.rag.service.search;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 检索请求对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {
    private String query;                    // 查询文本
    private String tenantId;                 // 租户 ID
    private Long userId;                     // 用户 ID
    private List<String> knowledgeBaseIds;   // 知识库 ID 列表
    private int topK;                        // 召回数量
    private int maxResults;                  // 最终返回数量
    private double scoreThreshold;           // 分数阈值
    private Map<String, Object> filters;     // 元数据过滤条件
    private boolean enableVectorSearch;      // 启用向量检索
    private boolean enableKeywordSearch;     // 启用关键词检索
    private boolean enableRerank;            // 启用重排序
}
