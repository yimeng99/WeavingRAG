package com.weaving.llm.common.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIProviderCreateDTO {
    @NotBlank(message = "服务商代码不能为空")
    private String providerCode;

    @NotBlank(message = "服务商名称不能为空")
    private String providerName;

    private String website;

    private String icon;

    private String description;

    private Integer status = 1;
}