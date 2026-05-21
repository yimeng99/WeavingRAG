package com.weaving.llm.rag.service.query;

import lombok.Getter;

/**
 * 查询类型枚举
 */
@Getter
public enum QueryType {
    FACTUAL("事实型", "询问具体事实、数据、定义"),
    COMPLEX("复杂型", "需要多步推理的复杂问题"),
    CONVERSATIONAL("口语型", "包含口语化表达、指代词"),
    AMBIGUOUS("模糊型", "意图不明确、信息不完整"),
    KEYWORD("关键词", "纯关键词搜索");

    private final String name;
    private final String description;

    QueryType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
