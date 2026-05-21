package com.weaving.llm.common.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模型服务配置实体
 *
 * <p>对应数据库表 ai_model_services，关联服务商与模型，配置具体的API接入信息。
 * 一个服务商可以提供多个模型，一个模型也可以被多个服务商提供。
 * 通过 priority 字段实现负载均衡，数字越小优先级越高。
 */
@Data
@TableName("ai_model_services")
public class AIModelServiceConfig {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的服务商ID (ai_providers.id) */
    private Long providerId;

    /** 关联的模型ID (ai_models.id) */
    private Long modelId;

    /** 接口类型：openai, anthropic, gemini, zhipu, minimax, moonshot, qwen, deepseek, hunyuan, doubao */
    private String apiType;

    /** API密钥（AES-256-GCM加密存储） */
    private String apiKey;

    /** API端点URL */
    private String url;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 优先级，数字越小优先级越高（用于负载均衡） */
    private Integer priority;

    /** 该配置的备注说明 */
    private String description;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}