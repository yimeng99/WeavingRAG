package com.weaving.llm.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 本地存储路径
     */
    private String localPath = "uploads";

    /**
     * 最大文件大小（字节）默认 100MB
     */
    private long maxFileSize = 104857600;

    /**
     * 允许的文件扩展名
     */
    private String[] allowedExtensions = new String[]{
            "pdf", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "txt", "md", "csv",
            "jpg", "jpeg", "png", "gif", "bmp",
            "zip", "rar", "7z"
    };
}
