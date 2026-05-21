package com.weaving.llm.common.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型实体
 *
 * <p>对应数据库表 ai_models，存储 AI 模型基础信息。
 * 模型标识 (modelIdentifier) 全局唯一，如 gpt-4o、claude-3-5-sonnet 等。
 * 模型独立于服务商存在，一个模型可被多个服务商提供。
 */
@Data
@TableName("ai_models")
public class AIModel {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型唯一标识，全局唯一，如 gpt-4o、qwen3.5 */
    private String modelIdentifier;

    /** 模型显示名称，如 GPT-4o */
    private String displayName;

    /** 模型描述，用于展示给用户 */
    private String description;

    /** 模型徽章标签，如"多模态"、"代码增强" */
    private String modelBadge;

    /** 最大上下文长度（千tokens） */
    private Integer contextLength;

    /** 是否支持多模态：0-否，1-是 */
    private Integer multimodal;

    /** 参考输入价格（元/千tokens），仅供参考 */
    private BigDecimal inputPrice;

    /** 参考输出价格（元/千tokens），仅供参考 */
    private BigDecimal outputPrice;

    /** 是否全局可用：0-停用，1-启用 */
    private Integer isActive;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}