package com.weaving.llm.common.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 普通 AI 对话服务
 */
@Slf4j
@Service
public class WeavingCharService {

    @Autowired
    private ChatModel chatLanguageModel;
    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 简单对话
     * @param prompt 用户问题
     * @return AI 回答
     */
    public String chat(String prompt) {
        log.info("调用大模型：{}", prompt);
        UserMessage userMessage = UserMessage.from(prompt);
        ChatResponse response = chatLanguageModel.chat(userMessage);
        String aiResponse = response.aiMessage().text();
        log.info("大模型响应：{}", aiResponse);
        return aiResponse;
    }

    /**
     * RAG 增强对话 - 将检索结果和用户问题一起交给大模型
     * @param question 用户问题
     * @param searchResults 检索结果列表
     * @return AI 生成的完整回答
     */
    public String ragChat(String question, List<String> searchResults) {
        // 构建系统提示
        String systemPrompt = buildSystemPrompt(searchResults);
        
        // 构建用户消息
        String userMessage = buildUserMessage(question, searchResults);
        
        log.info("RAG 对话 - 问题：{}, 检索结果数：{}", question, searchResults.size());
        
        try {
            // 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt));
            messages.add(UserMessage.from(userMessage));
            
            // 调用大模型
            ChatResponse response = chatLanguageModel.chat(messages);
            String aiResponse = response.aiMessage().text();
            
            log.info("RAG 对话完成，回答长度：{}", aiResponse.length());
            return aiResponse;
            
        } catch (Exception e) {
            log.error("RAG 对话失败", e);
            // 降级处理：直接调用大模型
            return chat(question);
        }
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt(List<String> searchResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个基于知识库内容的智能问答助手。\n");
        sb.append("请根据以下检索到的相关信息来回答用户的问题。\n");
        sb.append("要求：\n");
        sb.append("1. 优先基于检索到的信息回答问题\n");
        sb.append("2. 如果检索信息与问题不相关，可以说明没有相关信息\n");
        sb.append("3. 回答要准确、简洁、有条理\n");
        sb.append("4. 不要编造检索结果中没有的信息\n");
        sb.append("5. 如果信息不足，可以补充自己的知识，但要说明\n");
        return sb.toString();
    }

    /**
     * 构建用户消息（包含检索结果）
     */
    private String buildUserMessage(String question, List<String> searchResults) {
        StringBuilder sb = new StringBuilder();
        
        // 添加检索结果
        sb.append("【检索到的相关信息】\n");
        for (int i = 0; i < searchResults.size(); i++) {
            sb.append(String.format("[%d] %s\n", i + 1, searchResults.get(i)));
        }
        
        // 添加用户问题
        sb.append("\n【用户问题】\n");
        sb.append(question);
        
        sb.append("\n\n请根据以上信息回答用户的问题：");
        
        return sb.toString();
    }

    /**
     * 简单对话 - 流式响应
     * @param prompt 用户问题
     * @param emitter SSE 发射器
     */
    public void chatStream(String prompt, SseEmitter emitter) {
        log.info("调用大模型（流式）：{}", prompt);
        
        try {
            emitter.send(SseEmitter.event().name("start").data("{\"type\":\"start\"}"));

            ChatRequest chatRequest = ChatRequest.builder()
                    .modelName("qwen3.5-flash")
                    .messages(List.of(UserMessage.from(prompt)))
                    .build();
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String s) {
                    log.info("Partial Response: {}", s);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(s));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    log.info("Complete Response: {}", chatResponse.aiMessage().text());
                    try {
                        emitter.send(SseEmitter.event().name("complete").data(chatResponse.aiMessage().text()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Error during streaming", throwable);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + throwable.getMessage() + "\"}"));
                    } catch (IOException e) {
                        log.error("Error sending SSE event", e);
                        emitter.complete();
                    }

                }
            });
            ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
            String aiResponse = response.aiMessage().text();
            
            // 分块发送（模拟流式效果）
//            int chunkSize = 10;
//            for (int i = 0; i < aiResponse.length(); i += chunkSize) {
//                int end = Math.min(i + chunkSize, aiResponse.length());
//                String chunk = aiResponse.substring(i, end);
//                emitter.send(SseEmitter.event().name("token").data(chunk));
//                Thread.sleep(30);
//            }
//
//            emitter.send(SseEmitter.event().name("complete").data("{\"type\":\"complete\",\"length\":" + aiResponse.length() + "}"));
//            log.info("大模型流式响应完成，长度：{}", aiResponse.length());
            
        } catch (IOException e) {
            log.error("发送 SSE 消息失败", e);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.error("流式响应被中断", e);
//        }
//       catch (Exception e) {
            log.error("大模型调用失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
            } catch (IOException ex) { /* ignore */ }
        }
    }

    /**
     * RAG 增强对话 - 流式响应
     * @param question 用户问题
     * @param searchResults 检索结果列表
     * @param emitter SSE 发射器
     */
    public void ragChatStream(String question, List<String> searchResults, SseEmitter emitter) {
        String systemPrompt = buildSystemPrompt(searchResults);
        String userMessage = buildUserMessage(question, searchResults);
        
        log.info("RAG 流式对话 - 问题：{}, 检索结果数：{}", question, searchResults.size());
        
        try {
            emitter.send(SseEmitter.event().name("start").data("{\"type\":\"start\"}"));
            
            // 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt));
            messages.add(UserMessage.from(userMessage));
            
            // 使用 streamingChatModel 进行流式响应
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .build();
            
            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(partialResponse));
                    } catch (IOException e) {
                        log.error("发送流式 token 失败", e);
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    try {
                        String fullResponse = chatResponse.aiMessage().text();
                        log.info("RAG 流式对话完成，回答长度：{}", fullResponse.length());
                        emitter.send(SseEmitter.event().name("complete").data(fullResponse));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送完成事件失败", e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("RAG 流式对话出错", throwable);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + throwable.getMessage() + "\"}"));
                    } catch (IOException e) {
                        log.error("发送错误事件失败", e);
                    }
                    emitter.complete();
                }
            });
            
        } catch (IOException e) {
            log.error("发送 SSE 消息失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
            } catch (IOException ex) { /* ignore */ }
            emitter.complete();
        }
    }
}
