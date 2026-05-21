package com.weaving.llm.rag.service;

import java.util.List;
import java.util.Map;

public interface TextChunkingService {
    
    List<String> chunkByChar(String content, int chunkSize, int overlap);
    
    List<String> chunkByParagraph(String content, int maxChars);
    
    List<String> chunkBySentence(String content, int maxChars);
    
    List<String> chunkBySeparator(String content, String separators, int maxChars);
    
    List<String> chunkByLine(String content, int maxLines);
    
    List<Map<String, Object>> chunkWithMeta(String content, String strategy, int chunkSize, int overlap);
    
    List<Map<String, Object>> chunkCustom(String content, String type, Map<String, Object> params);
    
    Map<String, Object> getChunkingStrategies();
    
    Map<String, Object> getBuiltInStrategies();
}
