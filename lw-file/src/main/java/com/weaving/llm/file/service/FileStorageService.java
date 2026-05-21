package com.weaving.llm.file.service;

import com.weaving.llm.file.domain.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param userId 用户 ID
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, Long userId);

    /**
     * 下载文件
     *
     * @param fileId 文件 ID
     * @return 文件输入流
     */
    InputStream downloadFile(String fileId);

    /**
     * 删除文件
     *
     * @param fileId 文件 ID
     * @return 是否成功
     */
    boolean deleteFile(String fileId);

    /**
     * 获取文件信息
     *
     * @param fileId 文件 ID
     * @return 文件信息
     */
    FileInfo getFileInfo(String fileId);

    /**
     * 批量上传文件
     *
     * @param files 文件列表
     * @param userId 用户 ID
     * @return 文件信息列表
     */
    List<FileInfo> uploadFiles(List<MultipartFile> files, Long userId);

    /**
     * 检查文件是否存在
     *
     * @param fileId 文件 ID
     * @return 是否存在
     */
    boolean exists(String fileId);
}
