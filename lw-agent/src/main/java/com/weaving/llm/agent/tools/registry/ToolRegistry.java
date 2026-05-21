package com.weaving.llm.agent.tools.registry;

import com.weaving.llm.agent.tools.AgentTool;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 工具注册中心
 * 自动发现并注册所有工具类
 */
@Slf4j
@Component
public class ToolRegistry {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // 工具注册表
    private final Map<String, AgentTool> toolRegistry = new HashMap<>();
    
    // 按分类存储的工具
    private final Map<String, List<AgentTool>> toolsByCategory = new HashMap<>();
    
    /**
     * 初始化时自动注册所有工具
     */
    @PostConstruct
    public void init() {
        log.info(" 开始注册 Agent 工具...");
        
        // 从 Spring 容器中获取所有实现 AgentTool 接口的 Bean
        Map<String, AgentTool> toolBeans = applicationContext.getBeansOfType(AgentTool.class);
        
        for (AgentTool tool : toolBeans.values()) {
            registerTool(tool);
        }
        
        log.info("工具注册完成，共注册 {} 个工具", toolRegistry.size());
        log.info("工具分类：{}", toolsByCategory.keySet());
    }
    
    /**
     * 注册单个工具
     * @param tool 工具实例
     */
    public void registerTool(AgentTool tool) {
        String toolName = tool.getName();
        
        if (toolRegistry.containsKey(toolName)) {
            log.warn("工具已存在，跳过注册：{} (类名：{})", toolName, tool.getClass().getSimpleName());
            return;
        }
        
        toolRegistry.put(toolName, tool);
        
        // 按分类存储
        String category = tool.getCategory();
        toolsByCategory.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(tool);
        
        log.info("注册工具：{} - {} ({})", 
            toolName, 
            tool.getDescription(),
            category);
    }
    
    /**
     * 获取所有已注册的工具
     * @return 工具列表
     */
    public List<AgentTool> getAllTools() {
        return new java.util.ArrayList<>(toolRegistry.values());
    }
    
    /**
     * 根据名称获取工具
     * @param name 工具名称
     * @return 工具实例，如果不存在则返回 null
     */
    public AgentTool getTool(String name) {
        return toolRegistry.get(name);
    }
    
    /**
     * 根据分类获取工具
     * @param category 分类名称
     * @return 该分类下的工具列表
     */
    public List<AgentTool> getToolsByCategory(String category) {
        return toolsByCategory.getOrDefault(category, new java.util.ArrayList<>());
    }
    
    /**
     * 获取所有分类
     * @return 分类名称集合
     */
    public Set<String> getCategories() {
        return toolsByCategory.keySet();
    }
    
    /**
     * 获取工具数量
     * @return 已注册的工具数量
     */
    public int getToolCount() {
        return toolRegistry.size();
    }
    
    /**
     * 检查是否包含指定工具
     * @param toolName 工具名称
     * @return 如果存在返回 true
     */
    public boolean hasTool(String toolName) {
        return toolRegistry.containsKey(toolName);
    }
    
    /**
     * 注销工具
     * @param toolName 工具名称
     */
    public void unregisterTool(String toolName) {
        AgentTool tool = toolRegistry.remove(toolName);
        if (tool != null) {
            String category = tool.getCategory();
            List<AgentTool> categoryTools = toolsByCategory.get(category);
            if (categoryTools != null) {
                categoryTools.remove(tool);
            }
            log.info("已注销工具：{}", toolName);
        }
    }
    
    /**
     * 动态注册新工具 (支持运行时添加)
     * @param tool 工具实例
     * @return 是否注册成功
     */
    public boolean addTool(AgentTool tool) {
        if (tool == null) {
            log.warn("不能注册 null 工具");
            return false;
        }
        
        String toolName = tool.getName();
        if (toolRegistry.containsKey(toolName)) {
            log.warn("工具已存在，覆盖注册：{} (类名：{})", toolName, tool.getClass().getSimpleName());
        }
        
        registerTool(tool);
        
        // 触发监听器通知 (如果有)
        notifyToolAdded(tool);
        
        return true;
    }
    
    /**
     * 批量注册工具
     * @param tools 工具列表
     * @return 成功注册的数量
     */
    public int addTools(List<AgentTool> tools) {
        int count = 0;
        for (AgentTool tool : tools) {
            if (addTool(tool)) {
                count++;
            }
        }
        return count;
    }
    
    // 工具变更监听器
    private final List<java.util.function.Consumer<AgentTool>> toolAddedListeners = new java.util.ArrayList<>();
    
    /**
     * 添加工具注册监听器
     * @param listener 监听器回调
     */
    public void onToolAdded(java.util.function.Consumer<AgentTool> listener) {
        toolAddedListeners.add(listener);
    }
    
    /**
     * 通知工具已添加
     * @param tool 新注册的工具
     */
    private void notifyToolAdded(AgentTool tool) {
        for (java.util.function.Consumer<AgentTool> listener : toolAddedListeners) {
            try {
                listener.accept(tool);
            } catch (Exception e) {
                log.error("工具添加监听器执行失败", e);
            }
        }
    }
}
