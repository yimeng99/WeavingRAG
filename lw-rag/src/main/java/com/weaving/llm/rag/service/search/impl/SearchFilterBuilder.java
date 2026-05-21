package com.weaving.llm.rag.service.search.impl;

import com.weaving.llm.rag.service.search.SearchRequest;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * 检索过滤器构建器 - Phase 2
 * 构建权限和过滤条件
 */
@Slf4j
@Component
public class SearchFilterBuilder {

    /**
     * 构建基础过滤条件（租户 + 知识库）
     */
    public Filter buildBaseFilter(SearchRequest request) {
        Filter filter = null;

        // 租户隔离（如果支持多租户）
        if (request.getTenantId() != null && !request.getTenantId().isEmpty()) {
//            filter = metadata("tenantId").isEqualTo(request.getTenantId());
        }

        // 知识库范围过滤
//        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
//            if (request.getKnowledgeBaseIds().size() == 1) {
//                Filter kbFilter = metadata("knowledgeBaseId").isEqualTo(request.getKnowledgeBaseIds().get(0));
//                filter = filter == null ? kbFilter : filter.and(kbFilter);
//            } else {
//                // 多个知识库 ID 暂时不支持，只取第一个
//                Filter kbFilter = metadata("knowledgeBaseId").isEqualTo(request.getKnowledgeBaseIds().get(0));
//                filter = filter == null ? kbFilter : filter.and(kbFilter);
//            }
//        }

        log.debug("基础过滤条件：{}", filter);
        return filter;
    }

    /**
     * 构建用户权限过滤
     */
    public Filter buildUserFilter(SearchRequest request) {
        if (request.getUserId() == null) {
            return null;
        }

        // 用户级权限过滤
        Filter filter = new Filter() {
            @Override
            public boolean test(Object object) {
                return false;
            }
        }; // metadata("userId").isEqualTo(String.valueOf(request.getUserId()));
        log.debug("用户权限过滤：{}", filter);
        return filter;
    }

    /**
     * 构建时间范围过滤
     * Chroma 不支持时间范围过滤，返回 null
     */
    public Filter buildTimeFilter(Map<String, Object> timeRange) {
        log.debug("Chroma 不支持时间范围过滤");
        return null;
    }

    /**
     * 构建元数据过滤
     */
    public Filter buildMetadataFilter(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        Filter filter = null;

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

//            if (value instanceof String) {
//                Filter fieldFilter = metadata(key).isEqualTo((String) value);
//                filter = filter == null ? fieldFilter : filter.and(fieldFilter);
//            } else if (value instanceof Number) {
//                Filter fieldFilter = metadata(key).isEqualTo(String.valueOf(value));
//                filter = filter == null ? fieldFilter : filter.and(fieldFilter);
//            }
        }

        log.debug("元数据过滤：{}", filter);
        return filter;
    }

    /**
     * 构建完整过滤条件
     */
    public Filter buildFullFilter(SearchRequest request) {
        Filter filter = null;

        // 基础过滤
        Filter baseFilter = buildBaseFilter(request);
        if (baseFilter != null) {
            filter = baseFilter;
        }

        // 用户权限过滤
        Filter userFilter = buildUserFilter(request);
        if (userFilter != null) {
            filter = filter == null ? userFilter : filter.and(userFilter);
        }

        // 时间范围过滤（Chroma 不支持）
        // 元数据过滤
        if (request.getFilters() != null && request.getFilters().containsKey("metadata")) {
            Filter metaFilter = buildMetadataFilter(
                (Map<String, Object>) request.getFilters().get("metadata"));
            if (metaFilter != null) {
                filter = filter == null ? metaFilter : filter.and(metaFilter);
            }
        }

        log.info("完整过滤条件：{}", filter);
        return filter;
    }
}
