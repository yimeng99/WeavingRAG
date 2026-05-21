package com.weaving.llm.common.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIModelCreateDTO {
    @NotBlank(message = "模型标识不能为空")
    private String modelIdentifier;

    @NotBlank(message = "模型显示名称不能为空")
    private String displayName;

    private String description;

    private String modelBadge;

    private Integer contextLength;

    private Integer multimodal = 0;

    private java.math.BigDecimal inputPrice;

    private java.math.BigDecimal outputPrice;

    private Integer isActive = 1;
}