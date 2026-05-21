package com.weaving.llm.common.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 服务商实体
 *
 * <p>对应数据库表 ai_providers，存储 AI 服务商信息。
 * 服务商代码 (providerCode) 全局唯一，如 openai、anthropic、google 等。
 */
@Data
@TableName("ai_providers")
public class AIProvider {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务商代码，全局唯一，如 openai */
    private String providerCode;

    /** 服务商显示名称，如 OpenAI */
    private String providerName;

    /** 官网地址 */
    private String website;

    /** 服务商图标CDN地址 */
    private String icon;

    /** 服务商简介 */
    private String description;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}