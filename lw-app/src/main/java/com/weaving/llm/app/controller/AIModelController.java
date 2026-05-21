package com.weaving.llm.app.controller;

import com.weaving.llm.common.ai.domain.dto.AIModelCreateDTO;
import com.weaving.llm.common.ai.domain.dto.AIModelServiceConfigCreateDTO;
import com.weaving.llm.common.ai.domain.dto.AIProviderCreateDTO;
import com.weaving.llm.common.ai.domain.entity.AIModelServiceConfig;
import com.weaving.llm.common.ai.domain.vo.AIModelVO;
import com.weaving.llm.common.ai.domain.vo.AIProviderVO;
import com.weaving.llm.common.ai.service.AIModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型管理控制器
 *
 * <p>提供 AI 服务商、模型、模型服务配置的 HTTP 接口。
 * 基础路径: /v0/ai
 */
@RestController
@RequestMapping("/v0/ai")
@RequiredArgsConstructor
@Tag(name = "AI模型管理", description = "AI服务商、模型配置管理")
public class AIModelController {

    private final AIModelService aiModelService;

    // ==================== Provider 相关接口 ====================

    /**
     * 创建 AI 服务商
     */
    @PostMapping("/providers")
    @Operation(summary = "创建服务商")
    public void createProvider(@Valid @RequestBody AIProviderCreateDTO dto) {
        aiModelService.createProvider(dto);
    }

    /**
     * 更新 AI 服务商信息
     *
     * @param id 服务商ID
     */
    @PutMapping("/providers/{id}")
    @Operation(summary = "更新服务商")
    public void updateProvider(@PathVariable Long id, @Valid @RequestBody AIProviderCreateDTO dto) {
        aiModelService.updateProvider(id, dto);
    }

    /**
     * 删除 AI 服务商
     *
     * @param id 服务商ID
     */
    @DeleteMapping("/providers/{id}")
    @Operation(summary = "删除服务商")
    public void deleteProvider(@PathVariable Long id) {
        aiModelService.deleteProvider(id);
    }

    /**
     * 查询服务商列表
     *
     * @param status 可选，筛选状态：1-启用，0-禁用
     */
    @GetMapping("/providers")
    @Operation(summary = "查询服务商列表")
    public List<AIProviderVO> listProviders(@RequestParam(required = false) Integer status) {
        return aiModelService.listProviders(status);
    }

    /**
     * 获取服务商详情
     *
     * @param id 服务商ID
     */
    @GetMapping("/providers/{id}")
    @Operation(summary = "获取服务商详情")
    public AIProviderVO getProvider(@PathVariable Long id) {
        return aiModelService.getProvider(id);
    }

    // ==================== Model 相关接口 ====================

    /**
     * 创建 AI 模型
     */
    @PostMapping("/models")
    @Operation(summary = "创建模型")
    public void createModel(@Valid @RequestBody AIModelCreateDTO dto) {
        aiModelService.createModel(dto);
    }

    /**
     * 更新 AI 模型信息
     *
     * @param id 模型ID
     */
    @PutMapping("/models/{id}")
    @Operation(summary = "更新模型")
    public void updateModel(@PathVariable Long id, @Valid @RequestBody AIModelCreateDTO dto) {
        aiModelService.updateModel(id, dto);
    }

    /**
     * 删除 AI 模型
     *
     * @param id 模型ID
     */
    @DeleteMapping("/models/{id}")
    @Operation(summary = "删除模型")
    public void deleteModel(@PathVariable Long id) {
        aiModelService.deleteModel(id);
    }

    /**
     * 查询模型列表
     *
     * @param isActive 可选，筛选启用状态：1-启用，0-停用
     */
    @GetMapping("/models")
    @Operation(summary = "查询模型列表")
    public List<AIModelVO> listModels(@RequestParam(required = false) Integer isActive) {
        return aiModelService.listModels(isActive);
    }

    /**
     * 获取模型详情
     *
     * @param id 模型ID
     */
    @GetMapping("/models/{id}")
    @Operation(summary = "获取模型详情")
    public AIModelVO getModel(@PathVariable Long id) {
        return aiModelService.getModel(id);
    }

    /**
     * 根据模型标识获取模型
     *
     * @param identifier 模型唯一标识，如 gpt-4o、qwen3.5
     */
    @GetMapping("/models/identifier/{identifier}")
    @Operation(summary = "根据标识获取模型")
    public AIModelVO getModelByIdentifier(@PathVariable String identifier) {
        return aiModelService.getModelByIdentifier(identifier);
    }

    // ==================== Service Config 相关接口 ====================

    /**
     * 查询模型服务配置列表
     *
     * @param providerId 可选，筛选服务商ID
     * @param modelId    可选，筛选模型ID
     */
    @GetMapping("/services")
    @Operation(summary = "查询模型服务配置列表")
    public List<AIModelServiceConfig> listServiceConfigs(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long modelId) {
        return aiModelService.listServiceConfigs(providerId, modelId);
    }

    /**
     * 获取模型的可用的服务配置
     *
     * <p>根据模型标识查找启用状态的模型，返回优先级最高的可用服务配置。
     *
     * @param modelIdentifier 模型标识，如 gpt-4o
     */
    @GetMapping("/services/active/{modelIdentifier}")
    @Operation(summary = "获取模型的可用的服务配置")
    public AIModelServiceConfig getActiveServiceConfig(@PathVariable String modelIdentifier) {
        return aiModelService.getActiveServiceConfig(modelIdentifier);
    }

    /**
     * 创建模型服务配置
     *
     * <p>关联服务商与模型，配置API类型、密钥、地址等信息。
     */
    @PostMapping("/services")
    @Operation(summary = "创建模型服务配置")
    public void createServiceConfig(@Valid @RequestBody AIModelServiceConfigCreateDTO dto) {
        aiModelService.createServiceConfig(dto);
    }

    /**
     * 更新模型服务配置
     *
     * @param id 服务配置ID
     */
    @PutMapping("/services/{id}")
    @Operation(summary = "更新模型服务配置")
    public void updateServiceConfig(@PathVariable Long id, @Valid @RequestBody AIModelServiceConfigCreateDTO dto) {
        aiModelService.updateServiceConfig(id, dto);
    }

    /**
     * 删除模型服务配置
     *
     * @param id 服务配置ID
     */
    @DeleteMapping("/services/{id}")
    @Operation(summary = "删除模型服务配置")
    public void deleteServiceConfig(@PathVariable Long id) {
        aiModelService.deleteServiceConfig(id);
    }
}