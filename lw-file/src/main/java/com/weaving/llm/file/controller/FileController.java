package com.weaving.llm.file.controller;

import com.weaving.llm.common.context.CurrentUserHolder;
import com.weaving.llm.common.domain.R;
import com.weaving.llm.file.domain.FileInfo;
import com.weaving.llm.file.service.FileConversionService;
import com.weaving.llm.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Tag(name = "文件管理", description = "文件上传、下载、删除等管理接口")
@RestController
@RequestMapping("/v0/file")
public class FileController {

    @Autowired
    @Qualifier("localFileStorageService")
    private FileStorageService fileStorageService;

    @Autowired
    private FileConversionService fileConversionService;


    @PostMapping("/upload")
    @Operation(summary = "上传单个文件")
    public R<Map<String, Object>> uploadFile(@Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file) {
        log.info("接收到文件上传请求，文件名：{}, 大小：{}", file.getOriginalFilename(), file.getSize());
        Long userId = CurrentUserHolder.getUserId();

        try {
            FileInfo fileInfo = fileStorageService.uploadFile(file, userId != null ? userId : 0L);
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileInfo.getOriginalName());
            result.put("filePath", fileInfo.getFilePath());
            result.put("fileUrl", fileInfo.getFileUrl());
            result.put("fileSize", fileInfo.getFileSize());
            result.put("contentType", fileInfo.getContentType());
            return R.ok(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    public R<Map<String, Object>> uploadFiles(@Parameter(description = "上传的文件列表") @RequestParam("files") List<MultipartFile> files) {
        log.info("接收到批量文件上传请求，文件数：{}", files.size());
        Long userId = CurrentUserHolder.getUserId();

        try {
            List<FileInfo> fileInfos = fileStorageService.uploadFiles(files, userId != null ? userId : 0L);
            Map<String, Object> result = new HashMap<>();
            result.put("total", fileInfos.size());
            result.put("files", fileInfos);
            return R.ok(result);
        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return R.fail("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "文件 ID/路径") @PathVariable String fileId) {

        log.info("接收到文件下载请求：fileId={}", fileId);

        try (InputStream inputStream = fileStorageService.downloadFile(fileId)) {
            byte[] content = inputStream.readAllBytes();

            FileInfo fileInfo = fileStorageService.getFileInfo(fileId);
            String contentType = fileInfo != null ? fileInfo.getContentType() : "application/octet-stream";
            String originalName = fileInfo != null ? fileInfo.getOriginalName() : "download";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content);

        } catch (IOException e) {
            log.error("文件下载失败：fileId={}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 预览文件（在浏览器中打开）
     */
    @GetMapping("/preview/{fileId}")
    @Operation(summary = "预览文件")
    public ResponseEntity<byte[]> previewFile(
            @Parameter(description = "文件 ID/路径") @PathVariable String fileId) {

        log.info("接收到文件预览请求：fileId={}", fileId);

        try (InputStream inputStream = fileStorageService.downloadFile(fileId)) {
            byte[] content = inputStream.readAllBytes();

            FileInfo fileInfo = fileStorageService.getFileInfo(fileId);
            String contentType = fileInfo != null ? fileInfo.getContentType() : "application/octet-stream";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content);

        } catch (IOException e) {
            log.error("文件预览失败：fileId={}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public R<Boolean> deleteFile(
            @Parameter(description = "文件 ID/路径") @PathVariable String fileId) {

        log.info("接收到文件删除请求：fileId={}", fileId);

        try {
            boolean deleted = fileStorageService.deleteFile(fileId);
            return R.ok(deleted);
        } catch (Exception e) {
            log.error("文件删除失败：fileId={}", fileId, e);
            return R.fail("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/info/{fileId}")
    @Operation(summary = "获取文件信息")
    public R<FileInfo> getFileInfo(
            @Parameter(description = "文件 ID/路径") @PathVariable String fileId) {

        log.info("接收到文件信息查询请求：fileId={}", fileId);

        FileInfo fileInfo = fileStorageService.getFileInfo(fileId);
        if (fileInfo != null) {
            return R.ok(fileInfo);
        } else {
            return R.fail("文件不存在");
        }
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists/{fileId}")
    @Operation(summary = "检查文件是否存在")
    public R<Boolean> checkFileExists(
            @Parameter(description = "文件 ID/路径") @PathVariable String fileId) {

        boolean exists = fileStorageService.exists(fileId);
        return R.ok(exists);
    }

    /**
     * 获取支持的转换格式
     */
    @GetMapping("/conversion/supported/{extension}")
    @Operation(summary = "获取支持的转换格式")
    public R<List<String>> getSupportedConversions(
            @Parameter(description = "源文件扩展名") @PathVariable String extension) {

        List<String> supportedTargets = fileConversionService.getSupportedTargets(extension);
        return R.ok(supportedTargets);
    }
}
