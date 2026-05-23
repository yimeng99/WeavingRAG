package com.weaving.llm.common.enums;

import lombok.Getter;

/**
 * 切片策略枚举
 */
@Getter
public enum ChunkingStrategyEnum {

    INTELLIGENT("intelligent", "智能切片"),
    CHAR("char", "字符切片"),
    PAGE("page", "页面切片"),
    HEADING("heading", "标题切片"),
    REGEX("regex", "正则切片"),
    SEPARATOR("separator", "分隔符切片");

    private final String code;
    private final String description;

    ChunkingStrategyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChunkingStrategyEnum fromCode(String code) {
        for (ChunkingStrategyEnum strategy : values()) {
            if (strategy.code.equals(code)) {
                return strategy;
            }
        }
        return null;
    }
}