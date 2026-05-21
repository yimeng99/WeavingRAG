package com.weaving.llm.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.weaving.llm.file.domain.FileInfo;
import com.weaving.llm.file.service.FileStorageService;
import com.weaving.llm.file.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * 阿里云 OSS 文件存储服务实现
 */
@Slf4j
@Service("ossFileStorageService")
@ConditionalOnProperty(name = "aliyun.oss.enabled", havingValue = "true", matchIfMissing = false)
public class OssFileStorageService implements FileStorageService {

    @Value("${aliyun.oss.endpoint:oss-cn-hangzhou.aliyuncs.com}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name:rag-weaving}")
    private String bucketName;

    @Value("${aliyun.oss.accessKeyId:}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret:}")
    private String accessKeySecret;

    private OSS getOssClient() {
//        return new OSS(endpoint, accessKeyId, accessKeySecret);
        return null;
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, Long userId) {
        OSS ossClient = null;
        try {
            // 校验文件
            FileUtils.validateFile(file, null);

            // 生成文件名
            String originalName = file.getOriginalFilename();
            String extension = FileUtils.getExtension(originalName);
            String storedName = FileUtils.generateStoredName(extension);

            // 生成 OSS 路径（按日期分目录）
            String datePath = FileUtils.getDatePath();
            String objectKey = datePath + "/" + storedName;

            // 上传到 OSS
            ossClient = getOssClient();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, objectKey, file.getInputStream(), metadata);
            ossClient.putObject(putObjectRequest);

            // 计算 MD5
            String md5Hash = FileUtils.calculateMd5(file.getInputStream());

            // 构建文件信息
            String fileUrl = "https://" + bucketName + "." + endpoint + "/" + objectKey;

            return FileInfo.builder()
                    .fileId(objectKey)
                    .originalName(originalName)
                    .storedName(storedName)
                    .filePath(objectKey)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .extension(extension)
                    .contentType(file.getContentType())
                    .storageType("OSS")
                    .userId(userId)
                    .uploadTime(new Date())
                    .md5Hash(md5Hash)
                    .build();

        } catch (Exception e) {
            log.error("OSS 文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public InputStream downloadFile(String fileId) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            InputStream inputStream = ossClient.getObject(bucketName, fileId).getObjectContent();
            if (inputStream == null) {
                throw new FileNotFoundException("文件不存在：" + fileId);
            }
            return inputStream;
        } catch (Exception e) {
            log.error("文件下载失败：fileId={}", fileId, e);
            throw new RuntimeException("文件下载失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public boolean deleteFile(String fileId) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            ossClient.deleteObject(bucketName, fileId);
            log.info("OSS 文件删除成功：{}", fileId);
            return true;
        } catch (Exception e) {
            log.error("OSS 文件删除失败：fileId={}", fileId, e);
            throw new RuntimeException("文件删除失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public FileInfo getFileInfo(String fileId) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            ObjectMetadata objectSummary =
                    ossClient.getObjectMetadata(bucketName, fileId);

            if (objectSummary == null) {
                return null;
            }

            return FileInfo.builder()
                    .fileId(fileId)
                    .fileSize(objectSummary.getContentLength())
                    .contentType(objectSummary.getContentType())
                    .storageType("OSS")
                    .uploadTime(objectSummary.getLastModified())
                    .build();
        } catch (Exception e) {
            log.error("获取文件信息失败：fileId={}", fileId, e);
            return null;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public List<FileInfo> uploadFiles(List<MultipartFile> files, Long userId) {
        List<FileInfo> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                result.add(uploadFile(file, userId));
            }
        }
        return result;
    }

    @Override
    public boolean exists(String fileId) {
        OSS ossClient = null;
        try {
            ossClient = getOssClient();
            return ossClient.doesObjectExist(bucketName, fileId);
        } catch (Exception e) {
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
