package com.weaving.llm.rag.service.search.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.weaving.llm.rag.service.search.SearchRequest;
import com.weaving.llm.rag.service.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 检索结果缓存服务 - Phase 6
 * 使用 Caffeine 实现本地缓存
 */
@Slf4j
@Component
public class SearchCacheService {

    // 缓存：热点查询（10 分钟 TTL）
    private final Cache<String, List<SearchResult>> hotCache;

    // 缓存：普通查询（30 分钟 TTL）
    private final Cache<String, List<SearchResult>> normalCache;

    public SearchCacheService() {
        this.hotCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        this.normalCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(SearchRequest request) {
        StringBuilder key = new StringBuilder();
        key.append("query:").append(request.getQuery());

        if (request.getTenantId() != null) {
            key.append("|tenant:").append(request.getTenantId());
        }

        if (request.getUserId() != null) {
            key.append("|user:").append(request.getUserId());
        }

        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
            key.append("|kb:").append(String.join(",", request.getKnowledgeBaseIds()));
        }

        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            key.append("|filters:").append(request.getFilters().hashCode());
        }

        return key.toString();
    }

    /**
     * 从缓存获取结果
     */
    public List<SearchResult> getFromCache(SearchRequest request) {
        String cacheKey = generateCacheKey(request);

        // 先查热点缓存
        List<SearchResult> results = hotCache.getIfPresent(cacheKey);
        if (results != null) {
            log.info("缓存命中 (hot): {}", request.getQuery());
            return results;
        }

        // 再查普通缓存
        results = normalCache.getIfPresent(cacheKey);
        if (results != null) {
            log.info("缓存命中 (normal): {}", request.getQuery());
            // 提升到热点缓存
            hotCache.put(cacheKey, results);
            return results;
        }

        log.info("缓存未命中：{}", request.getQuery());
        return null;
    }

    /**
     * 写入缓存
     */
    public void putToCache(SearchRequest request, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        String cacheKey = generateCacheKey(request);

        // 高频查询放入热点缓存
        if (results.size() > 0) {
            hotCache.put(cacheKey, results);
            log.debug("写入热点缓存：{}", request.getQuery());
        } else {
            normalCache.put(cacheKey, results);
            log.debug("写入普通缓存：{}", request.getQuery());
        }
    }

    /**
     * 清除缓存
     */
    public void invalidateCache(String query, String tenantId, Long userId) {
        StringBuilder key = new StringBuilder();
        key.append("query:").append(query);

        if (tenantId != null) {
            key.append("|tenant:").append(tenantId);
        }

        if (userId != null) {
            key.append("|user:").append(userId);
        }

        String cacheKey = key.toString();
        hotCache.invalidate(cacheKey);
        normalCache.invalidate(cacheKey);

        log.info("清除缓存：{}", cacheKey);
    }

    /**
     * 清除所有缓存
     */
    public void invalidateAll() {
        hotCache.invalidateAll();
        normalCache.invalidateAll();
        log.info("清除所有缓存");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                hotCache.estimatedSize(),
                normalCache.estimatedSize(),
                hotCache.stats().hitRate(),
                normalCache.stats().hitRate()
        );
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final long hotCacheSize;
        public final long normalCacheSize;
        public final double hotCacheHitRate;
        public final double normalCacheHitRate;

        public CacheStats(long hotCacheSize, long normalCacheSize,
                         double hotCacheHitRate, double normalCacheHitRate) {
            this.hotCacheSize = hotCacheSize;
            this.normalCacheSize = normalCacheSize;
            this.hotCacheHitRate = hotCacheHitRate;
            this.normalCacheHitRate = normalCacheHitRate;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hotSize=%d, normalSize=%d, hotHitRate=%.2f%%, normalHitRate=%.2f%%}",
                    hotCacheSize, normalCacheSize,
                    hotCacheHitRate * 100, normalCacheHitRate * 100);
        }
    }
}
