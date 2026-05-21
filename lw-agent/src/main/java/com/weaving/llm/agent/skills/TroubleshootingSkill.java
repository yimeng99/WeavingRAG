package com.weaving.llm.agent.skills;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 问题排查技能
 * 用于诊断和解决技术问题
 */
@Component
public class TroubleshootingSkill implements Skill {
    
    @Override
    public String getName() {
        return "troubleshooting";
    }
    
    @Override
    public String getDescription() {
        return "诊断系统问题、分析错误日志、提供解决方案";
    }
    
    @Override
    public String getCategory() {
        return "DIAGNOSIS";
    }
    
    @Override
    public String getTriggerCondition() {
        return "当用户遇到错误、异常或系统故障时触发";
    }
    
    @Override
    public int getPriority() {
        return 5; // 高优先级，问题排查通常比较紧急
    }
    
    /**
     * 分析错误信息
     * @param errorMessage 错误信息
     * @return 分析报告和建议
     */
    @Tool("分析错误信息并提供解决方案")
    public String analyzeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "请提供具体的错误信息";
        }
        
        StringBuilder analysis = new StringBuilder("错误分析:\n\n");
        
        // 常见错误类型识别
        if (errorMessage.contains("NullPointerException") || errorMessage.contains("NPE")) {
            analysis.append("【空指针异常】\n");
            analysis.append("可能原因:\n");
            analysis.append("1. 对象未初始化就使用\n");
            analysis.append("2. 数据库查询返回 null\n");
            analysis.append("3. 参数传递为 null\n\n");
            analysis.append("解决方案:\n");
            analysis.append("1. 添加 null 检查\n");
            analysis.append("2. 使用 Optional 包装可能为 null 的值\n");
            analysis.append("3. 检查依赖注入是否正确\n\n");
        }
        
        if (errorMessage.contains("IllegalArgumentException")) {
            analysis.append("【非法参数异常】\n");
            analysis.append("可能原因：方法接收到不合法的参数值\n\n");
            analysis.append("解决方案:\n");
            analysis.append("1. 在方法入口处进行参数校验\n");
            analysis.append("2. 使用@Valid 注解进行验证\n\n");
        }
        
        if (errorMessage.contains("IOException")) {
            analysis.append("【IO 异常】\n");
            analysis.append("可能原因:\n");
            analysis.append("1. 文件不存在\n");
            analysis.append("2. 网络连接失败\n");
            analysis.append("3. 权限不足\n\n");
            analysis.append("解决方案:\n");
            analysis.append("1. 检查文件路径是否正确\n");
            analysis.append("2. 添加重试机制\n");
            analysis.append("3. 使用 try-with-resources 关闭资源\n\n");
        }
        
        if (errorMessage.contains("Timeout") || errorMessage.contains("timeout")) {
            analysis.append("【超时异常】\n");
            analysis.append("可能原因:\n");
            analysis.append("1. 网络延迟过高\n");
            analysis.append("2. 数据库查询过慢\n");
            analysis.append("3. 外部服务响应慢\n\n");
            analysis.append("解决方案:\n");
            analysis.append("1. 增加超时时间\n");
            analysis.append("2. 优化 SQL 查询\n");
            analysis.append("3. 添加缓存层\n\n");
        }
        
        // 通用建议
        analysis.append("\n【通用排查步骤】\n");
        analysis.append("1. 查看完整的堆栈跟踪信息\n");
        analysis.append("2. 定位错误发生的代码位置\n");
        analysis.append("3. 检查相关日志记录\n");
        analysis.append("4. 复现问题并调试\n");
        analysis.append("5. 使用断点或日志分析变量状态\n");
        
        return analysis.toString();
    }
    
    /**
     * 性能问题诊断
     * @param symptoms 性能问题症状描述
     * @return 诊断报告
     */
    @Tool("诊断性能问题")
    public String diagnosePerformance(String symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return "请描述性能问题的具体表现";
        }
        
        StringBuilder report = new StringBuilder("性能诊断报告:\n\n");
        
        if (symptoms.contains("慢") || symptoms.contains("延迟")) {
            report.append("【响应慢】\n");
            report.append("可能瓶颈:\n");
            report.append("1. 数据库查询 - 检查慢查询日志\n");
            report.append("2. 网络 IO - 检查带宽和延迟\n");
            report.append("3. CPU 密集计算 - 考虑异步处理\n");
            report.append("4. 内存不足 - 检查 GC 情况\n\n");
            
            report.append("优化建议:\n");
            report.append("1. 添加索引优化查询\n");
            report.append("2. 使用缓存减少数据库访问\n");
            report.append("3. 批量处理改为异步\n");
            report.append("4. 增加 JVM 堆内存\n\n");
        }
        
        if (symptoms.contains("内存") || symptoms.contains("OOM") || symptoms.contains("OutOfMemory")) {
            report.append("【内存问题】\n");
            report.append("排查步骤:\n");
            report.append("1. 使用 jmap 导出堆转储\n");
            report.append("2. 用 MAT 分析内存泄漏\n");
            report.append("3. 检查是否有大对象未释放\n\n");
            
            report.append("解决方案:\n");
            report.append("1. 优化数据结构\n");
            report.append("2. 及时关闭资源\n");
            report.append("3. 调整 JVM 参数\n\n");
        }
        
        return report.toString();
    }
}
