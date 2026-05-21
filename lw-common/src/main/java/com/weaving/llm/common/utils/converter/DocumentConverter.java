package com.weaving.llm.common.utils.converter;

import java.io.File;
import java.io.InputStream;

/**
 * 文档转换器接口
 * 将不同格式的文档转换为统一的 Markdown 格式
 */
public interface DocumentConverter {
    
    /**
     * 获取支持的文档类型
     * @return 文档类型标识，如 "docx", "pdf", "xlsx"
     */
    String getSupportedType();
    
    /**
     * 检查是否支持指定文件
     * @param fileName 文件名
     * @return 是否支持
     */
    boolean supports(String fileName);
    
    /**
     * 转换文件
     * @param file 文件对象
     * @return 转换结果
     */
    ConversionResult convert(File file);
    
    /**
     * 转换输入流
     * @param inputStream 输入流
     * @param fileName 文件名（用于确定文件类型）
     * @return 转换结果
     */
    ConversionResult convert(InputStream inputStream, String fileName);
    
    /**
     * 提取纯文本内容
     * @param file 文件对象
     * @return 纯文本内容
     */
    String extractText(File file);
    
    /**
     * 提取纯文本内容
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 纯文本内容
     */
    String extractText(InputStream inputStream, String fileName);
}
