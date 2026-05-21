package com.weaving.llm.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具
 * 提供日期时间相关功能
 */
@Slf4j
@Component
public class DateTimeTool implements AgentTool {
    
    @Override
    public String getName() {
        return "datetime";
    }
    
    @Override
    public String getDescription() {
        return "获取当前的日期和时间";
    }
    
    @Override
    public String getCategory() {
        return "UTILITY";
    }
    
    @Tool("获取当前的日期和时间")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        log.info("获取当前时间：{}", formattedDateTime);
        return "当前时间：" + formattedDateTime;
    }
    
    @Tool("获取当前日期")
    public String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日");
        String formattedDate = now.format(formatter);
        log.info("获取当前日期：{}", formattedDate);
        return "当前日期：" + formattedDate;
    }
    
    @Tool("获取当前时间")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = now.format(formatter);
        log.info("获取当前时间：{}", formattedTime);
        return "当前时间：" + formattedTime;
    }
}
