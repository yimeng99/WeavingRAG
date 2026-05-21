package com.weaving.llm.rag.service.search.impl;

import com.weaving.llm.rag.service.search.SearchRequest;
import com.weaving.llm.rag.service.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重排序服务 - Phase 5
 * 对初步检索结果进行精细优化
 */
@Slf4j
@Component
public class RerankService {

    private static final double DEFAULT_THRESHOLD = 0.3;  // 降低默认阈值
    private static final int RERANK_TOP_K = 30;
    private static final double MMR_LAMBDA = 0.7; // MMR 多样性参数

    /**
     * 执行重排序和优化
     */
    public List<SearchResult> rerank(List<SearchResult> results, SearchRequest request) {
        log.info("开始重排序，原始结果数={}, maxResults={}", results.size(), request.getMaxResults());

        if (results.isEmpty()) {
            return results;
        }

        List<SearchResult> processed = results;

        // 1. 阈值过滤（只对向量检索分数进行过滤，因为融合后的分数是排名分数）
        if (request.getScoreThreshold() > 0) {
            processed = filterByVectorScore(processed, request.getScoreThreshold());
            log.info("阈值过滤后剩余 {} 条结果", processed.size());
        }

        // 2. 重排序（如果启用）
        if (request.isEnableRerank() && processed.size() > RERANK_TOP_K) {
            processed = rerankWithCrossEncoder(processed.subList(0, Math.min(processed.size(), RERANK_TOP_K)));
            log.info("重排序完成");
        }

        // 3. 多样性优化 (MMR)
        processed = maximizeMarginalRelevance(processed, request.getMaxResults());
        log.info("多样性优化后剩余 {} 条结果", processed.size());

        // 4. 最终排序
        processed = sortByFinalScore(processed);

        // 5. 设置排名
        for (int i = 0; i < processed.size(); i++) {
            processed.get(i).setRank(i + 1);
        }

        // 6. 截取 Top-N
        List<SearchResult> finalResults = processed.stream()
                .limit(request.getMaxResults())
                .collect(Collectors.toList());

        log.info("重排序完成，最终返回 {} 条结果", finalResults.size());
        return finalResults;
    }

    /**
     * 阈值过滤（基于向量分数）
     */
    private List<SearchResult> filterByVectorScore(List<SearchResult> results, double threshold) {
        return results.stream()
                .filter(r -> r.getVectorScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 阈值过滤（基于最终分数，用于融合后的结果）
     */
    private List<SearchResult> filterByThreshold(List<SearchResult> results, double threshold) {
        return results.stream()
                .filter(r -> r.getFinalScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 使用 Cross-Encoder 重排序
     * 简化实现，实际应调用 BGE-reranker 等模型
     */
    private List<SearchResult> rerankWithCrossEncoder(List<SearchResult> results) {
        log.info("使用 Cross-Encoder 重排序 {} 条结果", results.size());

        // TODO: 集成 BGE-reranker 或类似模型
        // 伪代码:
        // List<Double> scores = crossEncoderModel.predict(query, candidates);
        // for (int i = 0; i < results.size(); i++) {
        //     results.get(i).setRerankScore(scores.get(i));
        //     results.get(i).setFinalScore(scores.get(i));
        // }

        // 简化实现：基于内容长度和关键词匹配度进行简单重排序
        for (SearchResult result : results) {
            double rerankScore = calculateSimpleRerankScore(result);
            result.setRerankScore(rerankScore);
            result.setFinalScore(rerankScore);
        }

        // 按重排序分数排序
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getRerankScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 简单重排序分数计算（临时实现）
     */
    private double calculateSimpleRerankScore(SearchResult result) {
        double baseScore = result.getFinalScore();

        // 内容长度因子（适中的长度更好）
        int contentLength = result.getContent() != null ? result.getContent().length() : 0;
        double lengthFactor = 1.0;
        if (contentLength > 50 && contentLength < 500) {
            lengthFactor = 1.1;
        } else if (contentLength < 20 || contentLength > 1000) {
            lengthFactor = 0.9;
        }

        // 双路召回加分
        double sourceBonus = "both".equals(result.getSource()) ? 0.1 : 0;

        return baseScore * lengthFactor + sourceBonus;
    }

    /**
     * MMR (Maximal Marginal Relevance) 多样性优化
     * 避免返回过于相似的内容
     */
    private List<SearchResult> maximizeMarginalRelevance(List<SearchResult> results, int maxResults) {
        if (results.size() <= maxResults) {
            return results;
        }

        List<SearchResult> selected = new ArrayList<>();
        Set<Integer> remaining = new HashSet<>();

        // 初始化：选择分数最高的
        for (int i = 0; i < results.size(); i++) {
            remaining.add(i);
        }

        // 选择第一个（分数最高）
        int firstIndex = 0;
        selected.add(results.get(firstIndex));
        remaining.remove(firstIndex);

        // 贪心选择剩余结果
        while (selected.size() < maxResults && !remaining.isEmpty()) {
            int bestIndex = -1;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int idx : remaining) {
                SearchResult candidate = results.get(idx);

                // 计算 MMR 分数
                double relevanceScore = candidate.getFinalScore();
                double similarityScore = calculateMaxSimilarity(candidate, selected);

                double mmrScore = MMR_LAMBDA * relevanceScore - (1 - MMR_LAMBDA) * similarityScore;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestIndex = idx;
                }
            }

            if (bestIndex >= 0) {
                selected.add(results.get(bestIndex));
                remaining.remove(bestIndex);
            }
        }

        return selected;
    }

    /**
     * 计算与已选结果的最大相似度
     */
    private double calculateMaxSimilarity(SearchResult candidate, List<SearchResult> selected) {
        double maxSim = 0.0;

        for (SearchResult selectedResult : selected) {
            double sim = calculateContentSimilarity(candidate, selectedResult);
            maxSim = Math.max(maxSim, sim);
        }

        return maxSim;
    }

    /**
     * 计算内容相似度（简化版 Jaccard 相似度）
     */
    private double calculateContentSimilarity(SearchResult result1, SearchResult result2) {
        if (result1.getContent() == null || result2.getContent() == null) {
            return 0.0;
        }

        Set<String> set1 = tokenize(result1.getContent());
        Set<String> set2 = tokenize(result2.getContent());

        // Jaccard 相似度
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 简单分词
     */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) return tokens;

        // 按字符分词（简化实现）
        for (int i = 0; i < text.length() - 1; i++) {
            tokens.add(text.substring(i, Math.min(i + 2, text.length())));
        }

        return tokens;
    }

    /**
     * 按最终分数排序
     */
    private List<SearchResult> sortByFinalScore(List<SearchResult> results) {
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getFinalScore).reversed())
                .collect(Collectors.toList());
    }
}
