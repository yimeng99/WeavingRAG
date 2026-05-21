package com.weaving.llm.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.rag.mapper.KnowledgeDocumentMapper;
import com.weaving.llm.rag.service.KnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public List<KnowledgeDocument> getDocumentsByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getUserId, userId)
                .orderByDesc(KnowledgeDocument::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public KnowledgeDocument saveDocument(Long userId, String knowledgeBaseId, String title, String content, String type, String source) {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .docId(null) // 使用雪花算法生成
                .userId(userId)
                .knowledgeBaseId(knowledgeBaseId)
                .title(title)
                .content(content)
                .type(type)
                .source(source)
                .status(1) // 默认已完成
                .build();
        this.save(document);
        log.info("保存文档：{} - {}", document.getDocId(), title);
        return document;
    }
}
