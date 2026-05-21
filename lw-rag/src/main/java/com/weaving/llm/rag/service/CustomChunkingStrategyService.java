package com.weaving.llm.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.CustomChunkingStrategy;

import java.util.List;

public interface CustomChunkingStrategyService extends IService<CustomChunkingStrategy> {
    
    List<CustomChunkingStrategy> getByUserId(Long userId);
    
    CustomChunkingStrategy createStrategy(CustomChunkingStrategy strategy);
    
    CustomChunkingStrategy updateStrategy(String id, CustomChunkingStrategy strategy);
    
    boolean deleteStrategy(String id);
}
