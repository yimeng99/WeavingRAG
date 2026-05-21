package com.weaving.llm.agent.skills;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码生成技能
 * 用于自动生成代码片段
 */
@Component
public class CodeGenerationSkill implements Skill {
    
    @Override
    public String getName() {
        return "code_generation";
    }
    
    @Override
    public String getDescription() {
        return "根据需求生成高质量的 Java 代码，包括类、方法、测试等";
    }
    
    @Override
    public String getCategory() {
        return "CREATION";
    }
    
    @Override
    public String getTriggerCondition() {
        return "当用户需要创建新代码、生成样板代码或实现功能时触发";
    }
    
    @Override
    public int getPriority() {
        return 3;
    }
    
    /**
     * 生成 Java 类模板
     * @param className 类名
     * @param packageName 包名
     * @return 生成的类代码
     */
    @Tool("生成 Java 类模板")
    public String generateClassTemplate(String className, String packageName) {
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(";\n\n");
        code.append("import lombok.Data;\n");
        code.append("import lombok.NoArgsConstructor;\n\n");
        code.append("@Data\n");
        code.append("@NoArgsConstructor\n");
        code.append("public class ").append(className).append(" {\n");
        code.append("    \n");
        code.append("    // TODO: 添加字段\n");
        code.append("    private String id;\n");
        code.append("    private String name;\n");
        code.append("    \n");
        code.append("    // TODO: 添加构造方法\n");
        code.append("    \n");
        code.append("    // TODO: 添加业务方法\n");
        code.append("}\n");
        return code.toString();
    }
    
    /**
     * 生成 REST Controller 模板
     * @param entityName 实体名称
     * @return 生成的 Controller 代码
     */
    @Tool("生成 REST Controller 模板")
    public String generateControllerTemplate(String entityName) {
        String className = entityName + "Controller";
        String serviceName = entityName + "Service";
        String serviceNameVar = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1) + "Service";
        
        StringBuilder code = new StringBuilder();
        code.append("package com.weaving.llm.controller;\n\n");
        code.append("import com.weaving.llm.service.").append(serviceName).append(";\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.web.bind.annotation.*;\n");
        code.append("import lombok.extern.slf4j.Slf4j;\n\n");
        code.append("@Slf4j\n");
        code.append("@RestController\n");
        code.append("@RequestMapping(\"/api/").append(entityName.toLowerCase()).append("\")\n");
        code.append("public class ").append(className).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(serviceName).append(" ").append(serviceNameVar).append(";\n\n");
        code.append("    @GetMapping\n");
        code.append("    public Object list() {\n");
        code.append("        return ").append(serviceNameVar).append(".findAll();\n");
        code.append("    }\n\n");
        code.append("    @GetMapping(\"/{id}\")\n");
        code.append("    public Object get(@PathVariable String id) {\n");
        code.append("        return ").append(serviceNameVar).append(".findById(id);\n");
        code.append("    }\n\n");
        code.append("    @PostMapping\n");
        code.append("    public Object create(@RequestBody Object request) {\n");
        code.append("        return ").append(serviceNameVar).append(".save(request);\n");
        code.append("    }\n\n");
        code.append("    @DeleteMapping(\"/{id}\")\n");
        code.append("    public Object delete(@PathVariable String id) {\n");
        code.append("        ").append(serviceNameVar).append(".deleteById(id);\n");
        code.append("        return \"success\";\n");
        code.append("    }\n");
        code.append("}\n");
        return code.toString();
    }
    
    /**
     * 生成单元测试模板
     * @param className 要测试的类名
     * @return 生成的测试代码
     */
    @Tool("生成单元测试模板")
    public String generateUnitTestTemplate(String className) {
        String testClassName = className + "Test";
        
        StringBuilder code = new StringBuilder();
        code.append("package com.weaving.llm;\n\n");
        code.append("import com.weaving.llm.service.").append(className).append(";\n");
        code.append("import org.junit.jupiter.api.Test;\n");
        code.append("import org.springframework.boot.test.context.SpringBootTest;\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        code.append("@SpringBootTest\n");
        code.append("public class ").append(testClassName).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(className).append(" ").append(Character.toLowerCase(className.charAt(0))).append(className.substring(1)).append(";\n\n");
        code.append("    @Test\n");
        code.append("    public void testCreate() {\n");
        code.append("        // TODO: 编写测试逻辑\n");
        code.append("        assertNotNull(").append(Character.toLowerCase(className.charAt(0))).append(className.substring(1)).append(");\n");
        code.append("    }\n");
        code.append("}\n");
        return code.toString();
    }
}
