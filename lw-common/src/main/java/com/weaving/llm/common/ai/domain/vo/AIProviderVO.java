package com.weaving.llm.common.ai.domain.vo;

import lombok.Data;

@Data
public class AIProviderVO {
    private Long id;
    private String providerCode;
    private String providerName;
    private String website;
    private String icon;
    private String description;
    private Integer status;
}