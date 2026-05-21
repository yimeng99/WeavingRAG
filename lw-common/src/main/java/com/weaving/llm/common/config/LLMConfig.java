package com.weaving.llm.common.config;

import com.weaving.llm.common.service.RagChatAIStreamService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 依梦
 * @Date: 2025/10/24
 * @Description: LLMConfig
 */
@Slf4j
@Configuration
public class LLMConfig {

    @Value("${aliyun.api-key:YOUR_API_KEY_HERE}")
    private String apiKey;
    
    @Value("${aliyun.model-name:qwen3.5-flash}")
    private String modelName;
    
    @Value("${aliyun.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;
    
    @Value("${aliyun.timeout:PT2M}")
    private String timeoutDuration;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.embedding-model:quentinz/bge-large-zh-v1.5}")
    private String ollamaEmbeddingModel;

    /**
     * 创建同步对话模型
     * @return ChatModel 实例
     */
    @Bean
    public ChatModel chatLanguageModel() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=utf-8");
        
        log.info("初始化 ChatLanguageModel，超时时间：{}", timeoutDuration);
        
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .customHeaders(headers)
                .timeout(Duration.parse(timeoutDuration))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=utf-8");
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .customHeaders(headers)
                .timeout(Duration.parse(timeoutDuration))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
    
    /**
     * 创建嵌入模型（用于生成向量）- 使用 Ollama
     * @return EmbeddingModel 实例
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化 EmbeddingModel，使用 Ollama，模型：{}，地址：{}", 
                ollamaEmbeddingModel, ollamaBaseUrl);
        
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaEmbeddingModel)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    /**
     * 创建 RAG 流式对话 AI Service（TokenStream Flex API）
     * @param streamingChatModel 流式对话模型
     * @return RagChatAIStreamService 实例
     */
    @Bean
    public RagChatAIStreamService ragChatAIStreamService(StreamingChatModel streamingChatModel) {
        log.info("初始化 RagChatAIStreamService，使用 TokenStream Flex API");
        return AiServices.builder(RagChatAIStreamService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }

}
