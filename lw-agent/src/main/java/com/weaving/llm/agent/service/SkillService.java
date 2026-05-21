package com.weaving.llm.agent.service;

import com.weaving.llm.agent.skills.Skill;
import com.weaving.llm.agent.skills.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能服�? * 负责管理技能的注册、查询和应用
 */
@Slf4j
@Service
public class SkillService {
    
    @Autowired
    private SkillRegistry skillRegistry;
    
    /**
     * 获取所有技�?     * @return 技能列�?     */
    public List<Skill> getAllSkills() {
        return skillRegistry.getAllSkills();
    }
    
    /**
     * 根据名称获取技�?     * @param skillName 技能名�?     * @return 技能实�?     */
    public Skill getSkill(String skillName) {
        return skillRegistry.getSkill(skillName);
    }
    
    /**
     * 根据分类获取技�?     * @param category 分类名称
     * @return 该分类下的技能列�?     */
    public List<Skill> getSkillsByCategory(String category) {
        return skillRegistry.getSkillsByCategory(category);
    }
    
    /**
     * 获取所有技能分�?     * @return 分类列表
     */
    public Set<String> getSkillCategories() {
        return skillRegistry.getCategories();
    }
    
    /**
     * 根据场景推荐技�?     * @param scenario 使用场景描述
     * @return 推荐的技能列�?(按优先级排序)
     */
    public List<Skill> recommendSkillsForScenario(String scenario) {
        if (scenario == null || scenario.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Skill> allSkills = skillRegistry.getAllSkills();
        
        // 根据场景关键词匹配技�?
        return allSkills.stream()
                .filter(skill -> isSkillRelevant(skill, scenario))
                .sorted(Comparator.comparingInt(Skill::getPriority).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * 判断技能是否与场景相关
     * @param skill 技�?     * @param scenario 场景描述
     * @return 是否相关
     */
    private boolean isSkillRelevant(Skill skill, String scenario) {
        String lowerScenario = scenario.toLowerCase();
        String description = skill.getDescription().toLowerCase();
        String triggerCondition = skill.getTriggerCondition().toLowerCase();
        
        // 检查场景描述中是否包含技能相关的关键�?
        return lowerScenario.contains(description.split(",")[0]) ||
               lowerScenario.contains(triggerCondition.split(",")[0]) ||
               matchKeywords(lowerScenario, skill);
    }
    
    /**
     * 关键词匹�?     * @param scenario 场景描述
     * @param skill 技�?     * @return 是否匹配
     */
    private boolean matchKeywords(String scenario, Skill skill) {
        // 根据技能类型进行关键词匹配
        switch (skill.getCategory()) {
            case "ANALYSIS":
                return scenario.contains("分析") || scenario.contains("理解") || 
                       scenario.contains("查看") || scenario.contains("代码");
            case "CREATION":
                return scenario.contains("生成") || scenario.contains("创建") || 
                       scenario.contains("编写") || scenario.contains("实现");
            case "DIAGNOSIS":
                return scenario.contains("错误") || scenario.contains("异常") || 
                       scenario.contains("问题") || scenario.contains("故障") ||
                       scenario.contains("排查") || scenario.contains("诊断");
            default:
                return true;
        }
    }
    
    /**
     * 动态添加新技�?     * @param skill 技能实�?     * @return 是否添加成功
     */
    public boolean addSkill(Skill skill) {
        return skillRegistry.addSkill(skill);
    }
    
    /**
     * 批量添加技�?     * @param skills 技能列�?     * @return 成功添加的数�?     */
    public int addSkills(List<Skill> skills) {
        return skillRegistry.addSkills(skills);
    }
    
    /**
     * 移除技�?     * @param skillName 技能名�?     */
    public void removeSkill(String skillName) {
        skillRegistry.unregisterSkill(skillName);
    }
    
    /**
     * 获取技能统计信�?     * @return 技能信�?Map
     */
    public Map<String, Object> getSkillStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", skillRegistry.getSkillCount());
        stats.put("categories", skillRegistry.getCategories());
        
        // 按分类统�?
        Map<String, Integer> categoryStats = new HashMap<>();
        for (String category : skillRegistry.getCategories()) {
            categoryStats.put(category, skillRegistry.getSkillsByCategory(category).size());
        }
        stats.put("categoryStats", categoryStats);
        
        return stats;
    }
    
    /**
     * 获取技能的详细使用指南
     * @param skillName 技能名�?     * @return 使用指南
     */
    public String getSkillUsageGuide(String skillName) {
        Skill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            return "技能不存在�? + skillName";
        }
        
        StringBuilder guide = new StringBuilder();
//        guide.append("技能：").append(skill.getName()).append("\n");
//        guide.append("描述�?).append(skill.getDescription()).append("\n");
//        guide.append("分类�?).append(skill.getCategory()).append("\n");
//        guide.append("触发条件�?).append(skill.getTriggerCondition()).append("\n");
//        guide.append("优先级：").append(skill.getPriority()).append("\n");
//
        return guide.toString();
    }
}
