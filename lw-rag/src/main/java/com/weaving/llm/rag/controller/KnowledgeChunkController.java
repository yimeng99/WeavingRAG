package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.rag.service.DocumentChunkService;
import com.weaving.llm.rag.service.KnowledgeDocumentService;
import com.weaving.llm.rag.service.TextChunkingService;
import com.weaving.llm.rag.service.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "知识库切片管理", description = "知识库切片等管理接口")
@RestController
@RequestMapping("/v0/knowledge/chunk")
public class KnowledgeChunkController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private DocumentChunkService documentChunkService;

    @Resource
    private TextChunkingService textChunkingService;


    @Resource
    private VectorStoreService vectorStoreService;


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


    @PostMapping("/reChunk/{docId}")
    @Operation(summary = "重新分片文档", description = "使用新的配置重新分片文档")
    public R<Map<String, Object>> reChunkDocument(@PathVariable String docId) {
        try {
            log.info("接收到重新分片请求，docId={}", docId);

            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc == null) {
                return R.fail("文档不存在");
            }

            String content = doc.getContent();
            if (content == null || content.trim().isEmpty()) {
                return R.fail("文档内容为空，无法分片");
            }

            // 使用文档原有配置
            String chunkingStrategy = doc.getChunkingStrategy();
            Integer chunkSize = doc.getChunkSize();
            if (chunkingStrategy == null) chunkingStrategy = "intelligent";
            if (chunkSize == null || chunkSize <= 0) chunkSize = 1000;

            documentChunkService.deleteChunksByDocId(docId);
            log.info("已删除旧切片，docId={}", docId);

            Map<String, Object> result = new HashMap<>();
            int chunkCount = performChunking(docId, content, chunkingStrategy, chunkSize, doc, result);

            doc.setChunkCount(chunkCount);
            doc.setStatus(1);
            knowledgeDocumentService.updateById(doc);

            result.put("chunkCount", chunkCount);
            return R.ok(result);
        } catch (Exception e) {
            log.error("重新分片失败，docId={}", docId, e);
            return R.fail("重新分片失败：" + e.getMessage());
        }
    }


    /**
     * 执行文档分片
     */
    private int performChunking(String docId, String content, String strategy, int chunkSize,
                                KnowledgeDocument doc, Map<String, Object> result) {
        try {
            String actualStrategy = mapChunkingStrategy(strategy);

            List<Map<String, Object>> chunks = textChunkingService.chunkWithMeta(content, actualStrategy, chunkSize, 0);

            int savedCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> chunkData = chunks.get(i);
                String chunkContent = (String) chunkData.get("content");

                if (chunkContent != null && !chunkContent.trim().isEmpty()) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .docId(docId)
                            .content(chunkContent)
                            .chunkIndex(i)
                            .status(1)
                            .build();
                    documentChunkService.save(chunk);
                    savedCount++;
                }
            }

            // 向量化处理
            log.info("开始向量化处理，docId={}, 切片数={}", docId, savedCount);
            try {
                List<DocumentChunk> savedChunks = documentChunkService.getChunksByDocId(docId);
                if (!savedChunks.isEmpty()) {
                    List<String> chunkTexts = savedChunks.stream()
                            .map(DocumentChunk::getContent)
                            .collect(Collectors.toList());

                    List<String> vectorIds = vectorStoreService.addVectors(
                            chunkTexts,
                            docId,
                            doc.getKnowledgeBaseId(),
                            doc.getUserId(),
                            doc.getTitle()
                    );

                    for (int i = 0; i < savedChunks.size() && i < vectorIds.size(); i++) {
                        DocumentChunk chunk = savedChunks.get(i);
                        chunk.setVectorId(vectorIds.get(i));
                        documentChunkService.updateById(chunk);
                    }
                    log.info("向量化完成，docId={}, 向量数={}", docId, vectorIds.size());
                }
            } catch (Exception e) {
                log.error("向量化失败，docId={}", docId, e);
                result.put("embeddingError", "向量化失败：" + e.getMessage());
            }

            return savedCount;
        } catch (Exception e) {
            log.error("文档分片失败，docId={}", docId, e);
            return 0;
        }
    }

    /**
     * 映射前端策略到后端策略
     */
    private String mapChunkingStrategy(String strategy) {
        switch (strategy) {
            case "intelligent":
                return "paragraph";
            case "char":
                return "char";
            case "page":
                return "paragraph";
            case "heading":
                return "separator";
            case "regex":
                return "separator";
            case "separator":
                return "separator";
            default:
                return "char";
        }
    }



}