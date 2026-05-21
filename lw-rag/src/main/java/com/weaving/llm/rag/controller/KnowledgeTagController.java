package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.KnowledgeTag;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.rag.service.KnowledgeTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: 35038
 * @Description: 知识库标签管理控制器
 */
@Slf4j
@Tag(name = "知识库标签管理", description = "知识库标签管理接口")
@RestController
@RequestMapping("/v0/knowledge/tag")
public class KnowledgeTagController {

    @Autowired
    private KnowledgeTagService knowledgeTagService;

    @GetMapping("/bases/{baseId}")
    @Operation(summary = "获取知识库标签列表", description = "获取指定知识库下的所有标签")
    public R<List<KnowledgeTag>> getTagsByBaseId(
            @Parameter(description = "知识库 ID", required = true) @PathVariable String baseId) {
        List<KnowledgeTag> tags = knowledgeTagService.getTagsByBaseId(baseId);
        return R.ok(tags);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取标签详情", description = "根据标签 ID 查询详细信息")
    public R<KnowledgeTag> getTag(@Parameter(description = "标签 ID", required = true) @PathVariable String id) {
        KnowledgeTag tag = knowledgeTagService.getById(id);
        if (tag == null) {
            return R.fail("标签不存在");
        }
        return R.ok(tag);
    }

    @PostMapping
    @Operation(summary = "创建标签", description = "为指定知识库创建新标签")
    public R<KnowledgeTag> createTag(@RequestBody Map<String, String> params) {
        String knowledgeBaseId = params.get("knowledgeBaseId");
        String name = params.get("name");
        String color = params.get("color");

        if (knowledgeBaseId == null || knowledgeBaseId.trim().isEmpty()) {
            return R.fail("知识库 ID 不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            return R.fail("标签名称不能为空");
        }

        KnowledgeTag tag = knowledgeTagService.createTag(knowledgeBaseId, name, color);
        return R.ok(tag);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签", description = "更新标签信息")
    public R<KnowledgeTag> updateTag(
            @Parameter(description = "标签 ID", required = true) @PathVariable String id,
            @RequestBody Map<String, String> params) {
        String name = params.get("name");
        String color = params.get("color");

        KnowledgeTag tag = knowledgeTagService.updateTag(id, name, color)
                ? knowledgeTagService.getById(id)
                : null;

        if (tag == null) {
            return R.fail("标签不存在");
        }
        return R.ok(tag);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签", description = "根据标签 ID 删除标签")
    public R<Boolean> deleteTag(
            @Parameter(description = "标签 ID", required = true) @PathVariable String id) {
        boolean result = knowledgeTagService.deleteTag(id);
        return result ? R.ok(true) : R.fail("删除失败");
    }

    @PostMapping("/batch")
    @Operation(summary = "批量创建标签", description = "为指定知识库批量创建多个标签")
    public R<Map<String, Object>> batchCreateTags(
            @RequestBody Map<String, Object> params) {
        String knowledgeBaseId = (String) params.get("knowledgeBaseId");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> tags = (List<Map<String, String>>) params.get("tags");

        if (knowledgeBaseId == null || knowledgeBaseId.trim().isEmpty()) {
            return R.fail("知识库 ID 不能为空");
        }
        if (tags == null || tags.isEmpty()) {
            return R.fail("标签列表不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        for (Map<String, String> tagData : tags) {
            String name = tagData.get("name");
            String color = tagData.get("color");
            if (name != null && !name.trim().isEmpty()) {
                try {
                    knowledgeTagService.createTag(knowledgeBaseId, name, color);
                    successCount++;
                } catch (Exception e) {
                    log.error("创建标签失败：{}", name, e);
                    failCount++;
                }
            } else {
                failCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        return R.ok(result);
    }

    @PutMapping("/reorder")
    @Operation(summary = "调整标签顺序", description = "调整知识库下标签的排序")
    public R<Boolean> reorderTags(@RequestBody Map<String, Object> params) {
        String knowledgeBaseId = (String) params.get("knowledgeBaseId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tagOrders = (List<Map<String, Object>>) params.get("tagOrders");

        if (knowledgeBaseId == null || tagOrders == null) {
            return R.fail("参数错误");
        }

        for (Map<String, Object> order : tagOrders) {
            String tagId = (String) order.get("id");
            Object sortOrderObj = order.get("sortOrder");
            Integer sortOrder = sortOrderObj != null
                    ? (sortOrderObj instanceof Number ? ((Number) sortOrderObj).intValue() : Integer.valueOf(sortOrderObj.toString()))
                    : 0;

            KnowledgeTag tag = knowledgeTagService.getById(tagId);
            if (tag != null && knowledgeBaseId.equals(tag.getKnowledgeBaseId())) {
                tag.setSortOrder(sortOrder);
                knowledgeTagService.updateById(tag);
            }
        }

        return R.ok(true);
    }
}
