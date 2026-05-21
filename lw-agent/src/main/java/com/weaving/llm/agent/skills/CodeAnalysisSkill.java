package com.weaving.llm.agent.skills;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 代码分析技能
 * 用于分析和理解代码结构
 */
@Component
public class CodeAnalysisSkill implements Skill {
    
    @Override
    public String getName() {
        return "code_analysis";
    }
    
    @Override
    public String getDescription() {
        return "分析代码结构、识别设计模式、检测代码异味";
    }
    
    @Override
    public String getCategory() {
        return "ANALYSIS";
    }
    
    @Override
    public String getTriggerCondition() {
        return "当用户需要理解、分析或优化代码时触发";
    }
    
    @Override
    public int getPriority() {
        return 3;
    }
    
    /**
     * 分析代码复杂度
     * @param code 代码内容
     * @return 复杂度分析报告
     */
    @Tool("分析代码的复杂度")
    public String analyzeComplexity(String code) {
        if (code == null || code.isEmpty()) {
            return "无法分析空代码";
        }
        
        int lines = code.split("\n").length;
        int methods = countOccurrences(code, "\\b(void|int|String|boolean|double|float|long|short|byte|char|Object|List|Map|Set|Array)\\s+\\w+\\s*\\(");
        int loops = countOccurrences(code, "\\b(for|while|do)\\s*[({]") + countOccurrences(code, "\\.(forEach|map|filter|reduce)\\s*\\(");
        
        String complexity = "低";
        if (lines > 100 || methods > 10 || loops > 5) {
            complexity = "中";
        }
        if (lines > 300 || methods > 20 || loops > 10) {
            complexity = "高";
        }
        
        return String.format("代码分析报告:\n- 行数：%d\n- 方法数：%d\n- 循环数：%d\n- 复杂度：%s", 
            lines, methods, loops, complexity);
    }
    
    /**
     * 识别设计模式
     * @param code 代码内容
     * @return 识别出的设计模式
     */
    @Tool("识别代码中使用的设计模式")
    public String identifyPatterns(String code) {
        if (code == null || code.isEmpty()) {
            return "无法识别空代码的模式";
        }
        
        StringBuilder patterns = new StringBuilder("识别到的设计模式:\n");
        
        if (code.contains("getInstance") && code.contains("private static")) {
            patterns.append("- 单例模式\n");
        }
        if (code.contains("Builder") && code.contains("build()")) {
            patterns.append("- 建造者模式\n");
        }
        if (code.contains("Factory") || code.contains("create")) {
            patterns.append("- 工厂模式\n");
        }
        if (code.contains("@Autowired") || code.contains("@Inject")) {
            patterns.append("- 依赖注入模式\n");
        }
        if (code.contains("interface") && code.contains("implements")) {
            patterns.append("- 策略模式/观察者模式\n");
        }
        
        return patterns.toString();
    }
    
    private int countOccurrences(String text, String regex) {
        return text.length() - text.replaceAll(regex, "").length();
    }
}
