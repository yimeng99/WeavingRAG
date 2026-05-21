package com.weaving.llm.rag.service.search;

/**
 * 检索阶段枚举
 */
public enum SearchPhase {
    VECTOR("向量检索"),
    KEYWORD("关键词检索"),
    HYBRID("混合检索"),
    RERANK("重排序");

    private final String description;

    SearchPhase(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
