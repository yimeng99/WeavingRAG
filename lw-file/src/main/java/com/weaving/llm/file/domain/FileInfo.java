package com.weaving.llm.file.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 文件信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    /**
     * 文件 ID
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storedName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件 URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 文件 MIME 类型
     */
    private String contentType;

    /**
     * 存储类型：LOCAL-本地，OSS-阿里云
     */
    private String storageType;

    /**
     * 上传用户 ID
     */
    private Long userId;

    /**
     * 上传时间
     */
    private Date uploadTime;

    /**
     * 文件描述
     */
    private String description;

    /**
     * MD5 校验值
     */
    private String md5Hash;
}
