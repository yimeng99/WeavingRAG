package com.weaving.llm.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.CustomChunkingStrategy;
import com.weaving.llm.rag.mapper.CustomChunkingStrategyMapper;
import com.weaving.llm.rag.service.CustomChunkingStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CustomChunkingStrategyServiceImpl extends ServiceImpl<CustomChunkingStrategyMapper, CustomChunkingStrategy> implements CustomChunkingStrategyService {

    @Override
    public List<CustomChunkingStrategy> getByUserId(Long userId) {
        LambdaQueryWrapper<CustomChunkingStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomChunkingStrategy::getUserId, userId)
               .eq(CustomChunkingStrategy::getDeleted, 0)
               .eq(CustomChunkingStrategy::getStatus, 1)
               .orderByAsc(CustomChunkingStrategy::getCreateTime);
        return list(wrapper);
    }

    @Override
    public CustomChunkingStrategy createStrategy(CustomChunkingStrategy strategy) {
        strategy.setStatus(1);
        save(strategy);
        log.info("创建自定义分片策略: {}", strategy.getName());
        return strategy;
    }

    @Override
    public CustomChunkingStrategy updateStrategy(String id, CustomChunkingStrategy strategy) {
        CustomChunkingStrategy existing = getById(id);
        if (existing == null) return null;
        if (strategy.getName() != null) existing.setName(strategy.getName());
        if (strategy.getDescription() != null) existing.setDescription(strategy.getDescription());
        if (strategy.getType() != null) existing.setType(strategy.getType());
        if (strategy.getChunkSize() != null) existing.setChunkSize(strategy.getChunkSize());
        if (strategy.getOverlap() != null) existing.setOverlap(strategy.getOverlap());
        if (strategy.getSeparators() != null) existing.setSeparators(strategy.getSeparators());
        if (strategy.getMaxChars() != null) existing.setMaxChars(strategy.getMaxChars());
        updateById(existing);
        return existing;
    }

    @Override
    public boolean deleteStrategy(String id) {
        return removeById(id);
    }
}
