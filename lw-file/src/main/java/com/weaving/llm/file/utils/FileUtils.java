package com.weaving.llm.file.utils;

import com.weaving.llm.file.config.FileStorageProperties;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * 文件工具类
 */
public class FileUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");

    /**
     * 校验文件
     */
    public static void validateFile(MultipartFile file, FileStorageProperties properties) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 校验文件大小
        if (properties != null && file.getSize() > properties.getMaxFileSize()) {
            throw new IllegalArgumentException(
                    "文件大小超过限制：" + (properties.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        // 校验文件扩展名
        String extension = getExtension(originalName);
        if (properties != null && properties.getAllowedExtensions() != null) {
            if (!Arrays.asList(properties.getAllowedExtensions()).contains(extension.toLowerCase())) {
                throw new IllegalArgumentException("不支持的文件类型：" + extension);
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 生成存储文件名
     */
    public static String generateStoredName(String extension) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return timestamp + "_" + uuid + "." + extension;
    }

    /**
     * 生成文件 ID
     */
    public static String generateFileId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取日期路径（用于分目录存储）
     */
    public static String getDatePath() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * 计算文件 MD5
     */
    public static String calculateMd5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算 MD5 失败", e);
        }
    }

    /**
     * 计算文件 MD5（从 InputStream）
     */
    public static String calculateMd5(java.io.InputStream inputStream) throws Exception {
        byte[] data = inputStream.readAllBytes();
        return calculateMd5(data);
    }

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
