package com.weaving.llm.app;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author 依梦
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {"com.weaving.llm"})
@MapperScan({"com.weaving.llm.common.mapper", "com.weaving.llm.common.ai.mapper", "com.weaving.llm.rag.mapper", "com.weaving.llm.app.mapper"})
public class LlmWeavingApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(LlmWeavingApplication.class, args);
        
        String port = context.getEnvironment().getProperty("server.port", "8080");
        String localHostUrl = "http://localhost:" + port;
        String networkUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port;
        
        log.info("\n" +
                "\n=====================================================" +
                "\n  LLM Weaving 项目启动成功！" +
                "\n  本地访问：{}" +
                "\n  网络访问：{}" +
                "\n  Swagger: {}/swagger-ui.html" +
                "\n=====================================================", 
                localHostUrl, networkUrl, localHostUrl);
    }

}
