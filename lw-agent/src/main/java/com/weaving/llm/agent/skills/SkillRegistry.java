package com.weaving.llm.agent.skills;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 技能注册中心
 * 自动发现并注册所有技能类
 */
@Slf4j
@Component
public class SkillRegistry {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 技能注册表
    private final Map<String, Skill> skillRegistry = new HashMap<>();
    
    // 按分类存储的技能
    private final Map<String, List<Skill>> skillsByCategory = new HashMap<>();
    
    /**
     * 初始化时自动注册所有技能
     */
    @PostConstruct
    public void init() {
        log.info("开始注册 Agent 技能...");
        
        // 从 Spring 容器中获取所有实现 Skill 接口的 Bean
        Map<String, Skill> skillBeans = applicationContext.getBeansOfType(Skill.class);
        
        for (Skill skill : skillBeans.values()) {
            registerSkill(skill);
        }
        
        log.info("技能注册完成，共注册 {} 个技能", skillRegistry.size());
        log.info("技能分类：{}", skillsByCategory.keySet());
    }
    
    /**
     * 注册单个技能
     * @param skill 技能实例
     */
    public void registerSkill(Skill skill) {
        String skillName = skill.getName();
        
        if (skillRegistry.containsKey(skillName)) {
            log.warn("技能已存在，跳过注册：{} (类名：{})", skillName, skill.getClass().getSimpleName());
            return;
        }
        
        skillRegistry.put(skillName, skill);
        
        // 按分类存储
        String category = skill.getCategory();
        skillsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(skill);
        
        log.info("注册技能：{} - {} ({})", 
            skillName, 
            skill.getDescription(),
            category);
    }
    
    /**
     * 获取所有已注册的技能
     * @return 技能列表
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(skillRegistry.values());
    }
    
    /**
     * 根据名称获取技能
     * @param name 技能名称
     * @return 技能实例，如果不存在则返回 null
     */
    public Skill getSkill(String name) {
        return skillRegistry.get(name);
    }
    
    /**
     * 根据分类获取技能
     * @param category 分类名称
     * @return 该分类下的技能列表
     */
    public List<Skill> getSkillsByCategory(String category) {
        return skillsByCategory.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * 获取所有分类
     * @return 分类名称集合
     */
    public Set<String> getCategories() {
        return skillsByCategory.keySet();
    }
    
    /**
     * 获取技能数量
     * @return 已注册的技能数量
     */
    public int getSkillCount() {
        return skillRegistry.size();
    }
    
    /**
     * 检查是否包含指定技能
     * @param skillName 技能名称
     * @return 如果存在返回 true
     */
    public boolean hasSkill(String skillName) {
        return skillRegistry.containsKey(skillName);
    }
    
    /**
     * 注销技能
     * @param skillName 技能名称
     */
    public void unregisterSkill(String skillName) {
        Skill skill = skillRegistry.remove(skillName);
        if (skill != null) {
            String category = skill.getCategory();
            List<Skill> categorySkills = skillsByCategory.get(category);
            if (categorySkills != null) {
                categorySkills.remove(skill);
            }
            log.info("已注销技能：{}", skillName);
        }
    }
    
    /**
     * 动态注册新技能 (支持运行时添加)
     * @param skill 技能实例
     * @return 是否注册成功
     */
    public boolean addSkill(Skill skill) {
        if (skill == null) {
            log.warn("不能注册 null 技能");
            return false;
        }
        
        String skillName = skill.getName();
        if (skillRegistry.containsKey(skillName)) {
            log.warn("技能已存在，覆盖注册：{} (类名：{})", skillName, skill.getClass().getSimpleName());
        }
        
        registerSkill(skill);
        
        // 触发监听器通知 (如果有)
        notifySkillAdded(skill);
        
        return true;
    }
    
    /**
     * 批量注册技能
     * @param skills 技能列表
     * @return 成功注册的数量
     */
    public int addSkills(List<Skill> skills) {
        int count = 0;
        for (Skill skill : skills) {
            if (addSkill(skill)) {
                count++;
            }
        }
        return count;
    }
    
    // 技能变更监听器
    private final List<java.util.function.Consumer<Skill>> skillAddedListeners = new ArrayList<>();
    
    /**
     * 添加技能注册监听器
     * @param listener 监听器回调
     */
    public void onSkillAdded(java.util.function.Consumer<Skill> listener) {
        skillAddedListeners.add(listener);
    }
    
    /**
     * 通知技能已添加
     * @param skill 新注册的技能
     */
    private void notifySkillAdded(Skill skill) {
        for (java.util.function.Consumer<Skill> listener : skillAddedListeners) {
            try {
                listener.accept(skill);
            } catch (Exception e) {
                log.error("技能添加监听器执行失败", e);
            }
        }
    }
}
