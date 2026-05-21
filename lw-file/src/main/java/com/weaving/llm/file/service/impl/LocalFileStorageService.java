package com.weaving.llm.file.service.impl;

import com.weaving.llm.file.config.FileStorageProperties;
import com.weaving.llm.file.domain.FileInfo;
import com.weaving.llm.file.service.FileStorageService;
import com.weaving.llm.file.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service("localFileStorageService")
public class LocalFileStorageService implements FileStorageService {

    @Autowired
    private FileStorageProperties properties;

    private final Path fileStorageLocation;

    public LocalFileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, Long userId) {
        try {
            // 校验文件
            FileUtils.validateFile(file, properties);

            // 生成文件名
            String originalName = file.getOriginalFilename();
            String extension = FileUtils.getExtension(originalName);
            String storedName = FileUtils.generateStoredName(extension);

            // 创建目录（按日期分目录存储）
            String datePath = FileUtils.getDatePath();
            Path targetLocation = this.fileStorageLocation.resolve(datePath);
            Files.createDirectories(targetLocation);

            // 保存文件
            Path filePath = targetLocation.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 计算 MD5
            String md5Hash = FileUtils.calculateMd5(file.getInputStream());

            // 构建文件信息
            return FileInfo.builder()
                    .fileId(FileUtils.generateFileId())
                    .originalName(originalName)
                    .storedName(storedName)
                    .filePath(datePath + "/" + storedName)
                    .fileUrl("/api/file/" + datePath + "/" + storedName)
                    .fileSize(file.getSize())
                    .extension(extension)
                    .contentType(file.getContentType())
                    .storageType("LOCAL")
                    .userId(userId)
                    .uploadTime(new Date())
                    .md5Hash(md5Hash)
                    .build();

        } catch (Exception e) {
            log.error("本地文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String fileId) {
        try {
            // fileId 格式：datePath/storedName
            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在：" + fileId);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("文件下载失败：fileId={}", fileId, e);
            throw new RuntimeException("文件下载失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String fileId) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功：{}", fileId);
                return true;
            }
            log.warn("文件不存在，跳过删除：{}", fileId);
            return false;
        } catch (IOException e) {
            log.error("文件删除失败：fileId={}", fileId, e);
            throw new RuntimeException("文件删除失败：" + e.getMessage(), e);
        }
    }

    @Override
    public FileInfo getFileInfo(String fileId) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
            if (!Files.exists(filePath)) {
                return null;
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            String fileName = filePath.getFileName().toString();
            String extension = FileUtils.getExtension(fileName);

            return FileInfo.builder()
                    .fileId(fileId)
                    .originalName(fileName)
                    .storedName(fileName)
                    .filePath(fileId)
                    .fileSize(attrs.size())
                    .extension(extension)
                    .storageType("LOCAL")
                    .uploadTime(new Date(attrs.creationTime().toMillis()))
                    .build();
        } catch (IOException e) {
            log.error("获取文件信息失败：fileId={}", fileId, e);
            return null;
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
        try {
            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
}
