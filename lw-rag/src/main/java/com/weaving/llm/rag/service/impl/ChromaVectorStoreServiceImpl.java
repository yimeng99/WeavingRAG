package com.weaving.llm.rag.service.impl;

import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.rag.service.VectorStoreService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.data.document.Metadata.metadata;

/**
 * Chroma 向量存储服务实现
 *
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 基于 LangChain4j 的 Chroma 向量存储实现
 */
@Slf4j
@Service("chromaVectorStoreService")
public class ChromaVectorStoreServiceImpl implements VectorStoreService {

    @Autowired(required = false)
    private EmbeddingStore embeddingStore;

    @Resource
    private EmbeddingModel embeddingModel;

    // 文档分块器
    private DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

    // 向量维度 (bge-large-zh-v1.5 是 1024 维)
    private static final int VECTOR_DIMENSION = 1024;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        if (!isAvailable()) {
            log.warn("Chroma 未配置或不可用，向量搜索功能将禁用");
            return;
        }
        log.info("ChromaVectorStoreService 初始化完成");
    }

    @Override
    public boolean isAvailable() {
        return embeddingStore != null && embeddingModel != null;
    }

    @Override
    public void configureSplitter(int segmentSizeInTokens, int maxOverlapInTokens) {
        this.splitter = DocumentSplitters.recursive(segmentSizeInTokens, maxOverlapInTokens);
        log.info("文档分块器配置更新：segmentSize={}, overlap={}", segmentSizeInTokens, maxOverlapInTokens);
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    @Override
    public Map<String, Object> embedDocument(KnowledgeDocument doc) {
        Map<String, Object> result = new HashMap<>();

        // 检查服务是否可用
        if (!isAvailable()) {
            result.put("success", false);
            result.put("message", "向量存储服务不可用");
            return result;
        }

        try {
            log.info("开始处理文档：{} - {}", doc.getDocId(), doc.getTitle());

            // 1. 文档分块
            Document document = Document.document(doc.getContent());
            List<TextSegment> segments = splitter.split(document);

            log.info("文档分块完成，共 {} 个片段", segments.size());

            if (segments.isEmpty()) {
                result.put("success", false);
                result.put("message", "文档内容为空或无法分割");
                return result;
            }

            // 2. 为每个片段生成向量并添加元数据
            List<TextSegment> segmentsWithMetadata = new ArrayList<>();
            List<String> vectorIds = new ArrayList<>();

            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);

                // 添加元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("docId", doc.getDocId());
                metadata.put("userId", doc.getUserId() != null ? doc.getUserId().toString() : "0");
                metadata.put("knowledgeBaseId", doc.getKnowledgeBaseId() != null ? doc.getKnowledgeBaseId() : "default");
                metadata.put("title", doc.getTitle());
                metadata.put("chunkIndex", String.valueOf(i));

//                TextSegment segmentWithMetadata = TextSegment.from(segment.text(), metadata);
//                segmentsWithMetadata.add(segmentWithMetadata);

                // 生成向量 ID
                String vectorId = doc.getDocId() + "_chunk_" + i;
                vectorIds.add(vectorId);

                log.debug("片段 {}/{} 准备完成", i + 1, segments.size());
            }

            // 3. 批量生成向量并存储
            List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMetadata).content();

            // 4. 添加到向量存储
            embeddingStore.addAll(vectorIds, embeddings, segmentsWithMetadata);

            log.info("成功插入向量数据");

            result.put("success", true);
            result.put("message", "文档向量化完成");
            result.put("docId", doc.getDocId());
            result.put("chunkCount", segments.size());
            result.put("vectorIds", vectorIds);

            log.info("文档向量化完成：{} - {}, 共 {} 个片段", doc.getDocId(), doc.getTitle(), segments.size());

        } catch (Exception e) {
            log.error("文档向量化失败：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "向量化失败：" + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> embedDocuments(List<KnowledgeDocument> documents) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int totalChunks = 0;

        log.info("开始批量处理 {} 个文档", documents.size());

        for (KnowledgeDocument doc : documents) {
            try {
                Map<String, Object> docResult = embedDocument(doc);
                if ((Boolean) docResult.get("success")) {
                    successCount++;
                    totalChunks += (Integer) docResult.get("chunkCount");
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("处理文档 {} 失败：{}", doc.getTitle(), e.getMessage());
            }
        }

        result.put("success", true);
        result.put("totalDocs", documents.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalChunks", totalChunks);

        log.info("批量处理完成：成功 {}, 失败 {}, 总片段数 {}", successCount, failCount, totalChunks);

        return result;
    }

    /**
     * 批量处理多个文档 (异步)
     * @param documents 文档列表
     * @return CompletableFuture
     */
    @Async("taskExecutor")
    public CompletableFuture<Map<String, Object>> embedDocumentsAsync(List<KnowledgeDocument> documents) {
        return CompletableFuture.completedFuture(embedDocuments(documents));
    }

    @Override
    public List<String> addVectors(List<String> texts, String docId, String knowledgeBaseId, Long userId, String title) {
        List<String> vectorIds = new ArrayList<>();

        if (!isAvailable()) {
            log.warn("向量存储服务不可用，跳过向量化处理，docId={}", docId);
            return vectorIds;
        }

        try {
            log.info("开始向量化处理，docId={}, 文本数={}", docId, texts.size());

            if (texts.isEmpty()) {
                return vectorIds;
            }

            // 为每个文本生成向量并添加元数据
            List<TextSegment> segmentsWithMetadata = new ArrayList<>();

            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);

                // 添加元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("docId", docId);
                metadata.put("userId", userId != null ? userId.toString() : "0");
                metadata.put("knowledgeBaseId", knowledgeBaseId != null ? knowledgeBaseId : "default");
                metadata.put("title", title != null ? title : "");
                metadata.put("chunkIndex", String.valueOf(i));

//                TextSegment segmentWithMetadata = TextSegment.from(text, metadata);
//                segmentsWithMetadata.add(segmentWithMetadata);

                // 生成向量 ID
                String vectorId = docId + "_chunk_" + i;
                vectorIds.add(vectorId);

                log.debug("文本 {}/{} 准备完成", i + 1, texts.size());
            }

            // 批量生成向量并存储
            List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMetadata).content();
            embeddingStore.addAll(vectorIds, embeddings, segmentsWithMetadata);

            log.info("成功插入向量数据，docId={}, 向量数={}", docId, vectorIds.size());

        } catch (Exception e) {
            log.error("向量化失败，docId={}", docId, e);
            throw new RuntimeException("向量化失败：" + e.getMessage(), e);
        }

        return vectorIds;
    }

    @Override
    public List<String> addVectors(List<String> texts, String docId) {
        return addVectors(texts, docId, "default", 0L, "");
    }

    @Override
    public void deleteVectorsByDocId(String docId) {
        if (!isAvailable()) {
            log.warn("向量存储服务不可用，跳过删除向量，docId={}", docId);
            return;
        }
        try {
            log.info("开始删除文档 {} 的向量数据", docId);

            // 通过元数据过滤查询要删除的向量
            Filter filter = metadata("docId").isEqualTo(docId);
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
//                    .queryEmbedding(new float[VECTOR_DIMENSION])
                    .maxResults(10000)
                    .filter(filter)
                    .build();

            EmbeddingSearchResult<?> searchResult = embeddingStore.search(searchRequest);
            List<String> idsToDelete = new ArrayList<>();
            for (Object match : searchResult.matches()) {
                if (match instanceof EmbeddingMatch) {
                    idsToDelete.add(((EmbeddingMatch<?>) match).embeddingId());
                }
            }

            if (!idsToDelete.isEmpty()) {
                embeddingStore.removeAll(idsToDelete);
                log.info("成功删除文档 {} 的 {} 个向量数据", docId, idsToDelete.size());
            } else {
                log.info("未找到文档 {} 的向量数据", docId);
            }

        } catch (Exception e) {
            log.error("删除向量失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除向量失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> search(String query, int maxResults,
                                           Long userId, String knowledgeBaseId,
                                           Integer offset) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (!isAvailable()) {
            log.warn("向量存储服务不可用，返回空搜索结果");
            return results;
        }

        try {
            log.info("开始向量检索，查询：{}, 最大结果数：{}, 用户 ID: {}, 知识库 ID: {}",
                    query, maxResults, userId, knowledgeBaseId);

            // 1. 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 2. 构建过滤条件
            Filter filter = null;
            if (userId != null && knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
                filter = metadata("userId").isEqualTo(userId.toString())
                        .and(metadata("knowledgeBaseId").isEqualTo(knowledgeBaseId));
            } else if (userId != null) {
                filter = metadata("userId").isEqualTo(userId.toString());
            } else if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
                filter = metadata("knowledgeBaseId").isEqualTo(knowledgeBaseId);
            }

            // 3. 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(queryEmbedding.vector()))
                    .maxResults(maxResults)
                    .filter(filter)
                    .build();

            // 4. 执行搜索
            EmbeddingSearchResult<?> searchResult = embeddingStore.search(searchRequest);

            // 5. 解析结果
            for (Object matchObj : searchResult.matches()) {
                if (matchObj instanceof EmbeddingMatch) {
                    EmbeddingMatch<?> match = (EmbeddingMatch<?>) matchObj;
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", match.embeddingId());
                    item.put("score", match.score() != null ? match.score() : 0.0);
//                    if (match.embedded() != null && match.embedded() instanceof TextSegment) {
//                        TextSegment segment = (TextSegment) match.embedded();
//                        item.put("docId", segment.metadata("docId"));
//                        item.put("knowledgeBaseId", segment.metadata("knowledgeBaseId"));
//                        item.put("title", segment.metadata("title"));
//                        item.put("content", segment.text());
//                        item.put("chunkIndex", segment.metadata("chunkIndex"));
//                    }
                    results.add(item);
                }
            }

            log.info("向量检索完成，找到 {} 条相关结果", results.size());

        } catch (Exception e) {
            log.error("向量检索失败：{}", e.getMessage(), e);
        }

        return results;
    }

    private MetadataFilterBuilder metadata(String knowledgeBaseId) {
        return new MetadataFilterBuilder(knowledgeBaseId);
    }

    @Override
    public List<Map<String, Object>> search(String query, int maxResults) {
        return search(query, maxResults, null, null, null);
    }

    @Override
    public List<Map<String, Object>> searchWithFilter(String query, int maxResults,
                                                      Long userId, String knowledgeBaseId) {
        return search(query, maxResults, userId, knowledgeBaseId, null);
    }

    @Override
    public List<Map<String, Object>> searchWithPagination(String query, int maxResults, int page) {
        int offset = (page - 1) * maxResults;
        return search(query, maxResults, null, null, offset);
    }

    @Override
    public void clearAll() {
        if (!isAvailable()) {
            log.warn("向量存储服务不可用，跳过清空向量库");
            return;
        }
        try {
            embeddingStore.removeAll();
            log.info("已清空所有向量数据");
        } catch (Exception e) {
            log.error("清空向量库失败：{}", e.getMessage(), e);
            throw new RuntimeException("清空向量库失败", e);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        if (!isAvailable()) {
            stats.put("success", false);
            stats.put("error", "向量存储服务不可用");
            return stats;
        }

        try {
            stats.put("success", true);
            stats.put("collectionName", "knowledge_vectors");
            stats.put("status", "active");
        } catch (Exception e) {
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    @Override
    public Map<String, Object> embedDocumentChunks(List<DocumentChunk> chunks) {
        Map<String, Object> result = new HashMap<>();

        if (!isAvailable()) {
            result.put("success", false);
            result.put("message", "向量存储服务不可用");
            return result;
        }

        if (chunks == null || chunks.isEmpty()) {
            result.put("success", false);
            result.put("message", "切片列表为空");
            return result;
        }

        try {
            log.info("开始向量化切片，共 {} 个切片", chunks.size());

            List<TextSegment> segmentsWithMetadata = new ArrayList<>();
            List<String> vectorIds = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunkId", chunk.getChunkId());
                metadata.put("docId", chunk.getDocId());
                metadata.put("chunkIndex", String.valueOf(chunk.getChunkIndex()));

                TextSegment segment = TextSegment.from(chunk.getContent(), metadata);
                segmentsWithMetadata.add(segment);

                String vectorId = chunk.getDocId() + "_chunk_" + chunk.getChunkIndex();
                vectorIds.add(vectorId);
            }

            List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMetadata).content();
            embeddingStore.addAll(vectorIds, embeddings, segmentsWithMetadata);

            log.info("切片向量化完成，共 {} 个", vectorIds.size());

            result.put("success", true);
            result.put("message", "向量化完成");
            result.put("chunkCount", vectorIds.size());
            result.put("vectorIds", vectorIds);

        } catch (Exception e) {
            log.error("切片向量化失败：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "向量化失败：" + e.getMessage());
        }

        return result;
    }
}
