package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.rag.service.DocumentChunkService;
import com.weaving.llm.rag.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "知识库切片管理", description = "知识库切片等管理接口")
@RestController
@RequestMapping("/v0/knowledge/chunk")
public class KnowledgeChunkController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private DocumentChunkService documentChunkService;


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

    /**
     * 获取文档切片列表（分页）
     * @param knowledgeBaseId 知识库 ID
     * @param docId 文档 ID
     * @param current 页码（默认1）
     * @param size 每页数量（默认20）
     */
    @GetMapping("/list")
    @Operation(summary = "获取文档切片列表", description = "通过知识库 ID 和文档 ID 获取切片列表（分页）")
    public R<Map<String, Object>> getDocumentChunks(
            @Parameter(description = "知识库 ID") @RequestParam String knowledgeBaseId,
            @Parameter(description = "文档 ID") @RequestParam String docId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {

        log.info("获取文档切片列表请求：knowledgeBaseId={}, docId={}, current={}, size={}",
                knowledgeBaseId, docId, current, size);

        KnowledgeDocument doc = knowledgeDocumentService.getById(docId);

        if (doc == null) {
            return R.fail("文档不存在");
        }

        // 校验是否属于指定知识库
        if (!knowledgeBaseId.equals(doc.getKnowledgeBaseId())) {
            return R.fail("文档不属于该知识库");
        }

        int total = documentChunkService.getChunkCountByDocId(docId);
        List<DocumentChunk> chunks = documentChunkService.getChunksByDocIdPaged(docId, current, size);

        Map<String, Object> result = new HashMap<>();
        result.put("records", chunks);
        result.put("total", total);
        result.put("current", current);
        result.put("pageSize", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return R.ok(result);
    }
}