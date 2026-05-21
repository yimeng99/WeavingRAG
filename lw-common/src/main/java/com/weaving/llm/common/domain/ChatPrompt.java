package com.weaving.llm.common.domain;

import lombok.Data;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: ChatPrompt
 */
@Data
public class ChatPrompt {

    /**
     * 会话ID
     */
    private String chatSessionId;

    /**
     * 客户端流ID
     */
    private String clientStreamId;

    /**
     * 上一个消�?
     */
    private String parentMessageId;

    /**
     * 提示�?
     */
    private String prompt;
    
    /**
     * 选择的模型名�?
     */
    private String modelName;
    
    /**
     * 是否使用知识库增�?
     */
    private boolean useKnowledge = false;
}
