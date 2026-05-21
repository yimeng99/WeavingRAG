package com.weaving.llm.common.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIModelServiceConfigCreateDTO {
    @NotNull(message = "服务商ID不能为空")
    private Long providerId;

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotBlank(message = "API类型不能为空")
    private String apiType;

    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    @NotBlank(message = "API地址不能为空")
    private String url;

    private Integer status = 1;

    private Integer priority = 0;

    private String description;
}