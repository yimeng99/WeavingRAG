package com.weaving.llm.common.utils.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一文档转换服务
 * 提供统一的文档转换入口，自动识别文件类型并调用对应的转换器
 */
@Slf4j
@Service
public class DocumentConversionService {
    
    private final Map<String, DocumentConverter> converterMap = new HashMap<>();
    private final List<DocumentConverter> converters;
    
    @Value("${file.upload.path:uploads}")
    private String uploadPath;
    
    public DocumentConversionService(List<DocumentConverter> converters) {
        this.converters = converters;
        // 初始化转换器映射
        for (DocumentConverter converter : converters) {
            converterMap.put(converter.getSupportedType(), converter);
            log.info("注册文档转换器: {} -> {}", converter.getSupportedType(), converter.getClass().getSimpleName());
        }
    }
    
    /**
     * 转换文件
     * @param file 文件对象
     * @return 转换结果
     */
    public ConversionResult convert(File file) {
        if (file == null || !file.exists()) {
            return ConversionResult.failure(null, "文件不存在");
        }
        
        String fileName = file.getName();
        DocumentConverter converter = findConverter(fileName);
        
        if (converter == null) {
            return ConversionResult.failure(fileName, "不支持的文件格式: " + getFileExtension(fileName));
        }
        
        log.debug("使用 {} 转换文件: {}", converter.getClass().getSimpleName(), fileName);
        return converter.convert(file);
    }
    
    /**
     * 转换输入流
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 转换结果
     */
    public ConversionResult convert(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            return ConversionResult.failure(fileName, "输入流为空");
        }
        
        DocumentConverter converter = findConverter(fileName);
        
        if (converter == null) {
            return ConversionResult.failure(fileName, "不支持的文件格式: " + getFileExtension(fileName));
        }
        
        log.debug("使用 {} 转换文件: {}", converter.getClass().getSimpleName(), fileName);
        return converter.convert(inputStream, fileName);
    }
    
    /**
     * 转换 MultipartFile
     * @param multipartFile Spring MultipartFile
     * @return 转换结果
     */
    public ConversionResult convert(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return ConversionResult.failure(null, "文件为空");
        }
        
        try {
            return convert(multipartFile.getInputStream(), multipartFile.getOriginalFilename());
        } catch (Exception e) {
            return ConversionResult.failure(
                    multipartFile.getOriginalFilename(), 
                    "读取上传文件失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 提取纯文本
     * @param file 文件对象
     * @return 纯文本内容
     */
    public String extractText(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        
        DocumentConverter converter = findConverter(file.getName());
        if (converter == null) {
            log.warn("不支持的文件格式: {}", file.getName());
            return "";
        }
        
        return converter.extractText(file);
    }
    
    /**
     * 提取纯文本
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 纯文本内容
     */
    public String extractText(InputStream inputStream, String fileName) {
        DocumentConverter converter = findConverter(fileName);
        if (converter == null) {
            log.warn("不支持的文件格式: {}", fileName);
            return "";
        }
        
        return converter.extractText(inputStream, fileName);
    }
    
    /**
     * 检查是否支持指定文件格式
     * @param fileName 文件名
     * @return 是否支持
     */
    public boolean isSupported(String fileName) {
        return findConverter(fileName) != null;
    }
    
    /**
     * 获取支持的文件格式列表
     * @return 支持的格式列表
     */
    public List<String> getSupportedFormats() {
        return converters.stream()
                .map(DocumentConverter::getSupportedType)
                .toList();
    }
    
    /**
     * 查找适合的转换器
     */
    private DocumentConverter findConverter(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        for (DocumentConverter converter : converters) {
            if (converter.supports(fileName)) {
                return converter;
            }
        }
        
        return null;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 保存提取的图片到指定目录
     * @param result 转换结果
     * @param documentId 文档ID（用于创建子目录）
     * @return 保存的图片数量
     */
    public int saveExtractedImages(ConversionResult result, String documentId) {
        if (result == null || result.getImages() == null || result.getImages().isEmpty()) {
            return 0;
        }
        
        try {
            // 创建图片保存目录
            Path imageDir = Paths.get(uploadPath, "images", documentId).toAbsolutePath().normalize();
            Files.createDirectories(imageDir);
            
            int savedCount = 0;
            for (ImageInfo imageInfo : result.getImages()) {
                if (imageInfo.getData() != null) {
                    Path imagePath = imageDir.resolve(imageInfo.getFileName());
                    try (FileOutputStream fos = new FileOutputStream(imagePath.toFile())) {
                        fos.write(imageInfo.getData());
                        // 更新图片的保存路径
                        imageInfo.setFilePath(imagePath.toString());
                        savedCount++;
                        log.debug("保存图片: {}", imagePath);
                    }
                }
            }
            
            log.info("保存了 {} 张图片到目录: {}", savedCount, imageDir);
            return savedCount;
        } catch (IOException e) {
            log.error("保存图片失败", e);
            return 0;
        }
    }
    
    /**
     * 转换文件并保存图片
     * @param file 文件对象
     * @param documentId 文档ID
     * @return 转换结果
     */
    public ConversionResult convertWithImages(File file, String documentId) {
        ConversionResult result = convert(file);
        if (result.isSuccess()) {
            saveExtractedImages(result, documentId);
        }
        return result;
    }
    
    /**
     * 获取图片保存目录
     * @param documentId 文档ID
     * @return 图片目录路径
     */
    public String getImageDirectory(String documentId) {
        return Paths.get(uploadPath, "images", documentId).toAbsolutePath().normalize().toString();
    }
}
