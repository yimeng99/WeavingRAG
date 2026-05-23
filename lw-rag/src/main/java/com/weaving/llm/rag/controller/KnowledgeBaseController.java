package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.KnowledgeBase;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.pages.PageDataResult;
import com.weaving.llm.common.pages.PageUtils;
import com.weaving.llm.common.utils.CurrentUserUtil;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseCreateRequest;
import com.weaving.llm.rag.domain.dto.KnowledgeBaseUpdateRequest;
import com.weaving.llm.rag.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
