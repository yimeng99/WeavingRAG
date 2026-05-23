package com.weaving.llm.rag.domain.dto;

import com.weaving.llm.common.domain.KnowledgeDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文档创建 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentCreateDto extends KnowledgeDocument {

    /**
     * 知识库ID（必填）- 重写父类约束
     */
    @NotBlank(message = "知识库ID不能为空")
    private String knowledgeBaseId;

    /**
     * 文档标题（必填）
     */
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "文档标题长度不能超过 255 个字符")
    private String title;

    /**
     * 已上传的文件信息列表
     */
    private List<FileInfoDTO> uploadFileList;

    /**
     * 文件信息 DTO
     */
    @Data
    public static class FileInfoDTO {
        /**
         * 文件路径
         */
        private String filePath;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 文件类型
         */
        private String fileType;

        /**
         * 文件大小
         */
        private Long fileSize;
    }
}