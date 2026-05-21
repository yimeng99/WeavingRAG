package com.weaving.llm.agent.tools;

import dev.langchain4j.agent.tool.Tool;

/**
 * Agent 工具基础接口
 * 所有工具类都应该实现此接口
 */
public interface AgentTool {
    
    /**
     * 工具名称
     * @return 工具的唯一标识名称
     */
    String getName();
    
    /**
     * 工具描述
     * @return 工具的功能描述
     */
    String getDescription();
    
    /**
     * 工具分类
     * @return 工具所属类别 (如：SEARCH, CALCULATION, FILE, NETWORK 等)
     */
    default String getCategory() {
        return "GENERAL";
    }
}
