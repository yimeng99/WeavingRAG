package com.weaving.llm.rag.service.impl;

import com.weaving.llm.rag.service.TextChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TextChunkingServiceImpl implements TextChunkingService {

    @Override
    public List<String> chunkByChar(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        
        int length = content.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(content.substring(start, end));
            if (end == length) break;
            start = end - overlap;
        }
        return chunks;
    }

    @Override
    public List<String> chunkByParagraph(String content, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        
        String[] paragraphs = Pattern.compile("\\n\\s*\\n").split(content);
        StringBuilder currentChunk = new StringBuilder();
        
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) continue;
            
            if (currentChunk.length() + para.length() + 1 <= maxChars) {
                if (currentChunk.length() > 0) currentChunk.append("\n\n");
                currentChunk.append(para.trim());
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
                currentChunk = new StringBuilder(para.trim());
                while (currentChunk.length() > maxChars) {
                    chunks.add(currentChunk.substring(0, maxChars));
                    currentChunk = new StringBuilder(currentChunk.substring(maxChars));
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    @Override
    public List<String> chunkBySentence(String content, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        
        String[] sentences = Pattern.compile("[。！？.!?]+").split(content);
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) continue;
            sentence = sentence.trim();
            if (!sentence.endsWith("。") && !sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?")) {
                sentence = sentence + "。";
            }
            
            if (currentChunk.length() + sentence.length() <= maxChars) {
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
                currentChunk = new StringBuilder(sentence);
                while (currentChunk.length() > maxChars) {
                    chunks.add(currentChunk.substring(0, maxChars));
                    currentChunk = new StringBuilder(currentChunk.substring(maxChars));
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    @Override
    public List<String> chunkBySeparator(String content, String separators, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        
        String[] sepArray = separators.split(",");
        String regex = String.join("|", Arrays.stream(sepArray).map(Pattern::quote).toArray(String[]::new));
        String[] parts = Pattern.compile(regex).split(content);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String part : parts) {
            if (part.trim().isEmpty()) continue;
            
            if (currentChunk.length() + part.length() + 1 <= maxChars) {
                if (currentChunk.length() > 0) currentChunk.append(" ");
                currentChunk.append(part.trim());
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
                currentChunk = new StringBuilder(part.trim());
                while (currentChunk.length() > maxChars) {
                    chunks.add(currentChunk.substring(0, maxChars));
                    currentChunk = new StringBuilder(currentChunk.substring(maxChars));
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    @Override
    public List<String> chunkByLine(String content, int maxLines) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        
        String[] lines = content.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        int lineCount = 0;
        
        for (String line : lines) {
            if (currentChunk.length() > 0) {
                currentChunk.append("\n");
            }
            currentChunk.append(line);
            lineCount++;
            
            if (lineCount >= maxLines) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
                lineCount = 0;
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }

    @Override
    public List<Map<String, Object>> chunkWithMeta(String content, String strategy, int chunkSize, int overlap) {
        List<String> chunks;
        
        switch (strategy) {
            case "paragraph":
                chunks = chunkByParagraph(content, chunkSize);
                break;
            case "sentence":
                chunks = chunkBySentence(content, chunkSize);
                break;
            case "line":
                chunks = chunkByLine(content, chunkSize);
                break;
            case "separator":
                chunks = chunkBySeparator(content, "\n|,|;|。", chunkSize);
                break;
            default:
                chunks = chunkByChar(content, chunkSize, overlap);
        }
        
        return buildChunkResult(chunks);
    }

    @Override
    public List<Map<String, Object>> chunkCustom(String content, String type, Map<String, Object> params) {
        List<String> chunks;
        
        switch (type) {
            case "paragraph":
                int maxCharsPara = params.get("maxChars") != null ? ((Number) params.get("maxChars")).intValue() : 1000;
                chunks = chunkByParagraph(content, maxCharsPara);
                break;
            case "sentence":
                int maxCharsSent = params.get("maxChars") != null ? ((Number) params.get("maxChars")).intValue() : 500;
                chunks = chunkBySentence(content, maxCharsSent);
                break;
            case "line":
                int maxLines = params.get("maxLines") != null ? ((Number) params.get("maxLines")).intValue() : 10;
                chunks = chunkByLine(content, maxLines);
                break;
            case "separator":
                String separators = (String) params.getOrDefault("separators", "\n|,|;");
                int maxCharsSep = params.get("maxChars") != null ? ((Number) params.get("maxChars")).intValue() : 500;
                chunks = chunkBySeparator(content, separators, maxCharsSep);
                break;
            case "custom":
                int chunkSizeCustom = params.get("chunkSize") != null ? ((Number) params.get("chunkSize")).intValue() : 500;
                int overlapCustom = params.get("overlap") != null ? ((Number) params.get("overlap")).intValue() : 50;
                chunks = chunkByChar(content, chunkSizeCustom, overlapCustom);
                break;
            default:
                int chunkSizeDefault = params.get("chunkSize") != null ? ((Number) params.get("chunkSize")).intValue() : 500;
                int overlapDefault = params.get("overlap") != null ? ((Number) params.get("overlap")).intValue() : 50;
                chunks = chunkByChar(content, chunkSizeDefault, overlapDefault);
        }
        
        return buildChunkResult(chunks);
    }

    private List<Map<String, Object>> buildChunkResult(List<String> chunks) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> chunk = new HashMap<>();
            chunk.put("index", i);
            chunk.put("content", chunks.get(i));
            chunk.put("length", chunks.get(i).length());
            result.add(chunk);
        }
        return result;
    }

    @Override
    public Map<String, Object> getChunkingStrategies() {
        Map<String, Object> strategies = new LinkedHashMap<>();
        strategies.putAll(getBuiltInStrategies());
        return strategies;
    }

    @Override
    public Map<String, Object> getBuiltInStrategies() {
        Map<String, Object> strategies = new LinkedHashMap<>();
        
        Map<String, Object> charStrategy = new HashMap<>();
        charStrategy.put("name", "字符分片");
        charStrategy.put("description", "按固定字符数切分，适合代码或结构化文本");
        charStrategy.put("params", Arrays.asList("chunkSize", "overlap"));
        charStrategy.put("defaultChunkSize", 500);
        charStrategy.put("defaultOverlap", 50);
        charStrategy.put("type", "char");
        strategies.put("char", charStrategy);
        
        Map<String, Object> paragraphStrategy = new HashMap<>();
        paragraphStrategy.put("name", "段落分片");
        paragraphStrategy.put("description", "按段落切分，保留语义完整性");
        paragraphStrategy.put("params", Arrays.asList("maxChars"));
        paragraphStrategy.put("defaultMaxChars", 1000);
        paragraphStrategy.put("type", "paragraph");
        strategies.put("paragraph", paragraphStrategy);
        
        Map<String, Object> sentenceStrategy = new HashMap<>();
        sentenceStrategy.put("name", "句子分片");
        sentenceStrategy.put("description", "按句子切分，适合对话或问答");
        sentenceStrategy.put("params", Arrays.asList("maxChars"));
        sentenceStrategy.put("defaultMaxChars", 500);
        sentenceStrategy.put("type", "sentence");
        strategies.put("sentence", sentenceStrategy);
        
        Map<String, Object> lineStrategy = new HashMap<>();
        lineStrategy.put("name", "行分片");
        lineStrategy.put("description", "按固定行数切分，适合日志或CSV");
        lineStrategy.put("params", Arrays.asList("maxLines"));
        lineStrategy.put("defaultMaxLines", 10);
        lineStrategy.put("type", "line");
        strategies.put("line", lineStrategy);
        
        Map<String, Object> separatorStrategy = new HashMap<>();
        separatorStrategy.put("name", "分隔符分片");
        separatorStrategy.put("description", "按指定分隔符切分，如换行、逗号、分号");
        separatorStrategy.put("params", Arrays.asList("separators", "maxChars"));
        separatorStrategy.put("defaultSeparators", "\\n|,|;");
        separatorStrategy.put("defaultMaxChars", 500);
        separatorStrategy.put("type", "separator");
        strategies.put("separator", separatorStrategy);
        
        return strategies;
    }
}
