package com.weaving.llm.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.KnowledgeBase;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseCreateRequest;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseUpdateRequest;
import com.weaving.llm.rag.mapper.KnowledgeBaseMapper;
import com.weaving.llm.rag.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Override
    public List<KnowledgeBase> getBasesByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
               .eq(KnowledgeBase::getDeleted, 0)
               .orderByAsc(KnowledgeBase::getSortOrder)
               .orderByDesc(KnowledgeBase::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public KnowledgeBase createBase(Long userId, KnowledgeBaseCreateRequest request) {
        KnowledgeBase base = KnowledgeBase.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor() != null ? request.getColor() : "#667eea")
                .docCount(0)
                .status(1)
                .isPublic(0)
                .sortOrder(0)
                .build();
        save(base);
        log.info("创建知识库: id={}, name={}", base.getId(), request.getName());
        return base;
    }

    @Override
    @Transactional
    public boolean updateBase(String id, KnowledgeBaseUpdateRequest request) {
        KnowledgeBase base = getById(id);
        if (base == null) {
            return false;
        }
        if (request.getName() != null) base.setName(request.getName());
        if (request.getDescription() != null) base.setDescription(request.getDescription());
        if (request.getIcon() != null) base.setIcon(request.getIcon());
        if (request.getColor() != null) base.setColor(request.getColor());
        if (request.getStatus() != null) base.setStatus(request.getStatus());
        if (request.getIsPublic() != null) base.setIsPublic(request.getIsPublic());
        if (request.getSortOrder() != null) base.setSortOrder(request.getSortOrder());
        return updateById(base);
    }

    @Override
    @Transactional
    public boolean deleteBase(String id) {
        log.info("删除知识库: id={}", id);
        return removeById(id);
    }

    @Override
    @Transactional
    public void incrementDocCount(String baseId) {
        KnowledgeBase base = getById(baseId);
        if (base != null) {
            base.setDocCount(base.getDocCount() == null ? 1 : base.getDocCount() + 1);
            updateById(base);
        }
    }

    @Override
    @Transactional
    public void decrementDocCount(String baseId) {
        KnowledgeBase base = getById(baseId);
        if (base != null && base.getDocCount() != null && base.getDocCount() > 0) {
            base.setDocCount(base.getDocCount() - 1);
            updateById(base);
        }
    }
}
