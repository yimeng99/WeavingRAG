package com.weaving.llm.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weaving.llm.common.domain.DocumentChunk;

import java.util.List;

public interface DocumentChunkService extends IService<DocumentChunk> {

    List<DocumentChunk> getChunksByDocId(String docId);

    List<DocumentChunk> getChunksByDocIdPaged(String docId, int current, int size);

    List<DocumentChunk> pageListByDocId(String docId);

    int getChunkCountByDocId(String docId);

    void deleteChunksByDocId(String docId);

    void deleteChunksByChunkId(String chunkId);

    List<DocumentChunk> getChunksByDocIds(List<String> docIds);
}
