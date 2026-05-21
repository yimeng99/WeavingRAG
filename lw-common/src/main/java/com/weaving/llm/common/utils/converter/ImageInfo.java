package com.weaving.llm.common.utils.converter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageInfo {
    
    /**
     * 图片文件名
     */
    private String fileName;
    
    /**
     * 图片保存路径
     */
    private String filePath;
    
    /**
     * 图片格式（如 png, jpg, jpeg 等）
     */
    private String format;
    
    /**
     * 图片 MIME 类型
     */
    private String mimeType;
    
    /**
     * 图片字节数据
     */
    private byte[] data;
    
    /**
     * 图片宽度（像素）
     */
    private Integer width;
    
    /**
     * 图片高度（像素）
     */
    private Integer height;
    
    /**
     * 图片在文档中的位置索引
     */
    private Integer index;
    
    /**
     * 图片所在页码（适用于 PDF）
     */
    private Integer pageNumber;
    
    /**
     * 图片描述（如果有）
     */
    private String description;
        
    /**
     * 图片前面的文本内容（用于定位图片位置）
     */
    private String precedingText;
        
    /**
     * 图片后面的文本内容（用于定位图片位置）
     */
    private String followingText;
        
    /**
     * 图片所在段落索引
     */
    private Integer paragraphIndex;
        
    /**
     * 图片在文档中的字符位置
     */
    private Integer charPosition;
        
    /**
     * 图片所在段落的完整文本
     */
    private String paragraphText;
    
    /**
     * 创建图片信息
     */
    public static ImageInfo of(String fileName, byte[] data, String format) {
        return ImageInfo.builder()
                .fileName(fileName)
                .data(data)
                .format(format)
                .mimeType(getMimeType(format))
                .build();
    }
    
    /**
     * 根据格式获取 MIME 类型
     */
    public static String getMimeType(String format) {
        if (format == null) {
            return "image/unknown";
        }
        switch (format.toLowerCase()) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tiff":
            case "tif":
                return "image/tiff";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "image/" + format.toLowerCase();
        }
    }
}
