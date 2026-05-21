package com.weaving.llm.app.controller;

import com.weaving.llm.common.domain.R;
import com.weaving.llm.common.service.OSSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 依梦
 * @Date: 2025/10/27
 * @Description: 文件上传控制器
 */
@Slf4j
@Tag(name = "文件管理", description = "文件上传、删除等管理接口")
@RestController
@RequestMapping("/v0/files")
public class FileUploadController {
    
    @Autowired
    private OSSService ossService;
    
    /**
     * 单文件上传
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传单个文件到 OSS 对象存储")
    public R<Map<String, Object>> uploadFile(
            @Parameter(description = "要上传的文件", required = true) @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        
        try {
            String fileUrl = ossService.uploadFile(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return R.fail("上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileUrl}")
    @Operation(summary = "删除文件", description = "从 OSS 对象存储中删除指定文件")
    public R<Void> deleteFile(
            @Parameter(description = "文件 URL", required = true) @PathVariable String fileUrl) {
        try {
            ossService.deleteFile(fileUrl);
            return R.ok();
        } catch (Exception e) {
            log.error("文件删除失败：{}", fileUrl, e);
            return R.fail("删除失败：" + e.getMessage());
        }
    }
}
