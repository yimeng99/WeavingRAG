package com.weaving.llm.common.ai.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AIModelVO {
    private Long id;
    private String modelIdentifier;
    private String displayName;
    private String description;
    private String modelBadge;
    private Integer contextLength;
    private Integer multimodal;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Integer isActive;
}