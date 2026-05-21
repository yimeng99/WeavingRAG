package com.weaving.llm.agent.tools.example;

import com.weaving.llm.agent.tools.AgentTool;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例：天气查询工具
 * 
 * 这是一个演示如何动态添加的 Agent 工具示例
 * 部署上线后，可以通过 API 动态注册此类
 */
@Slf4j
@Component
public class WeatherTool implements AgentTool {
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "查询指定城市的天气情况";
    }
    
    @Override
    public String getCategory() {
        return "SEARCH";
    }
    
    @Tool("查询当前天气")
    public String getCurrentWeather(String city) {
        log.info("查询天气：{}", city);
        
        // TODO: 这里可以集成真实的天气 API
        // 如：和风天气、OpenWeatherMap 等
        
        // 示例返回
        return String.format("当前 %s 的天气：晴，温度 25°C，湿度 60%%，空气质量优", city);
    }
    
    @Tool("查询未来天气预报")
    public String getWeatherForecast(String city, int days) {
        log.info("查询天气预报：{} 天后", days);
        
        // 示例返回
        StringBuilder sb = new StringBuilder();
        sb.append(city).append("未来").append(days).append("天天气预报:\n");
        for (int i = 1; i <= days; i++) {
            sb.append("第").append(i).append("天：晴，").append(20 + i).append("°C\n");
        }
        return sb.toString();
    }
}
