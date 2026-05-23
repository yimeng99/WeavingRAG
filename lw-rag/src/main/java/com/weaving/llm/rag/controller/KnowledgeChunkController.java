package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.pages.PageDataResult;
import com.weaving.llm.common.pages.PageUtils;
import com.weaving.llm.common.domain.CustomChunkingStrategy;
import com.weaving.llm.rag.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "知识库切片管理", description = "知识库切片等管理接口")
@RestController
@RequestMapping("/v0/knowledge/chunks")
public class KnowledgeChunkController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private DocumentChunkService documentChunkService;

    @Resource
    private TextChunkingService textChunkingService;


    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private DocumentEmbeddingService documentEmbeddingService;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private CustomChunkingStrategyService customChunkingStrategyService;

    // ==================== 切片管理 ====================

    @GetMapping("/documents/{docId}/chunks")
    @Operation(summary = "获取文档切片", description = "获取文档的所有切片")
    public R<List<DocumentChunk>> getChunks(@PathVariable String docId) {
        return R.ok(documentChunkService.getChunksByDocId(docId));
    }

    @GetMapping("/page")
    @Operation(summary = "获取文档切片列表", description = "通过文档 ID 获取切片列表（分页）")
    public R<PageDataResult> getChunksPaged(@RequestParam String docId) {
        PageUtils.startPage();
        List<DocumentChunk> chunks = documentChunkService.pageListByDocId(docId);
        return R.ok(PageUtils.generatePageDataResult(chunks));
    }

    @DeleteMapping("/removeChunk")
    @Operation(summary = "删除某个切片", description = "删除某个切片")
    public R<Boolean> deleteChunk(@RequestParam String chunkId) {
        documentChunkService.removeById(chunkId);
        return R.ok(true);
    }


    @PostMapping("/documents/{docId}/chunk")
    @Operation(summary = "添加切片", description = "为文档添加单个切片")
    public R<DocumentChunk> addChunk(@PathVariable String docId, @RequestBody Map<String, String> params) {
        String content = params.get("content");
        DocumentChunk chunk = DocumentChunk.builder()
                .docId(docId)
                .content(content)
                .chunkIndex(documentChunkService.getChunksByDocId(docId).size())
                .status(1)
                .build();
        documentChunkService.save(chunk);
        return R.ok(chunk);
    }

    @PostMapping("/vectorize")
    @Operation(summary = "切片向量化", description = "对单个切片进行向量化")
    public R<Map<String, Object>> vectorizeChunk(@RequestParam String chunkId) {
        try {
            DocumentChunk chunk = documentChunkService.getById(chunkId);
            if (chunk == null) {
                return R.fail("切片不存在");
            }

            log.info("开始向量化切片：{}", chunkId);

            KnowledgeDocument doc = knowledgeDocumentService.getById(chunk.getDocId());
            if (doc == null) {
                return R.fail("文档不存在");
            }

            List<String> texts = Arrays.asList(chunk.getContent());
            List<String> vectorIds = vectorStoreService.addVectors(
                    texts,
                    chunk.getDocId(),
                    doc.getKnowledgeBaseId() != null ? doc.getKnowledgeBaseId() : "default",
                    doc.getUserId() != null ? doc.getUserId() : 0L,
                    doc.getTitle() != null ? doc.getTitle() : ""
            );

            String vectorId = vectorIds.get(0);
            chunk.setVectorId(vectorId);
            documentChunkService.updateById(chunk);

            Map<String, Object> result = new HashMap<>();
            result.put("vectorId", vectorId);

            log.info("切片向量化完成：{}", chunkId);

            return R.ok(result);

        } catch (Exception e) {
            log.error("切片向量化失败：{}", chunkId, e);
            return R.fail("向量化失败：" + e.getMessage());
        }
    }


    // ==================== 向量化 ====================

    @PostMapping("/documents/{docId}/embed")
    @Operation(summary = "文档向量化", description = "对指定文档进行分割和向量化")
    public R<Map<String, Object>> embedDocument(@PathVariable String docId) {
        log.info("接收到文档向量化请求，文档 ID: {}", docId);
        return R.ok(documentEmbeddingService.embedDocument(docId));
    }

    @PostMapping("/documents/{docId}/vectorize")
    @Operation(summary = "向量化文档", description = "批量向量化单个文档")
    public R<Map<String, Object>> vectorizeDocument(@PathVariable String docId) {
        try {
            log.info("开始向量化文档：{}", docId);
            Map<String, Object> embedResult = documentEmbeddingService.embedDocument(docId);
            return R.ok(embedResult);
        } catch (Exception e) {
            log.error("文档向量化失败：{}", docId, e);
            return R.fail("向量化失败：" + e.getMessage());
        }
    }


    @PostMapping("/config/splitter")
    @Operation(summary = "配置分块参数", description = "配置文档分块参数")
    public R<Map<String, Object>> configureSplitter(
            @RequestParam int segmentSize,
            @RequestParam int overlap) {
        documentEmbeddingService.configureSplitter(segmentSize, overlap);

        Map<String, Object> result = new HashMap<>();
        result.put("segmentSize", segmentSize);
        result.put("overlap", overlap);

        return R.ok(result);
    }

    @PostMapping("/documents/embed/async")
    @Operation(summary = "异步批量向量化", description = "异步批量处理文档向量化")
    public R<Map<String, Object>> embedDocumentsAsync(@RequestBody List<String> docIds) {
        log.info("接收到异步批量文档向量化请求，共 {} 个文档", docIds.size());

        List<KnowledgeDocument> documents = new ArrayList<>();
        for (String docId : docIds) {
            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc != null) {
                documents.add(doc);
            } else {
                log.warn("文档 {} 不存在，跳过", docId);
            }
        }

        try {
            Map<String, Object> result = vectorStoreService.embedDocuments(documents);
            result.put("totalDocs", documents.size());
            result.put("status", "processing");
            result.put("taskId", String.valueOf(System.currentTimeMillis()));

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("异步处理失败：" + e.getMessage());
        }
    }

    // ==================== 分片策略管理 ====================

    @GetMapping("/chunking/strategies")
    @Operation(summary = "获取内置分片策略", description = "获取所有内置的分片策略")
    public R<Map<String, Object>> getChunkingStrategies() {
        return R.ok(textChunkingService.getChunkingStrategies());
    }


    @PostMapping("/chunking/preview")
    @Operation(summary = "分片预览", description = "预览文本分片效果")
    public R<Map<String, Object>> previewChunking(@RequestBody Map<String, Object> params) {
        try {
            String content = (String) params.get("content");
            String strategy = (String) params.getOrDefault("strategy", "char");
            int chunkSize = params.get("chunkSize") != null ? ((Number) params.get("chunkSize")).intValue() : 500;
            int overlap = params.get("overlap") != null ? ((Number) params.get("overlap")).intValue() : 50;
            String separators = (String) params.getOrDefault("separators", "\n|,|;|。");

            if (strategy.startsWith("custom_")) {
                String strategyId = strategy.replace("custom_", "");
                CustomChunkingStrategy customStrategy = customChunkingStrategyService.getById(strategyId);
                if (customStrategy != null) {
                    Map<String, Object> customParams = new HashMap<>();
                    customParams.put("chunkSize", customStrategy.getChunkSize() != null ? customStrategy.getChunkSize() : 500);
                    customParams.put("overlap", customStrategy.getOverlap() != null ? customStrategy.getOverlap() : 50);
                    if (customStrategy.getSeparators() != null) {
                        customParams.put("separators", customStrategy.getSeparators().replace(",", "|"));
                    }
                    if (customStrategy.getMaxChars() != null) {
                        customParams.put("maxChars", customStrategy.getMaxChars());
                    }
                    List<Map<String, Object>> chunks = textChunkingService.chunkCustom(
                            content, customStrategy.getType(), customParams);

                    Map<String, Object> result = new HashMap<>();
                    result.put("chunks", chunks);
                    result.put("total", chunks.size());
                    result.put("strategy", "custom:" + customStrategy.getName());
                    return R.ok(result);
                } else {
                    return R.fail("自定义策略不存在");
                }
            } else {
                List<Map<String, Object>> chunks = textChunkingService.chunkWithMeta(content, strategy, chunkSize, overlap);
                Map<String, Object> result = new HashMap<>();
                result.put("chunks", chunks);
                result.put("total", chunks.size());
                result.put("strategy", strategy);
                return R.ok(result);
            }
        } catch (Exception e) {
            log.error("分片预览失败", e);
            return R.fail(e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    @GetMapping("/content")
    @Operation(summary = "获取文档原文", description = "通过知识库 ID 和文档 ID 获取文档完整内容")
    public R<KnowledgeDocument> getDocumentContent(
            @Parameter(description = "知识库 ID") @RequestParam String knowledgeBaseId,
            @Parameter(description = "文档 ID") @RequestParam String docId
    ) {
        log.info("获取文档原文请求：knowledgeBaseId={}, docId={}", knowledgeBaseId, docId);
        KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
        if (doc == null) {
            return R.fail("文档不存在");
        }
        // 校验是否属于指定知识库
        if (!knowledgeBaseId.equals(doc.getKnowledgeBaseId())) {
            return R.fail("文档不属于该知识库");
        }
        return R.ok(doc);
    }






}