package com.weaving.llm.rag.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 知识库创建请求
 */
@Data
public class KnowledgeBaseCreateRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
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
}