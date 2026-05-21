package com.weaving.llm.rag.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 知识库更新请求
 */
@Data
public class KnowledgeBaseUpdateRequest {

    /**
     * 知识库名称
     */
    @Size(max = 100, message = "知识库名称长度不能超过 100 个字符")
    private String name;

    /**
     * 知识库描述
     */
    @Size(max = 500, message = "知识库描述长度不能超过 500 个字符")
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 公开状态
     */
    private Integer isPublic;

    /**
     * 排序
     */
    private Integer sortOrder;
}