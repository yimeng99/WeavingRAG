package com.weaving.llm.common.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.weaving.llm.common.config.OSSConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 阿里云 OSS 文件上传服务
 */
@Slf4j
@Service
public class OSSService {
    
    @Autowired
    private OSS ossClient;
    
    @Autowired
    private OSSConfig ossConfig;
    
    /**
     * 上传文件到 OSS
     * @param file 上传的文件
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String newFileName = generateFileName(originalFilename);
        
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossConfig.getBucketName(), 
                newFileName, 
                inputStream
            );
            
            // 设置文件 ACL 为公共读 (使用单独的请求)
            ossClient.setObjectAcl(ossConfig.getBucketName(), newFileName, CannedAccessControlList.PublicRead);
            
            ossClient.putObject(putObjectRequest);
            
            String fileUrl = getFileUrl(newFileName);
            log.info("文件上传成功：{}, URL: {}", originalFilename, fileUrl);
            
            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传失败：{}", originalFilename, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    /**
     * 上传字节数组到 OSS
     * @param content 文件内容
     * @param fileName 文件名
     * @return 文件访问 URL
     */
    public String uploadBytes(byte[] content, String fileName) {
        String newFileName = generateFileName(fileName);
        
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossConfig.getBucketName(), 
                newFileName, 
                inputStream
            );
            
            // 上传后设置 ACL 为公共读
            ossClient.putObject(putObjectRequest);
            ossClient.setObjectAcl(ossConfig.getBucketName(), newFileName, CannedAccessControlList.PublicRead);
            
            String fileUrl = getFileUrl(newFileName);
            log.info("字节数据上传成功：{}, URL: {}", fileName, fileUrl);
            
            return fileUrl;
        } catch (Exception e) {
            log.error("字节数据上传失败：{}", fileName, e);
            throw new RuntimeException("字节数据上传失败", e);
        }
    }
    
    /**
     * 删除 OSS 中的文件
     * @param fileUrl 文件 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件删除成功：{}", fileUrl);
        } catch (Exception e) {
            log.error("文件删除失败：{}", fileUrl, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }
    
    /**
     * 获取文件的临时访问 URL(带签名)
     * @param objectKey 对象键
     * @param expirationMinutes 过期时间 (分钟)
     * @return 带签名的 URL
     */
    public String getPresignedUrl(String objectKey, int expirationMinutes) {
        Date expiration = new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L);
        URL url = ossClient.generatePresignedUrl(ossConfig.getBucketName(), objectKey, expiration);
        return url.toString();
    }
    
    /**
     * 生成唯一的文件名
     */
    private String generateFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "unnamed";
        }
        
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        return "uploads/" + timestamp + "_" + uuid + extension;
    }
    
    /**
     * 从完整 URL 中提取对象键
     */
    private String extractObjectKey(String fileUrl) {
        // 例如：https://bucket-name.endpoint/object-key -> object-key
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            throw new RuntimeException("无法解析文件 URL", e);
        }
    }
    
    /**
     * 获取文件的完整访问 URL
     */
    private String getFileUrl(String objectKey) {
        return String.format("https://%s.%s/%s", 
            ossConfig.getBucketName(), 
            ossConfig.getEndpoint().replaceFirst("^https?://", ""),
            objectKey);
    }
}
