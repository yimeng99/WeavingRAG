package com.weaving.llm.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 计算工具
 * 提供数学计算和表达式求值功能
 */
@Slf4j
@Component
public class CalculationTool implements AgentTool {
    
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "执行数学计算，支持加减乘除、幂运算、开方等";
    }
    
    @Override
    public String getCategory() {
        return "CALCULATION";
    }
    
    @Tool("执行数学计算，支持加减乘除、幂运算、开方等")
    public double calculate(String expression) {
        log.info("计算表达式：{}", expression);
        try {
            return evaluateExpression(expression);
        } catch (Exception e) {
            log.error("计算失败", e);
            return 0.0;
        }
    }
    
    /**
     * 计算表达式
     * @param expression 数学表达式
     * @return 计算结果
     */
    private double evaluateExpression(String expression) throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("不支持的脚本引擎");
        }
        Object result = engine.eval(expression);
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }
        return Double.parseDouble(result.toString());
    }
    
    @Tool("计算百分比")
    public String calculatePercentage(double value, double total) {
        if (total == 0) {
            return "错误：除数不能为零";
        }
        double percentage = (value / total) * 100;
        return String.format("百分比计算：%.2f 占 %.2f 的 %.2f%%", value, total, percentage);
    }
    
    @Tool("计算平方根")
    public String sqrt(double value) {
        if (value < 0) {
            return "错误：不能计算负数的平方根";
        }
        double result = Math.sqrt(value);
        return String.format("√%.4f = %.4f", value, result);
    }
    
    @Tool("计算幂")
    public String power(double base, double exponent) {
        double result = Math.pow(base, exponent);
        return String.format("%.2f ^ %.2f = %.4f", base, exponent, result);
    }
}
