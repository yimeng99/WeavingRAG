package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.KnowledgeBase;
import com.weaving.llm.common.domain.KnowledgeDocument;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.pages.PageDataResult;
import com.weaving.llm.common.pages.PageUtils;
import com.weaving.llm.common.utils.CurrentUserUtil;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseCreateRequest;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseUpdateRequest;
import com.weaving.llm.rag.service.DocumentEmbeddingService;
import com.weaving.llm.rag.service.KnowledgeBaseService;
import com.weaving.llm.rag.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 知识库管理控制器
 */
@Slf4j
@Tag(name = "知识库管理", description = "知识库管理接口")
@RestController
@RequestMapping("/v0/knowledge")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private DocumentEmbeddingService documentEmbeddingService;


    @GetMapping("/bases")
    @Operation(summary = "获取知识库列表", description = "查询所有知识库或指定用户的知识库列表")
    public R<PageDataResult> getKnowledgeBases(@ModelAttribute KnowledgeBase knowledgeBase) {
        PageUtils.startPage();
        List<KnowledgeBase> bases = knowledgeBaseService.pageList(knowledgeBase);
        return R.ok(PageUtils.generatePageDataResult(bases));
    }

    @GetMapping("/bases/{id}")
    @Operation(summary = "获取知识库详情", description = "根据知识库 ID 查询详细信息")
    public R<KnowledgeBase> getKnowledgeBase(@PathVariable String id) {
        return R.ok(knowledgeBaseService.getById(id));
    }

    @PostMapping("/bases")
    @Operation(summary = "创建知识库", description = "创建新的知识库")
    public R<KnowledgeBase> createKnowledgeBase(@RequestBody @Validated KnowledgeBaseCreateRequest request) {
        Long userId = CurrentUserUtil.getUserId();
        return R.ok(knowledgeBaseService.createBase(userId, request));
    }

    @PutMapping("/bases/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库信息")
    public R<Boolean> updateKnowledgeBase(@PathVariable String id, @RequestBody @Validated KnowledgeBaseUpdateRequest request) {
        return R.ok(knowledgeBaseService.updateBase(id, request));
    }

    @DeleteMapping("/bases/{id}")
    @Operation(summary = "删除知识库", description = "根据知识库 ID 删除知识库")
    public R<Boolean> deleteKnowledgeBase(@PathVariable String id) {
        return R.ok(knowledgeBaseService.deleteBase(id));
    }

    @PostMapping("/bases/vectorizeAll")
    @Operation(summary = "向量化知识库所有文档", description = "批量向量化知识库中的所有文档")
    public R<Map<String, Object>> vectorizeAllDocuments(@RequestParam String baseId) {
        try {
            log.info("开始向量化知识库所有文档：{}", baseId);

            List<KnowledgeDocument> documents = knowledgeDocumentService.getDocumentsByKnowledgeBaseId(baseId);

            if (documents.isEmpty()) {
                return R.fail("知识库中没有文档");
            }

            int successCount = 0;
            int failCount = 0;
            int totalChunks = 0;

            for (KnowledgeDocument doc : documents) {
                try {
                    Map<String, Object> embedResult = documentEmbeddingService.embedDocument(doc.getDocId());
                    if ((Boolean) embedResult.get("success")) {
                        successCount++;
                        totalChunks += (Integer) embedResult.get("chunkCount");
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("向量化文档失败：{}", doc.getDocId(), e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalDocs", documents.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalChunks", totalChunks);

            return R.ok(result);

        } catch (Exception e) {
            log.error("批量向量化失败：{}", baseId, e);
            return R.fail("批量向量化失败：" + e.getMessage());
        }
    }
}
