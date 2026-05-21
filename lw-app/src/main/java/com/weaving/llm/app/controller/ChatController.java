package com.weaving.llm.app.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weaving.llm.common.domain.ChatMessage;
import com.weaving.llm.common.domain.ChatSession;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.service.ChatMessageService;
import com.weaving.llm.common.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "聊天会话", description = "聊天会话和历史消息管理接口")
@RestController
@RequestMapping("/v0/chat")
public class ChatController {
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    /**
     * 获取会话列表 (分页)
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "分页查询聊天会话列表")
    public R<Page<ChatSession>> getChatSessions(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户 ID（可选）") @RequestParam(required = false) Long userId) {
        
        Page<ChatSession> page = new Page<>(current, size);
        if (userId != null) {
            List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
            page.setRecords(sessions);
            page.setTotal(sessions.size());
        } else {
            page = chatSessionService.page(page);
        }
        
        return R.ok(page);
    }
    
    /**
     * 获取单个会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "获取会话详情", description = "根据会话 ID 查询详细信息")
    public R<ChatSession> getChatSession(@PathVariable String sessionId) {
        return R.ok(chatSessionService.getById(sessionId));
    }
    
    /**
     * 创建或更新会话
     */
    @PostMapping("/sessions")
    @Operation(summary = "创建或更新会话", description = "创建新会话或更新现有会话")
    public R<ChatSession> createOrUpdateSession(@RequestBody Map<String, Object> params) {
        String sessionId = (String) params.get("sessionId");
        Long userId = (Long) params.get("userId");
        String title = (String) params.get("title");
        String model = (String) params.get("model");
        String preview = (String) params.get("preview");
        
        return R.ok(chatSessionService.createOrUpdateSession(sessionId, userId, title, model, preview));
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除会话", description = "根据会话 ID 删除聊天会话")
    public R<Boolean> deleteChatSession(@PathVariable String sessionId) {
        return R.ok(chatSessionService.removeById(sessionId));
    }
    
    /**
     * 获取会话的历史消息
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取历史消息", description = "查询指定会话的所有历史消息")
    public R<List<ChatMessage>> getSessionMessages(@PathVariable String sessionId) {
        return R.ok(chatMessageService.getMessagesBySessionId(sessionId));
    }
    
    /**
     * 保存消息
     */
    @PostMapping("/messages")
    @Operation(summary = "保存消息", description = "保存一条聊天消息到数据库")
    public R<ChatMessage> saveMessage(@RequestBody Map<String, Object> params) {
        String sessionId = (String) params.get("sessionId");
        Long userId = (Long) params.get("userId");
        String role = (String) params.get("role");
        String content = (String) params.get("content");
        String model = (String) params.get("model");
        
        return R.ok(chatMessageService.saveMessage(sessionId, userId, role, content, model));
    }
}