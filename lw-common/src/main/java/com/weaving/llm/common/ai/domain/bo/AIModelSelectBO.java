package com.weaving.llm.common.ai.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型选择 BO，用于调用方获取模型服务信息
 */
@Data
public class AIModelSelectBO {
    private Long serviceConfigId;
    private String modelIdentifier;
    private String displayName;
    private String apiType;
    private String apiKey;
    private String url;
}