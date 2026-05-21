package com.weaving.llm.common.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weaving.llm.common.ai.domain.dto.AIModelCreateDTO;
import com.weaving.llm.common.ai.domain.dto.AIModelServiceConfigCreateDTO;
import com.weaving.llm.common.ai.domain.dto.AIProviderCreateDTO;
import com.weaving.llm.common.ai.domain.entity.AIModel;
import com.weaving.llm.common.ai.domain.entity.AIModelServiceConfig;
import com.weaving.llm.common.ai.domain.entity.AIProvider;
import com.weaving.llm.common.ai.domain.vo.AIModelVO;
import com.weaving.llm.common.ai.domain.vo.AIProviderVO;
import com.weaving.llm.common.ai.mapper.AIModelMapper;
import com.weaving.llm.common.ai.mapper.AIModelServiceConfigMapper;
import com.weaving.llm.common.ai.mapper.AIProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型服务类
 *
 * <p>提供 AI 服务商、模型、模型服务配置的 CRUD 操作。
 * 所有模块可通过引入 lw-common 依赖调用此类。
 */
@Service
@RequiredArgsConstructor
public class AIModelService {

    private final AIProviderMapper providerMapper;
    private final AIModelMapper modelMapper;
    private final AIModelServiceConfigMapper serviceConfigMapper;

    // ==================== Provider 相关 ====================

    /**
     * 创建 AI 服务商
     *
     * @param dto 服务商创建DTO，包含服务商代码、名称、官网、图标等信息
     */
    public void createProvider(AIProviderCreateDTO dto) {
        AIProvider provider = new AIProvider();
        provider.setProviderCode(dto.getProviderCode());
        provider.setProviderName(dto.getProviderName());
        provider.setWebsite(dto.getWebsite());
        provider.setIcon(dto.getIcon());
        provider.setDescription(dto.getDescription());
        provider.setStatus(dto.getStatus());
        providerMapper.insert(provider);
    }

    /**
     * 更新 AI 服务商信息
     *
     * @param id  服务商ID
     * @param dto 服务商更新DTO
     */
    public void updateProvider(Long id, AIProviderCreateDTO dto) {
        AIProvider provider = providerMapper.selectById(id);
        if (provider != null) {
            provider.setProviderName(dto.getProviderName());
            provider.setWebsite(dto.getWebsite());
            provider.setIcon(dto.getIcon());
            provider.setDescription(dto.getDescription());
            provider.setStatus(dto.getStatus());
            providerMapper.updateById(provider);
        }
    }

    /**
     * 删除 AI 服务商
     *
     * @param id 服务商ID
     */
    public void deleteProvider(Long id) {
        providerMapper.deleteById(id);
    }

    /**
     * 查询服务商列表
     *
     * @param status 可选，筛选状态：1-启用，0-禁用
     * @return 服务商VO列表
     */
    public List<AIProviderVO> listProviders(Integer status) {
        LambdaQueryWrapper<AIProvider> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AIProvider::getStatus, status);
        }
        return providerMapper.selectList(wrapper).stream()
                .map(this::toProviderVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取服务商详情
     *
     * @param id 服务商ID
     * @return 服务商VO，未找到返回null
     */
    public AIProviderVO getProvider(Long id) {
        AIProvider provider = providerMapper.selectById(id);
        return provider == null ? null : toProviderVO(provider);
    }

    // ==================== Model 相关 ====================

    /**
     * 创建 AI 模型
     *
     * @param dto 模型创建DTO，包含模型标识、显示名称、描述、价格等信息
     */
    public void createModel(AIModelCreateDTO dto) {
        AIModel model = new AIModel();
        model.setModelIdentifier(dto.getModelIdentifier());
        model.setDisplayName(dto.getDisplayName());
        model.setDescription(dto.getDescription());
        model.setModelBadge(dto.getModelBadge());
        model.setContextLength(dto.getContextLength());
        model.setMultimodal(dto.getMultimodal());
        model.setInputPrice(dto.getInputPrice());
        model.setOutputPrice(dto.getOutputPrice());
        model.setIsActive(dto.getIsActive());
        modelMapper.insert(model);
    }

    /**
     * 更新 AI 模型信息
     *
     * @param id  模型ID
     * @param dto 模型更新DTO
     */
    public void updateModel(Long id, AIModelCreateDTO dto) {
        AIModel model = modelMapper.selectById(id);
        if (model != null) {
            model.setDisplayName(dto.getDisplayName());
            model.setDescription(dto.getDescription());
            model.setModelBadge(dto.getModelBadge());
            model.setContextLength(dto.getContextLength());
            model.setMultimodal(dto.getMultimodal());
            model.setInputPrice(dto.getInputPrice());
            model.setOutputPrice(dto.getOutputPrice());
            model.setIsActive(dto.getIsActive());
            modelMapper.updateById(model);
        }
    }

    /**
     * 删除 AI 模型
     *
     * @param id 模型ID
     */
    public void deleteModel(Long id) {
        modelMapper.deleteById(id);
    }

    /**
     * 查询模型列表
     *
     * @param isActive 可选，筛选启用状态：1-启用，0-停用
     * @return 模型VO列表
     */
    public List<AIModelVO> listModels(Integer isActive) {
        LambdaQueryWrapper<AIModel> wrapper = new LambdaQueryWrapper<>();
        if (isActive != null) {
            wrapper.eq(AIModel::getIsActive, isActive);
        }
        return modelMapper.selectList(wrapper).stream()
                .map(this::toModelVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取模型详情
     *
     * @param id 模型ID
     * @return 模型VO，未找到返回null
     */
    public AIModelVO getModel(Long id) {
        AIModel model = modelMapper.selectById(id);
        return model == null ? null : toModelVO(model);
    }

    /**
     * 根据模型标识获取模型
     *
     * @param identifier 模型唯一标识，如 gpt-4o、qwen3.5
     * @return 模型VO，未找到返回null
     */
    public AIModelVO getModelByIdentifier(String identifier) {
        LambdaQueryWrapper<AIModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIModel::getModelIdentifier, identifier);
        AIModel model = modelMapper.selectOne(wrapper);
        return model == null ? null : toModelVO(model);
    }

    // ==================== Service Config 相关 ====================

    /**
     * 查询模型服务配置列表
     *
     * @param providerId 可选，筛选服务商ID
     * @param modelId    可选，筛选模型ID
     * @return 服务配置列表，按优先级升序
     */
    public List<AIModelServiceConfig> listServiceConfigs(Long providerId, Long modelId) {
        LambdaQueryWrapper<AIModelServiceConfig> wrapper = new LambdaQueryWrapper<>();
        if (providerId != null) {
            wrapper.eq(AIModelServiceConfig::getProviderId, providerId);
        }
        if (modelId != null) {
            wrapper.eq(AIModelServiceConfig::getModelId, modelId);
        }
        wrapper.orderByAsc(AIModelServiceConfig::getPriority);
        return serviceConfigMapper.selectList(wrapper);
    }

    /**
     * 获取模型的可用的服务配置
     *
     * <p>根据模型标识查找启用状态的模型，返回优先级最高的可用服务配置。
     *
     * @param modelIdentifier 模型标识，如 gpt-4o
     * @return 可用的服务配置，未找到返回null
     */
    public AIModelServiceConfig getActiveServiceConfig(String modelIdentifier) {
        // 查找模型
        AIModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AIModel>().eq(AIModel::getModelIdentifier, modelIdentifier)
        );
        if (model == null || model.getIsActive() != 1) {
            return null;
        }
        // 查找可用的服务配置
        return serviceConfigMapper.selectOne(
                new LambdaQueryWrapper<AIModelServiceConfig>()
                        .eq(AIModelServiceConfig::getModelId, model.getId())
                        .eq(AIModelServiceConfig::getStatus, 1)
                        .orderByAsc(AIModelServiceConfig::getPriority)
                        .last("LIMIT 1")
        );
    }

    /**
     * 创建模型服务配置
     *
     * <p>关联服务商与模型，配置API类型、密钥、地址等信息。
     *
     * @param dto 服务配置创建DTO
     */
    public void createServiceConfig(AIModelServiceConfigCreateDTO dto) {
        AIModelServiceConfig config = new AIModelServiceConfig();
        config.setProviderId(dto.getProviderId());
        config.setModelId(dto.getModelId());
        config.setApiType(dto.getApiType());
        config.setApiKey(dto.getApiKey());
        config.setUrl(dto.getUrl());
        config.setStatus(dto.getStatus());
        config.setPriority(dto.getPriority());
        config.setDescription(dto.getDescription());
        serviceConfigMapper.insert(config);
    }

    /**
     * 更新模型服务配置
     *
     * <p>注意：providerId 和 modelId 不可修改，仅可更新密钥、地址、优先级等。
     *
     * @param id  服务配置ID
     * @param dto 服务配置更新DTO
     */
    public void updateServiceConfig(Long id, AIModelServiceConfigCreateDTO dto) {
        AIModelServiceConfig config = serviceConfigMapper.selectById(id);
        if (config != null) {
            config.setApiKey(dto.getApiKey());
            config.setUrl(dto.getUrl());
            config.setStatus(dto.getStatus());
            config.setPriority(dto.getPriority());
            config.setDescription(dto.getDescription());
            serviceConfigMapper.updateById(config);
        }
    }

    /**
     * 删除模型服务配置
     *
     * @param id 服务配置ID
     */
    public void deleteServiceConfig(Long id) {
        serviceConfigMapper.deleteById(id);
    }

    // ==================== 内部转换方法 ====================

    /**
     * 将实体转换为ProviderVO
     */
    private AIProviderVO toProviderVO(AIProvider provider) {
        AIProviderVO vo = new AIProviderVO();
        vo.setId(provider.getId());
        vo.setProviderCode(provider.getProviderCode());
        vo.setProviderName(provider.getProviderName());
        vo.setWebsite(provider.getWebsite());
        vo.setIcon(provider.getIcon());
        vo.setDescription(provider.getDescription());
        vo.setStatus(provider.getStatus());
        return vo;
    }

    /**
     * 将实体转换为ModelVO
     */
    private AIModelVO toModelVO(AIModel model) {
        AIModelVO vo = new AIModelVO();
        vo.setId(model.getId());
        vo.setModelIdentifier(model.getModelIdentifier());
        vo.setDisplayName(model.getDisplayName());
        vo.setDescription(model.getDescription());
        vo.setModelBadge(model.getModelBadge());
        vo.setContextLength(model.getContextLength());
        vo.setMultimodal(model.getMultimodal());
        vo.setInputPrice(model.getInputPrice());
        vo.setOutputPrice(model.getOutputPrice());
        vo.setIsActive(model.getIsActive());
        return vo;
    }
}