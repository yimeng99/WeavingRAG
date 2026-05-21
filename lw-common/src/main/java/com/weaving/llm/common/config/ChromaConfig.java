package com.weaving.llm.common.config;

import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chroma 向量数据库配置
 */
@Slf4j
@Configuration
public class ChromaConfig {

    @Value("${chroma.host:localhost}")
    private String host;

    @Value("${chroma.port:8000}")
    private Integer port;

    @Value("${chroma.collection-name:knowledge_vectors}")
    private String collectionName;

    @Bean
    @ConditionalOnProperty(name = "chroma.enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingStore embeddingStore() {
        try {
            // 使用 langchain4j 的 Chroma 嵌入存储
            String baseUrl = "http://" + host + ":" + port;
            log.info("正在连接 Chroma: {}", baseUrl);

            // 通过工厂方法创建
            EmbeddingStore store = createChromaStore(baseUrl, collectionName);

            if (store != null) {
                log.info("Chroma 连接成功：{}", baseUrl);
                return store;
            }
        } catch (Exception e) {
            log.error("Chroma 连接失败：{}", e.getMessage());
        }

        // Chroma 不可用时，返回内存存储作为降级方案
        log.warn("Chroma 不可用，使用 InMemoryEmbeddingStore 作为降级方案");
        return new dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<>();
    }

    private EmbeddingStore createChromaStore(String baseUrl, String collectionName) {
        try {
            // 反射方式创建，避免编译时依赖
            Class<?> chromaClass = Class.forName("dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore");
            Object builder = chromaClass.getMethod("builder").invoke(null);
            Object store = builder.getClass()
                    .getMethod("baseUrl", String.class)
                    .invoke(builder, baseUrl);
            store = store.getClass()
                    .getMethod("collectionName", String.class)
                    .invoke(store, collectionName);
            return (EmbeddingStore) store.getClass().getMethod("build").invoke(store);
        } catch (Exception e) {
            log.error("创建 Chroma 存储失败：{}", e.getMessage());
            return null;
        }
    }
}
