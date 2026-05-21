package com.weaving.llm.common.utils.converter;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 纯文本/Markdown 文件转换器
 * 支持 txt、md、markdown 等格式
 */
@Component
public class TxtConverter implements DocumentConverter {
    
    private static final String[] SUPPORTED_EXTENSIONS = {".txt", ".md", ".markdown", ".text"};
    
    @Override
    public String getSupportedType() {
        return "text";
    }
    
    @Override
    public boolean supports(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public ConversionResult convert(File file) {
        try {
            String content = readFileContent(file);
            
            String fileType = "text";
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
                fileType = "markdown";
            }
            
            ConversionResult result = ConversionResult.success(content, file.getName(), fileType);
            result.setTotalCharacters(content.length());
            
            // 统计行数
            int lineCount = content.split("\n").length;
            result.addMetadata("lineCount", lineCount);
            
            return result;
        } catch (Exception e) {
            return ConversionResult.failure(file.getName(), "读取文本文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public ConversionResult convert(InputStream inputStream, String fileName) {
        try {
            String content = readStreamContent(inputStream);
            
            String fileType = "text";
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
                fileType = "markdown";
            }
            
            ConversionResult result = ConversionResult.success(content, fileName, fileType);
            result.setTotalCharacters(content.length());
            
            int lineCount = content.split("\n").length;
            result.addMetadata("lineCount", lineCount);
            
            return result;
        } catch (Exception e) {
            return ConversionResult.failure(fileName, "读取文本文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public String extractText(File file) {
        try {
            return readFileContent(file);
        } catch (Exception e) {
            return "";
        }
    }
    
    @Override
    public String extractText(InputStream inputStream, String fileName) {
        try {
            return readStreamContent(inputStream);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 读取文件内容，自动检测编码
     */
    private String readFileContent(File file) throws IOException {
        // 首先尝试 UTF-8
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            // 检查是否有乱码（简单检测）
            if (!containsMojibake(content)) {
                return content;
            }
        } catch (Exception ignored) {
        }
        
        // 尝试 GBK（中文 Windows 常用编码）
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = new String(bytes, Charset.forName("GBK"));
            if (!containsMojibake(content)) {
                return content;
            }
        } catch (Exception ignored) {
        }
        
        // 默认返回 UTF-8
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 读取输入流内容
     */
    private String readStreamContent(InputStream inputStream) throws IOException {
        // 读取全部字节
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        byte[] bytes = buffer.toByteArray();
        
        // 尝试 UTF-8
        try {
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (!containsMojibake(content)) {
                return content;
            }
        } catch (Exception ignored) {
        }
        
        // 尝试 GBK
        try {
            String content = new String(bytes, Charset.forName("GBK"));
            if (!containsMojibake(content)) {
                return content;
            }
        } catch (Exception ignored) {
        }
        
        // 默认返回 UTF-8
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 检测是否包含乱码
     */
    private boolean containsMojibake(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 检测常见的乱码模式
        return text.contains("�") || text.contains("");
    }
}
