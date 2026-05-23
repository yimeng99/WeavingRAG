package com.weaving.llm.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.rag.mapper.DocumentChunkMapper;
import com.weaving.llm.rag.service.DocumentChunkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DocumentChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk> implements DocumentChunkService {

    @Override
    public List<DocumentChunk> getChunksByDocId(String docId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocId, docId)
               .orderByAsc(DocumentChunk::getChunkIndex);
        return list(wrapper);
    }

    @Override
    public List<DocumentChunk> getChunksByDocIdPaged(String docId, int current, int size) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DocumentChunk::getDocId, docId)
//               .orderByAsc(DocumentChunk::getChunkIndex)
//               .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size));
        return list(wrapper);
    }

    @Override
    public int getChunkCountByDocId(String docId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocId, docId);
        return Math.toIntExact(count(wrapper));
    }

    @Override
    public void deleteChunksByDocId(String docId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocId, docId);
        remove(wrapper);
        log.info("删除文档切片: docId={}", docId);
    }
}
