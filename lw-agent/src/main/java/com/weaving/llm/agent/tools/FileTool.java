package com.weaving.llm.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件操作工具
 * 提供文件读写、删除、目录浏览等功能
 */
@Slf4j
@Component
public class FileTool implements AgentTool {
    
    @Override
    public String getName() {
        return "file_manager";
    }
    
    @Override
    public String getDescription() {
        return "读取、写入、删除文本文件，管理文件和目录";
    }
    
    @Override
    public String getCategory() {
        return "FILE";
    }
    
    @Tool("读取文本文件的内容")
    public String readFile(String filePath) {
        log.info("读取文件：{}", filePath);
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "文件不存在：" + filePath;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return String.format("文件内容 (%s):\n\n%s", filePath, content);
        } catch (Exception e) {
            log.error("读取文件失败", e);
            return "读取文件失败：" + e.getMessage();
        }
    }
    
    @Tool("将文本内容写入文件")
    public String writeFile(String filePath, String content) {
        log.info("写入文件：{}", filePath);
        try {
            Path path = Paths.get(filePath);
            Path parentDir = path.getParent();
            
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "文件写入成功：" + filePath;
        } catch (Exception e) {
            log.error("写入文件失败", e);
            return "写入文件失败：" + e.getMessage();
        }
    }
    
    @Tool("删除文件或空目录")
    public String deleteFileOrDirectory(String path) {
        log.info("删除路径：{}", path);
        try {
            Path filePath = Paths.get(path);
            
            if (!Files.exists(filePath)) {
                return "路径不存在：" + path;
            }
            
            if (Files.isDirectory(filePath)) {
                // 删除空目录
                Files.delete(filePath);
                return "目录已删除：" + path;
            } else {
                // 删除文件
                Files.delete(filePath);
                return "文件已删除：" + path;
            }
        } catch (Exception e) {
            log.error("删除失败", e);
            return "删除失败：" + e.getMessage();
        }
    }
    
    @Tool("列出目录下的文件和文件夹")
    public String listDirectory(String directoryPath) {
        log.info("列出目录：{}", directoryPath);
        try {
            Path dir = Paths.get(directoryPath);
            
            if (!Files.exists(dir)) {
                return "目录不存在：" + directoryPath;
            }
            
            if (!Files.isDirectory(dir)) {
                return "路径不是目录：" + directoryPath;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("目录：").append(directoryPath).append("\n\n");
            
            int fileCount = 0;
            int dirCount = 0;
            
            try (var stream = Files.list(dir)) {
                var files = stream.toList();
                sb.append("总计：").append(files.size()).append(" 个项目\n\n");
                sb.append("内容列表:\n");
                
                for (var file : files) {
                    String type = Files.isDirectory(file) ? "[文件夹]" : "[文件]";
                    String size = Files.isDirectory(file) ? "-" : Files.size(file) + " B";
                    sb.append(String.format("%s %-30s %s%n", type, file.getFileName(), size));
                    
                    if (Files.isDirectory(file)) dirCount++;
                    else fileCount++;
                }
            }
            
            sb.append(String.format("\n统计：%d 个文件，%d 个文件夹", fileCount, dirCount));
            return sb.toString();
        } catch (Exception e) {
            log.error("列出目录失败", e);
            return "列出目录失败：" + e.getMessage();
        }
    }
}
