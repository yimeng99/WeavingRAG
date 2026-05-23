package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.service.RagChatAIStreamService;
import com.weaving.llm.common.service.WeavingCharService;
import com.weaving.llm.rag.service.DocumentEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @Author: 35038
 */
@Slf4j
@Tag(name = "知识库检索", description = "知识库检索接口")
@RestController
@RequestMapping("/v0/knowledge/search")
public class KnowledgeSearchController {


    @Resource
    private DocumentEmbeddingService documentEmbeddingService;

    @Resource
    private RagChatAIStreamService ragChatAIStreamService;

    @Resource
    private WeavingCharService weavingCharService;

    @Resource
    @Qualifier("ragChatExecutor")
    private Executor ragChatExecutor;

    /**
     * 向量检索相关文档片段 (支持过滤、分页)
     */
    @GetMapping("/search")
    @Operation(summary = "向量检索", description = "根据查询向量的量检索相关文档片段")
    public R<Map<String, Object>> search(
            @Parameter(description = "查询文本", required = true) @RequestParam String query,
            @Parameter(description = "最大结果数", example = "5") @RequestParam(defaultValue = "5") int maxResults,
            @Parameter(description = "用户 ID（可选）") @RequestParam(required = false) Long userId,
            @Parameter(description = "知识库 ID（可选）") @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page) {

        log.info("搜索请求：query={}, maxResults={}, userId={}, knowledgeBaseId={}, page={}",
                query, maxResults, userId, knowledgeBaseId, page);

        List<Map<String, Object>> results = documentEmbeddingService.searchRelevantSegments(
                query, maxResults, userId, knowledgeBaseId, page);

        Map<String, Object> response = new HashMap<>();
        response.put("list", results);
        response.put("total", results.size());
        response.put("page", page);
        response.put("pageSize", maxResults);
        response.put("hasMore", results.size() == maxResults);

        return R.ok(response);
    }

    /**
     * 混合检索 (关键词 + 向量)
     */
    @GetMapping("/search/hybrid")
    @Operation(summary = "混合检索", description = "关键词 + 向量混合检索")
    public R<Map<String, Object>> hybridSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int vectorTopK,
            @RequestParam(defaultValue = "10") int keywordTopK,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam(defaultValue = "1") int page) {

        log.info("混合检索：query={}, vectorTopK={}, keywordTopK={}", query, vectorTopK, keywordTopK);

        List<Map<String, Object>> vectorResults = documentEmbeddingService.searchRelevantSegments(
                query, vectorTopK, userId, knowledgeBaseId, page);

        Map<String, Object> response = new HashMap<>();
        response.put("vectorResults", vectorResults);
        response.put("keywordResults", Collections.emptyList());
        response.put("mergedResults", vectorResults);
        response.put("page", page);
        response.put("pageSize", vectorTopK);

        return R.ok(response);
    }

    /**
     * 向量检索
     */
    @PostMapping("/search")
    @Operation(summary = "向量检索", description = "根据查询文本检索相关文档片段")
    public R<Map<String, Object>> search(@RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            Integer maxResults = params.get("maxResults") != null ? Integer.valueOf(params.get("maxResults").toString()) : 10;

            if (query == null || query.trim().isEmpty()) {
                return R.fail("查询内容不能为空");
            }

            log.info("接收到检索请求，query={}, knowledgeBaseId={}, maxResults={}", query, knowledgeBaseId, maxResults);

            List<Map<String, Object>> searchResults = documentEmbeddingService.searchRelevantSegments(
                    query, maxResults, userId, knowledgeBaseId, 1);

            Map<String, Object> result = new HashMap<>();
            result.put("results", searchResults);
            result.put("total", searchResults.size());

            return R.ok(result);

        } catch (Exception e) {
            log.error("检索失败", e);
            return R.fail("检索失败：" + e.getMessage());
        }
    }

    /**
     * RAG 知识库问答 - 流式响应（SSE + TokenStream Flex API）
     */
    @GetMapping(value = "/rag/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG 流式问答", description = "基于知识库的流式问答接口（SSE）")
    public SseEmitter ragChatStream(
            @Parameter(description = "用户问题", required = true) @RequestParam String query,
            @Parameter(description = "知识库 ID（可选）") @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "最大检索结果数", example = "5") @RequestParam(defaultValue = "5") Integer maxResults) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (query == null || query.trim().isEmpty()) {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"success\":false,\"message\":\"问题不能为空\"}"));
                    emitter.complete();
                    return;
                }
                log.info("RAG 流式问答请求：query={}, knowledgeBaseId={}", query, knowledgeBaseId);

                List<Map<String, Object>> searchResults = documentEmbeddingService.searchRelevantSegments(
                        query, maxResults, null, knowledgeBaseId, 1);
                emitter.send(SseEmitter.event()
                        .name("search_results")
                        .data(searchResults));

                List<String> searchContents = searchResults.stream()
                        .map(r -> (String) r.get("content"))
                        .filter(c -> c != null && !c.trim().isEmpty())
                        .toList();

                emitter.send(SseEmitter.event().name("start").data("{\"type\":\"start\"}"));

                String userMessage = buildRagUserMessage(query, searchContents);

                ragChatAIStreamService.chat(userMessage)
                        .onPartialResponse(token -> {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (IOException e) {
                                log.error("发送流式 token 失败", e);
                            }
                        })
                        .onCompleteResponse(response -> {
                            try {
                                String fullResponse = response.aiMessage().text();
                                log.info("RAG 流式对话完成，回答长度：{}", fullResponse.length());
                                emitter.send(SseEmitter.event().name("complete").data(fullResponse));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("发送完成事件失败", e);
                            }
                        })
                        .onError(error -> {
                            log.error("RAG 流式对话出错", error);
                            try {
                                emitter.send(SseEmitter.event().name("error")
                                        .data("{\"error\":\"" + error.getMessage() + "\"}"));
                            } catch (IOException e) {
                                log.error("发送错误事件失败", e);
                            }
                            emitter.complete();
                        })
                        .start();

            } catch (Exception e) {
                log.error("RAG 流式问答失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送失败", ex);
                }
            }
        }, ragChatExecutor);

        emitter.onCompletion(() -> log.info("RAG 流式问答完成"));
        emitter.onTimeout(() -> {
            log.error("RAG 流式问答超时");
            emitter.complete();
        });
        emitter.onError(e -> log.error("RAG 流式问答错误", e));
        return emitter;
    }

    /**
     * RAG 知识库问答 - 普通响应（保留兼容）
     */
    @PostMapping("/rag/chat")
    @Operation(summary = "RAG 问答", description = "基于知识库的问答接口")
    public R<Map<String, Object>> ragChat(@RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            Integer maxResults = params.get("maxResults") != null ? Integer.valueOf(params.get("maxResults").toString()) : 10;

            if (query == null || query.trim().isEmpty()) {
                return R.fail("问题不能为空");
            }

            log.info("RAG 问答请求：query={}, knowledgeBaseId={}", query, knowledgeBaseId);

            List<Map<String, Object>> searchResults = documentEmbeddingService.searchRelevantSegments(
                    query, maxResults, null, knowledgeBaseId, 1);

            log.info("检索到 {} 条相关信息", searchResults.size());

            List<String> searchContents = searchResults.stream()
                    .map(r -> (String) r.get("content"))
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            String answer;
            boolean hasKnowledge;
            if (searchContents.isEmpty()) {
                log.info("未检索到相关信息，直接调用大模型");
                answer = weavingCharService.chat(query);
                hasKnowledge = false;
            } else {
                log.info("使用 RAG 模式生成回答");
                answer = weavingCharService.ragChat(query, searchContents);
                hasKnowledge = true;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("sources", searchResults);
            result.put("sourceCount", searchResults.size());
            result.put("hasKnowledge", hasKnowledge);

            return R.ok(result);

        } catch (Exception e) {
            log.error("RAG 问答失败", e);
            return R.fail("RAG 问答失败：" + e.getMessage());
        }
    }


    /**
     * 构建 RAG 用户消息（包含检索上下文）
     */
    private String buildRagUserMessage(String question, List<String> searchContents) {
        StringBuilder sb = new StringBuilder();

        if (!searchContents.isEmpty()) {
            sb.append("【检索到的相关信息】\n");
            for (int i = 0; i < searchContents.size(); i++) {
                sb.append(String.format("[%d] %s\n", i + 1, searchContents.get(i)));
            }
            sb.append("\n");
        }

        sb.append("【用户问题】\n");
        sb.append(question);
        sb.append("\n\n请根据以上信息回答用户的问题：");

        return sb.toString();
    }
}
