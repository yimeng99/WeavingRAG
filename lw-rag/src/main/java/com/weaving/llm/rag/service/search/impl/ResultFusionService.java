package com.weaving.llm.rag.service.search.impl;

import com.weaving.llm.rag.service.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 结果融合服务 - Phase 4
 * 将多路召回结果合并并排序
 */
@Slf4j
@Component
public class ResultFusionService {

    private static final double VECTOR_WEIGHT = 0.6;
    private static final double KEYWORD_WEIGHT = 0.4;
    private static final int RRF_K = 60; // RRF 参数

    /**
     * 融合多路召回结果
     */
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> keywordResults,
                                   FusionStrategy strategy) {
        log.info("开始结果融合，向量召回={}, 关键词召回={}, 策略={}",
                vectorResults.size(), keywordResults.size(), strategy);

        List<SearchResult> fusedResults;

        switch (strategy) {
            case RRF:
                fusedResults = fuseWithRRF(vectorResults, keywordResults);
                break;
            case LINEAR_WEIGHT:
                fusedResults = fuseWithLinearWeight(vectorResults, keywordResults);
                break;
            case MERGE:
            default:
                fusedResults = fuseWithMerge(vectorResults, keywordResults);
                break;
        }

        log.info("结果融合完成，共 {} 条结果", fusedResults.size());
        return fusedResults;
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合
     * 基于排名的融合，无需调参
     */
    private List<SearchResult> fuseWithRRF(List<SearchResult> vectorResults,
                                           List<SearchResult> keywordResults) {
        Map<String, SearchResult> resultMap = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult result = vectorResults.get(i);
            String key = result.getDocId() + "_" + result.getChunkIndex();

            resultMap.putIfAbsent(key, result);

            // RRF 分数计算
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(key, rrfScore, Double::sum);

            // 更新来源
            SearchResult existing = resultMap.get(key);
            if (existing != null && !"vector".equals(existing.getSource())) {
                existing.setSource("both");
            }
        }

        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult result = keywordResults.get(i);
            String key = result.getDocId() + "_" + result.getChunkIndex();

            resultMap.putIfAbsent(key, result);

            // RRF 分数计算
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(key, rrfScore, Double::sum);

            // 更新来源
            SearchResult existing = resultMap.get(key);
            if (existing != null && !"keyword".equals(existing.getSource())) {
                existing.setSource("both");
            }
        }

        // 根据 RRF 分数排序
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    SearchResult result = resultMap.get(entry.getKey());
                    if (result != null) {
                        result.setFinalScore(entry.getValue());
                    }
                    return result;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 线性加权融合
     */
    private List<SearchResult> fuseWithLinearWeight(List<SearchResult> vectorResults,
                                                    List<SearchResult> keywordResults) {
        Map<String, SearchResult> resultMap = new HashMap<>();

        // 处理向量检索结果
        for (SearchResult result : vectorResults) {
            String key = result.getDocId() + "_" + result.getChunkIndex();
            result.setFinalScore(result.getVectorScore() * VECTOR_WEIGHT);
            resultMap.put(key, result);
        }

        // 处理关键词检索结果
        for (SearchResult result : keywordResults) {
            String key = result.getDocId() + "_" + result.getChunkIndex();
            SearchResult existing = resultMap.get(key);

            if (existing != null) {
                // 合并分数
                existing.setKeywordScore(result.getKeywordScore());
                existing.setFinalScore(
                    existing.getVectorScore() * VECTOR_WEIGHT +
                    result.getKeywordScore() * KEYWORD_WEIGHT
                );
                existing.setSource("both");
            } else {
                result.setFinalScore(result.getKeywordScore() * KEYWORD_WEIGHT);
                resultMap.put(key, result);
            }
        }

        // 根据最终分数排序
        return resultMap.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::getFinalScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 简单合并（去重）
     */
    private List<SearchResult> fuseWithMerge(List<SearchResult> vectorResults,
                                             List<SearchResult> keywordResults) {
        Map<String, SearchResult> resultMap = new LinkedHashMap<>();

        // 先添加向量检索结果
        for (SearchResult result : vectorResults) {
            String key = result.getDocId() + "_" + result.getChunkIndex();
            resultMap.put(key, result);
        }

        // 添加关键词检索结果（去重）
        for (SearchResult result : keywordResults) {
            String key = result.getDocId() + "_" + result.getChunkIndex();
            if (!resultMap.containsKey(key)) {
                resultMap.put(key, result);
            } else {
                // 如果已存在，标记为双路召回
                resultMap.get(key).setSource("both");
            }
        }

        // 根据向量分数排序
        return resultMap.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::getVectorScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 分数归一化 (Min-Max)
     */
    public void normalizeScores(List<SearchResult> results) {
        if (results.isEmpty()) return;

        double minScore = results.stream()
                .mapToDouble(SearchResult::getVectorScore)
                .min()
                .orElse(0.0);

        double maxScore = results.stream()
                .mapToDouble(SearchResult::getVectorScore)
                .max()
                .orElse(1.0);

        double range = maxScore - minScore;
        if (range > 0) {
            for (SearchResult result : results) {
                double normalized = (result.getVectorScore() - minScore) / range;
                result.setVectorScore(normalized);
            }
        }
    }

    /**
     * 融合策略枚举
     */
    public enum FusionStrategy {
        RRF("RRF 排名融合"),
        LINEAR_WEIGHT("线性加权融合"),
        MERGE("简单合并");

        private final String description;

        FusionStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
