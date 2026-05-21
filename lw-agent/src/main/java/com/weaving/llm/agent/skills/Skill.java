package com.weaving.llm.agent.skills;

/**
 * Agent 技能基础接口
 * 所有技能类都应该实现此接口
 */
public interface Skill {
    
    /**
     * 技能名称
     * @return 技能的唯一标识名称
     */
    String getName();
    
    /**
     * 技能描述
     * @return 技能的功能描述
     */
    String getDescription();
    
    /**
     * 技能分类
     * @return 技能所属类别 (如：ANALYSIS, CREATION, OPTIMIZATION 等)
     */
    default String getCategory() {
        return "GENERAL";
    }
    
    /**
     * 技能触发条件
     * @return 描述何时应该使用该技能
     */
    default String getTriggerCondition() {
        return "根据用户需求自动触发";
    }
    
    /**
     * 技能优先级
     * @return 优先级数值，越大优先级越高
     */
    default int getPriority() {
        return 1;
    }
}
