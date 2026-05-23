package com.weaving.llm.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.rag.mapper.KnowledgeDocumentMapper;
import com.weaving.llm.rag.service.KnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 知识库文档服务实现类
 */
@Slf4j
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    @Override
    public List<KnowledgeDocument> getDocumentsByKnowledgeBaseId(String knowledgeBaseId) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(KnowledgeDocument::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public Page<KnowledgeDocument> getDocumentsPageList(Map<String, Object> params) {
        // 创建分页对象（PageHelper 会自动从请求中获取 pageNum, pageSize）
        Page<KnowledgeDocument> page = new Page<>();
        // 调用 Mapper 查询，PageHelper 会自动拦截并分页
//        List<KnowledgeDocument> records = baseMapper.selectDocumentsByKnowledgeBaseId(page, params);
        return page;
    }

    @Override
    public Page<KnowledgeDocument> getDocumentsByKnowledgeBaseIdPaged(Map<String, Object> params) {
        // 创建分页对象
        int current = params.containsKey("current") ? (int) params.get("current") : 1;
        int pageSize = params.containsKey("pageSize") ? (int) params.get("pageSize") : 10;
        Page<KnowledgeDocument> page = new Page<>(current, pageSize);
        // 查询分页数据
//        List<KnowledgeDocument> records = baseMapper.selectDocumentsByKnowledgeBaseId(page, params);
        return page;
    }

    @Override
    public List<KnowledgeDocument> getDocumentsByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getUserId, userId)
                .orderByDesc(KnowledgeDocument::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public KnowledgeDocument saveDocument(Long userId, String knowledgeBaseId, String title, String content, String type, String source) {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .docId(null)
                .userId(userId)
                .knowledgeBaseId(knowledgeBaseId)
                .title(title)
                .content(content)
                .type(type)
                .source(source)
                .status(1)
                .build();
        this.save(document);
        log.info("保存文档：{} - {}", document.getDocId(), title);
        return document;
    }
}
