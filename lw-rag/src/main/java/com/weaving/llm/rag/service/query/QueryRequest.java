package com.weaving.llm.rag.service.query;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * 查询请求对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryRequest {
    private String originalQuery;      // 用户原始查询
    private String knowledgeBaseId;    // 知识库 ID
    private Long userId;               // 用户 ID
    private List<String> context;      // 对话上下文
    private int maxResults;            // 最大结果数
}
