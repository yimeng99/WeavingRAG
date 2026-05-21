package com.weaving.llm.rag.service.search;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索结果对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult {
    private String id;                      // 文档 ID
    private String docId;                   // 文档 ID
    private String knowledgeBaseId;         // 知识库 ID
    private String title;                   // 标题
    private String content;                 // 内容
    private Integer chunkIndex;             // 切片索引
    private double vectorScore;             // 向量检索分数
    private double keywordScore;            // 关键词检索分数
    private double rerankScore;             // 重排序分数
    private double finalScore;              // 最终分数
    private int rank;                       // 排名
    private String source;                  // 来源 (vector/keyword/both)
    private Map<String, Object> metadata;   // 元数据
}
