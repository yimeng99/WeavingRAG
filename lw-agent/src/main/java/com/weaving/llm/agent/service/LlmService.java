package com.weaving.llm.agent.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 依梦
 * @Date: 2025/10/26
 * @Description: LlMService
 */
@Service
public class LlmService {

    // 从配置文件读�?API Key，如果没有配置则使用默认�?
    public static final String Q_W_API_KEY = System.getProperty("aliyun.api-key", "YOUR_API_KEY_HERE");
    
    // 可配置的模型名称，默认使�?qwen3.5-flash
    public static final String DEFAULT_MODEL_NAME = System.getProperty("aliyun.model-name", "qwen3.5-flash");
    
    // 基础 URL
   public static final String BASE_URL = System.getProperty("aliyun.base-url", "https://dashscope.aliyuncs.com/compatible-mode/v1");
    
    // 超时时间 (默认 2 分钟)
   public static final Duration DEFAULT_TIMEOUT = Duration.parse(System.getProperty("aliyun.timeout", "PT2M"));

    /**
     * 获取指定模型的流式对话模�?
     * @param modelName 模型名称
     * @return 流式对话模型
     */
   public StreamingChatModel getQianWenStreamingModel(String modelName) {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json;charset=utf-8");
       return OpenAiStreamingChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(Q_W_API_KEY)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : DEFAULT_MODEL_NAME)
                .customHeaders(map)
                .timeout(DEFAULT_TIMEOUT) // 设置超时时间
                .build();
    }
    
    /**
     * 获取默认模型的流式对话模�?
     * @return 流式对话模型
     */
    public StreamingChatModel getQianWenStreamingModel() {
        return getQianWenStreamingModel(DEFAULT_MODEL_NAME);
    }
    
    /**
     * 获取指定模型的同步对话模�?
     * @param modelName 模型名称
     * @return 同步对话模型
     */
   public ChatModel getQianWenModel(String modelName) {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json;charset=utf-8");
       return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(Q_W_API_KEY)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : DEFAULT_MODEL_NAME)
                .customHeaders(map)
                .timeout(DEFAULT_TIMEOUT) // 设置超时时间
                .build();
    }
    
    /**
     * 获取默认模型的同步对话模�?
     * @return 同步对话模型
     */
    public ChatModel getQianWenModel() {
        return getQianWenModel(DEFAULT_MODEL_NAME);
    }
    public StreamingChatModel getTextModel() {
        Map<String, String> map = new HashMap();
        map.put("Content-Type", "application/json;charset=utf-8");

        StreamingChatModel model = OpenAiStreamingChatModel
                .builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .apiKey("demo")
                .customHeaders(map)
                .build();
        return model;
    }

    public StreamingChatModel getModel() {
        return this.getTextModel();
    }

    public ChatModel getChatLanguageModel() {
//        QwenChatModel qwenChatModel = QwenChatModel.builder()
//                .apiKey(System.getenv("aliQwen-api"))
//                .modelName("qwen-plus")
//                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
//                .build();
        return OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .modelName("gpt-4o-mini")
                .apiKey("demo")
                .build();
    }


    public void chat (String text) {
        StreamingChatModel model = this.getModel();
        model.chat(text, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 使用PrintWriter以UTF-8编码输出
                PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
                out.println("onPartialResponse: " + partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
                out.println("onCompleteResponse: " + completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });

    }
}
