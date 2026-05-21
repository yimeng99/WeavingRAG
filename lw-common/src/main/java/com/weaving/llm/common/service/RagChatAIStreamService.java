package com.weaving.llm.common.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * RAG 流式对话 AI Service - 使用 TokenStream Flex API
 * 通过 LangChain4j AiServices 自动实现
 */
public interface RagChatAIStreamService {

    /**
     * RAG 流式对话 - 返回 TokenStream 供链式处理
     * @param question 用户问题
     * @return TokenStream 流式响应对象
     */
    @SystemMessage("""
        你是一个基于知识库内容的智能问答助手。
        请根据检索到的相关信息来回答用户的问题。
        要求：
        1. 优先基于检索到的信息回答问题
        2. 如果检索信息与问题不相关，可以说明没有相关信息
        3. 回答要准确、简洁、有条理
        4. 不要编造检索结果中没有的信息
        5. 如果信息不足，可以补充自己的知识，但要说明
        """)
    TokenStream chat(@UserMessage String question);
}
