package com.weaving.llm.common.utils.converter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档转换结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResult {
    
    /**
     * 转换后的文本内容
     */
    private String content;
    
    /**
     * 原始文件名
     */
    private String originalFileName;
    
    /**
     * 原始文件类型
     */
    private String originalFileType;
    
    /**
     * 转换后的格式类型
     */
    private String convertedFormat;
    
    /**
     * 总页数（适用于 PDF、Word 等分页文档）
     */
    private Integer totalPages;
    
    /**
     * 总字数
     */
    private Integer totalCharacters;
    
    /**
     * 元数据信息
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 提取的图片列表
     */
    @Builder.Default
    private List<ImageInfo> images = new ArrayList<>();
    
    /**
     * 转换是否成功
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * 添加图片
     */
    public void addImage(ImageInfo image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
    }
    
    /**
     * 创建成功结果
     */
    public static ConversionResult success(String content, String originalFileName, String originalFileType) {
        return ConversionResult.builder()
                .content(content)
                .originalFileName(originalFileName)
                .originalFileType(originalFileType)
                .convertedFormat("MARKDOWN")
                .totalCharacters(content != null ? content.length() : 0)
                .success(true)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ConversionResult failure(String originalFileName, String errorMessage) {
        return ConversionResult.builder()
                .originalFileName(originalFileName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 获取图片位置摘要信息
     * 用于在切片时确定图片的位置上下文
     * @return 图片位置摘要列表
     */
    public List<Map<String, Object>> getImagePositionSummary() {
        List<Map<String, Object>> summary = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return summary;
        }
        
        for (ImageInfo image : images) {
            Map<String, Object> position = new HashMap<>();
            position.put("index", image.getIndex());
            position.put("fileName", image.getFileName());
            position.put("pageNumber", image.getPageNumber());
            position.put("paragraphIndex", image.getParagraphIndex());
            position.put("charPosition", image.getCharPosition());
            position.put("precedingText", image.getPrecedingText());
            position.put("followingText", image.getFollowingText());
            position.put("paragraphText", image.getParagraphText());
            summary.add(position);
        }
        
        return summary;
    }
    
    /**
     * 根据文本位置查找对应的图片
     * @param textStart 文本起始位置
     * @param textEnd 文本结束位置
     * @return 在该范围内的图片列表
     */
    public List<ImageInfo> getImagesInRange(int textStart, int textEnd) {
        List<ImageInfo> result = new ArrayList<>();
        if (images == null) {
            return result;
        }
        
        for (ImageInfo image : images) {
            Integer charPos = image.getCharPosition();
            if (charPos != null && charPos >= textStart && charPos < textEnd) {
                result.add(image);
            }
        }
        
        return result;
    }
}
