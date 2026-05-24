package com.weaving.llm.rag.controller;

import com.weaving.llm.common.domain.*;
import com.weaving.llm.common.enums.ChunkingStrategyEnum;
import com.weaving.llm.common.pages.PageDataResult;
import com.weaving.llm.common.pages.PageUtils;
import com.weaving.llm.common.service.LocalFileService;
import com.weaving.llm.common.utils.StringUtils;
import com.weaving.llm.common.utils.converter.ConversionResult;
import com.weaving.llm.common.utils.converter.DocumentConversionService;
import com.weaving.llm.rag.domain.dto.DocumentChunkDto;
import com.weaving.llm.rag.domain.dto.DocumentCreateDto;
import com.weaving.llm.rag.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "知识库文档管理", description = "知识库文档、切片、标签、检索等管理接口")
@RestController
@RequestMapping("/v0/knowledge/documents")
public class KnowledgeDocumentController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private DocumentEmbeddingService documentEmbeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private LocalFileService localFileService;

    @Autowired
    private KnowledgeTagService knowledgeTagService;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private TextChunkingService textChunkingService;

    @Autowired
    private DocumentConversionService documentConversionService;



    @GetMapping("/page")
    @Operation(summary = "获取文档列表", description = "分页查询知识库下的文档列表")
    public R<PageDataResult> getDocuments(@ModelAttribute KnowledgeDocument knowledgeDocument) {
        PageUtils.startPage();
        List<KnowledgeDocument> list = knowledgeDocumentService.pageList(knowledgeDocument);
        return R.ok(PageUtils.generatePageDataResult(list));
    }

    @PostMapping
    @Operation(summary = "保存文档", description = "保存文档记录")
    public R<Map<String, Object>> saveDocument(@Valid @RequestBody DocumentCreateDto dto) {
        // 多个文件时，每文件插入一条记录
        int savedCount = 0;
        List<String> docIds = new ArrayList<>();
        List<Map<String, Object>> failedFiles = new ArrayList<>();
        List<DocumentCreateDto.FileInfoDTO> fileList = dto.getUploadFileList();
        // 遍历每个文件，每个文件插入一条记录
        for (DocumentCreateDto.FileInfoDTO fileInfo : fileList) {
            String fullPath = localFileService.getFullPath(fileInfo.getFilePath());
            File file = new File(fullPath);
            ConversionResult conversionResult = documentConversionService.convert(file);
            if (!conversionResult.isSuccess()) {
                log.error("文件转换失败：{}，跳过此文件", fileInfo.getFilePath());
                Map<String, Object> failedFile = new HashMap<>();
                failedFile.put("filePath", fileInfo.getFilePath());
                failedFile.put("fileName", fileInfo.getFileName());
                failedFile.put("reason", conversionResult.getErrorMessage());
                failedFiles.add(failedFile);
                continue;
            }
            KnowledgeDocument document = KnowledgeDocument.builder()
                    .knowledgeBaseId(dto.getKnowledgeBaseId())
                    .title(fileInfo.getFileName() != null ? fileInfo.getFileName() : dto.getTitle())
                    .content(conversionResult.getContent())
                    .type(conversionResult.getOriginalFileType())
                    .source(fileInfo.getFilePath())
                    .tags(dto.getTags())
                    .chunkingStrategy(dto.getChunkingStrategy())
                    .chunkSize(dto.getChunkSize())
                    .status(1)
                    .build();
            knowledgeDocumentService.save(document);
            docIds.add(document.getDocId());
            savedCount++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("savedCount", savedCount);
        result.put("docIds", docIds);
        result.put("failedCount", failedFiles.size());
        result.put("failedFiles", failedFiles);
        return R.ok(result);
    }

    @GetMapping("/chunking-strategies")
    @Operation(summary = "查询分片策略", description = "获取所有可用的分片策略列表")
    public R<List<Map<String, String>>> getChunkingStrategiesMap() {
        List<Map<String, String>> strategies = new ArrayList<>();
        for (ChunkingStrategyEnum strategy : ChunkingStrategyEnum.values()) {
            Map<String, String> item = new HashMap<>();
            item.put("value", strategy.getCode());
            item.put("label", strategy.getDescription());
            strategies.add(item);
        }
        return R.ok(strategies);
    }


    @GetMapping("/{docId}")
    @Operation(summary = "获取文档详情", description = "根据文档 ID 查询详细信息")
    public R<KnowledgeDocument> getDocument(@PathVariable String docId) {
        return R.ok(knowledgeDocumentService.getById(docId));
    }

    @PutMapping
    @Operation(summary = "更新文档", description = "更新文档信息")
    public R<Boolean> updateDocument(@RequestBody KnowledgeDocument document) {
        return R.ok(knowledgeDocumentService.updateById(document));
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "删除文档", description = "删除文档及对应的向量")
    public R<Boolean> deleteDocument(@PathVariable String docId) {
        log.info("删除文档：{}", docId);

        KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
        String knowledgeBaseId = doc != null ? doc.getKnowledgeBaseId() : null;

        try {
            documentEmbeddingService.deleteVectorsByDocId(docId);
        } catch (Exception e) {
            log.warn("删除向量失败，但继续删除文档记录：{}", e.getMessage());
        }

        boolean result = knowledgeDocumentService.removeById(docId);

        if (result && knowledgeBaseId != null) {
            knowledgeBaseService.decrementDocCount(knowledgeBaseId);
        }

        return R.ok(result);
    }


    @DeleteMapping("/removeAllChunk")
    @Operation(summary = "删除文档切片", description = "删除文档的所有切片")
    public R<Boolean> deleteChunks(@RequestParam String docId) {
        documentChunkService.deleteChunksByDocId(docId);
        return R.ok(true);
    }

    @PostMapping("/reChunk")
    @Operation(summary = "重新分片文档", description = "使用新的配置重新分片文档")
    public R<Map<String, Object>> reChunkDocument(@RequestParam String docId) {
        try {
            log.info("接收到重新分片请求，docId={}", docId);

            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc == null) {
                return R.fail("文档不存在");
            }

            String content = doc.getContent();
            if (content == null || content.trim().isEmpty()) {
                return R.fail("文档内容为空，无法分片");
            }

            // 使用文档原有配置
            String chunkingStrategy = doc.getChunkingStrategy();
            Integer chunkSize = doc.getChunkSize();
            if (chunkingStrategy == null) chunkingStrategy = "intelligent";
            if (chunkSize == null || chunkSize <= 0) chunkSize = 1000;

            documentChunkService.deleteChunksByDocId(docId);
            log.info("已删除旧切片，docId={}", docId);

            Map<String, Object> result = new HashMap<>();
            int chunkCount = performChunking(docId, content, chunkingStrategy, chunkSize, doc, result);

            doc.setChunkCount(chunkCount);
            doc.setStatus(1);
            knowledgeDocumentService.updateById(doc);

            result.put("chunkCount", chunkCount);
            return R.ok(result);
        } catch (Exception e) {
            log.error("重新分片失败，docId={}", docId, e);
            return R.fail("重新分片失败：" + e.getMessage());
        }
    }

    @PostMapping("/embedAsync")
    @Operation(summary = "异步批量向量化", description = "异步批量处理文档向量化")
    public R<Map<String, Object>> embedDocumentsAsync(@RequestBody List<String> docIds) {
        log.info("接收到异步批量文档向量化请求，共 {} 个文档", docIds.size());
        List<DocumentChunk> chunks = documentChunkService.getDocumentChunksByDocIds(docIds);
        if (StringUtils.isEmpty(chunks)) {
            return R.ok("切片列表为空!");
        }
        // 1. 先调用向量化服务，将内容向量化
//        List<DocumentChunkDto> vectorDocumentChunks =

        // 2. 在调用向量化存储，将向量化后内容存储起来

        vectorStoreService.embedDocumentChunks(chunks);
        return R.ok("向量化成功!");
    }


    /**
     * 执行文档分片
     */
    private int performChunking(String docId, String content, String strategy, int chunkSize,
                                KnowledgeDocument doc, Map<String, Object> result) {
        try {
            String actualStrategy = mapChunkingStrategy(strategy);

            List<Map<String, Object>> chunks = textChunkingService.chunkWithMeta(content, actualStrategy, chunkSize, 0);

            int savedCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> chunkData = chunks.get(i);
                String chunkContent = (String) chunkData.get("content");

                if (chunkContent != null && !chunkContent.trim().isEmpty()) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .docId(docId)
                            .content(chunkContent)
                            .chunkIndex(i)
                            .status(1)
                            .build();
                    documentChunkService.save(chunk);
                    savedCount++;
                }
            }

            // 向量化处理
            log.info("开始向量化处理，docId={}, 切片数={}", docId, savedCount);
            try {
                List<DocumentChunk> savedChunks = documentChunkService.getChunksByDocId(docId);
                if (!savedChunks.isEmpty()) {
                    List<String> chunkTexts = savedChunks.stream()
                            .map(DocumentChunk::getContent)
                            .collect(Collectors.toList());

                    List<String> vectorIds = vectorStoreService.addVectors(
                            chunkTexts,
                            docId,
                            doc.getKnowledgeBaseId(),
                            doc.getUserId(),
                            doc.getTitle()
                    );

                    for (int i = 0; i < savedChunks.size() && i < vectorIds.size(); i++) {
                        DocumentChunk chunk = savedChunks.get(i);
                        chunk.setVectorId(vectorIds.get(i));
                        documentChunkService.updateById(chunk);
                    }
                    log.info("向量化完成，docId={}, 向量数={}", docId, vectorIds.size());
                }
            } catch (Exception e) {
                log.error("向量化失败，docId={}", docId, e);
                result.put("embeddingError", "向量化失败：" + e.getMessage());
            }

            return savedCount;
        } catch (Exception e) {
            log.error("文档分片失败，docId={}", docId, e);
            return 0;
        }
    }

    /**
     * 映射前端策略到后端策略
     */
    private String mapChunkingStrategy(String strategy) {
        switch (strategy) {
            case "intelligent":
                return "paragraph";
            case "char":
                return "char";
            case "page":
                return "paragraph";
            case "heading":
                return "separator";
            case "regex":
                return "separator";
            case "separator":
                return "separator";
            default:
                return "char";
        }
    }



    @PutMapping("/{docId}/status")
    @Operation(summary = "更新文档状态", description = "更新文档处理状态")
    public R<Boolean> updateDocStatus(@PathVariable String docId, @RequestBody Map<String, Integer> params) {
        KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
        if (doc != null && params.get("status") != null) {
            doc.setStatus(params.get("status"));
            return R.ok(knowledgeDocumentService.updateById(doc));
        }
        return R.fail("文档不存在");
    }

    @PostMapping("/documents/embed/batch")
    @Operation(summary = "批量向量化", description = "批量处理多个文档的向量化")
    public R<Map<String, Object>> embedDocuments(@RequestBody List<String> docIds) {
        log.info("接收到批量文档向量化请求，共 {} 个文档", docIds.size());
        return R.ok(documentEmbeddingService.embedDocuments(docIds));
    }


    // ==================== 向量检索 ====================


    // ==================== 文档与标签关联 ====================

    @PutMapping("/documents/{docId}/tags")
    @Operation(summary = "设置文档标签", description = "为文档设置标签关联")
    public R<Boolean> setDocTags(@PathVariable String docId, @RequestBody List<String> tagIds) {
        knowledgeTagService.setDocTags(docId, tagIds);
        return R.ok(true);
    }

    // ==================== 私有辅助方法 ====================



    /**
     * 从文件中提取文本
     */
    private String extractTextFromFile(String filePath, String fileName) throws Exception {
        if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            return new String(java.nio.file.Files.readAllBytes(Paths.get(filePath)), "UTF-8");
        } else if (fileName.endsWith(".docx")) {
            return extractTextFromDocx(filePath);
        } else if (fileName.endsWith(".doc")) {
            return "DOC 格式暂不支持解析，请转换为 DOCX 格式";
        } else if (fileName.endsWith(".pdf")) {
            return "PDF 文件暂不支持解析内容预览";
        }
        return null;
    }

    /**
     * 从 DOCX 文件中提取文本
     */
    private String extractTextFromDocx(String filePath) throws Exception {
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(filePath)) {
            java.util.zip.ZipEntry entry = zipFile.getEntry("word/document.xml");
            if (entry == null) return "";

            try (java.io.InputStream is = zipFile.getInputStream(entry)) {
                byte[] bytes = is.readAllBytes();
                String xmlContent = new String(bytes, "UTF-8");

                StringBuilder text = new StringBuilder();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<w:t[^>]*>(.*?)</w:t>", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher matcher = pattern.matcher(xmlContent);
                while (matcher.find()) {
                    String t = matcher.group(1);
                    if (t != null) {
                        t = t.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&quot;", "\"").replaceAll("&#xA;", "\n");
                        text.append(t);
                    }
                }
                String result = text.toString();
                result = result.replaceAll("<[^>]+>", "");
                result = result.replaceAll("\\n\\s*\\n", "\n").trim();
                return result;
            }
        }
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName.endsWith(".pdf")) return "pdf";
        if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) return "word";
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) return "markdown";
        return "text";
    }


}