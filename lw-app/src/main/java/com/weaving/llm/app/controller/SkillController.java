package com.weaving.llm.app.controller;

import com.weaving.llm.agent.skills.Skill;
import com.weaving.llm.agent.service.SkillService;
import com.weaving.llm.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 技能管理控制器
 * 提供技能查询、推荐等功能的 RESTful API
 */
@Slf4j
@Tag(name = "技能管理", description = "技能查询、推荐等管理接口")
@RestController
@RequestMapping("/api/skills")
public class SkillController {
    
    @Autowired
    private SkillService skillService;
    
    /**
     * 获取所有技能列表
     */
    @GetMapping
    @Operation(summary = "获取所有技能", description = "查询系统中所有可用的技能列表")
    public R<Map<String, Object>> getAllSkills() {
        List<Skill> skills = skillService.getAllSkills();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", skills.size());
        result.put("skills", skills.stream()
                .map(skill -> Map.of(
                        "name", skill.getName(),
                        "description", skill.getDescription(),
                        "category", skill.getCategory(),
                        "triggerCondition", skill.getTriggerCondition(),
                        "priority", skill.getPriority()
                ))
                .toList());
        
        return R.ok(result);
    }
    
    /**
     * 根据分类获取技能
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "按分类查询技能", description = "根据指定分类查询技能列表")
    public R<Map<String, Object>> getSkillsByCategory(
            @Parameter(description = "技能分类", required = true) @PathVariable String category) {
        List<Skill> skills = skillService.getSkillsByCategory(category);
        
        Map<String, Object> result = new HashMap<>();
        result.put("category", category);
        result.put("total", skills.size());
        result.put("skills", skills.stream()
                .map(skill -> Map.of(
                        "name", skill.getName(),
                        "description", skill.getDescription(),
                        "priority", skill.getPriority()
                ))
                .toList());
        
        return R.ok(result);
    }
    
    /**
     * 获取所有技能分类
     */
    @GetMapping("/categories")
    @Operation(summary = "获取技能分类", description = "查询所有可用的技能分类")
    public R<Set<String>> getCategories() {
        return R.ok(skillService.getSkillCategories());
    }
    
    /**
     * 获取技能详情
     */
    @GetMapping("/{skillName}")
    @Operation(summary = "获取技能详情", description = "根据技能名称查询详细信息")
    public R<Map<String, Object>> getSkillDetail(
            @Parameter(description = "技能名称", required = true) @PathVariable String skillName) {
        Skill skill = skillService.getSkill(skillName);
        
        if (skill == null) {
            return R.fail("技能不存在：" + skillName);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", skill.getName());
        result.put("description", skill.getDescription());
        result.put("category", skill.getCategory());
        result.put("triggerCondition", skill.getTriggerCondition());
        result.put("priority", skill.getPriority());
        
        return R.ok(result);
    }
    
    /**
     * 根据场景推荐技能
     */
    @PostMapping("/recommend")
    @Operation(summary = "推荐技能", description = "根据场景描述智能推荐相关技能")
    public R<Map<String, Object>> recommendSkills(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "场景描述") @RequestBody Map<String, String> request) {
        String scenario = request.get("scenario");
        
        if (scenario == null || scenario.isEmpty()) {
            return R.fail("请提供场景描述");
        }
        
        List<Skill> recommendedSkills = skillService.recommendSkillsForScenario(scenario);
        
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", scenario);
        result.put("recommendedSkills", recommendedSkills.stream()
                .map(skill -> Map.of(
                        "name", skill.getName(),
                        "description", skill.getDescription(),
                        "category", skill.getCategory(),
                        "priority", skill.getPriority(),
                        "reason", "根据您的场景需求自动推荐"
                ))
                .toList());
        
        return R.ok(result);
    }
    
    /**
     * 获取技能统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取技能的统计信息")
    public R<Map<String, Object>> getStatistics() {
        return R.ok(skillService.getSkillStatistics());
    }
    
    /**
     * 获取技能使用指南
     */
    @GetMapping("/{skillName}/guide")
    @Operation(summary = "获取技能使用指南", description = "查询指定技能的使用指南")
    public R<Map<String, Object>> getSkillGuide(
            @Parameter(description = "技能名称", required = true) @PathVariable String skillName) {
        String guide = skillService.getSkillUsageGuide(skillName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("skillName", skillName);
        result.put("guide", guide);
        
        return R.ok(result);
    }
}