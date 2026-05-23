package com.weaving.llm.rag.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weaving.llm.common.domain.*;
import com.weaving.llm.common.enums.ChunkingStrategyEnum;
import com.weaving.llm.common.pages.PageDataResult;
import com.weaving.llm.common.pages.PageUtils;
import com.weaving.llm.common.service.LocalFileService;
import com.weaving.llm.common.service.RagChatAIStreamService;
import com.weaving.llm.common.service.WeavingCharService;
import com.weaving.llm.common.utils.converter.ConversionResult;
import com.weaving.llm.common.utils.converter.DocumentConversionService;
import com.weaving.llm.common.utils.JsonUtils;
import com.weaving.llm.rag.domain.dto.DocumentCreateDto;
import com.weaving.llm.rag.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private CustomChunkingStrategyService customChunkingStrategyService;

    @Autowired
    private WeavingCharService weavingCharService;

    @Autowired
    private DocumentConversionService documentConversionService;

    @Autowired
    private RagChatAIStreamService ragChatAIStreamService;

    @Autowired
    @Qualifier("ragChatExecutor")
    private Executor ragChatExecutor;

    // ==================== 文档管理 ====================

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

    @PostMapping("/upload-parse")
    @Operation(summary = "上传文件并解析", description = "上传文件并解析内容用于分片预览")
    public R<Map<String, Object>> uploadAndParse(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return R.fail("文件名不能为空");
            }

            String filePath = localFileService.storeFile(file);
            String content = extractTextFromFile(filePath, fileName);

            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("fileName", fileName);
            result.put("content", content);
            result.put("type", getFileType(fileName));
            return R.ok(result);
        } catch (Exception e) {
            log.error("解析文件失败", e);
            return R.fail("解析文件失败：" + e.getMessage());
        }
    }

    /**
     * 保存文档（包含切片配置）
     */
    @PostMapping("/save")
    @Operation(summary = "保存文档", description = "保存文档并执行分片向量化")
    public R<Map<String, Object>> saveDocumentWithChunking(@RequestBody DocumentCreateDto documentsCreateDto) {
        // 1. 创建基本的 文档
/*
        try {
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            String fileName = (String) params.get("fileName");
            String filePath = (String) params.get("filePath");
            String fileType = (String) params.get("fileType");
            String content = (String) params.get("content");
            String tags = (String) params.get("tags");
            String chunkingStrategy = (String) params.getOrDefault("chunkingStrategy", "intelligent");
            Integer chunkSize = params.get("chunkSize") != null ? Integer.valueOf(params.get("chunkSize").toString()) : 1000;
            Integer overlap = params.get("overlap") != null ? Integer.valueOf(params.get("overlap").toString()) : 50;

            log.info("接收到保存文档请求，knowledgeBaseId={}, 文件名={}, 切片策略={}",
                    knowledgeBaseId, fileName, chunkingStrategy);

            // 保存文档记录
            KnowledgeDocument doc = knowledgeDocumentService.saveDocument(null, knowledgeBaseId, fileName, content, fileType, filePath);
            List<String> fileUrls = new ArrayList<>();
            fileUrls.add(filePath);

            // 处理额外的文件 URL 列表
            for (String fileUrl : fileUrls) {
                try {
                    java.io.File file = new java.io.File(fileUrl);
                    if (!file.exists()) {
                        log.warn("文件不存在：{}", fileUrl);
                        continue;
                    }

                    String extraFileName = file.getName();
                    log.info("处理文件：{}", extraFileName);

                    KnowledgeDocument extraDoc = knowledgeDocumentService.saveDocument(
                            null, knowledgeBaseId, extraFileName, "", "", fileUrl);
                    extraDoc.setStatus(0);
                    extraDoc.setChunkingStrategy(chunkingStrategy);
                    extraDoc.setChunkSize(chunkSize);

                    ConversionResult conversionResult = documentConversionService.convertWithImages(file, extraDoc.getDocId());
                    if (!conversionResult.isSuccess()) {
                        log.error("文件转换失败：{}", conversionResult.getErrorMessage());
                        extraDoc.setStatus(2);
                        knowledgeDocumentService.updateById(extraDoc);
                        continue;
                    }

                    int imageCount = conversionResult.getImages() != null ? conversionResult.getImages().size() : 0;
                    String convertedContent = conversionResult.getContent();
                    String convertedFileType = conversionResult.getOriginalFileType();

                    extraDoc.setContent(convertedContent);
                    extraDoc.setType(convertedFileType);
                    knowledgeDocumentService.updateById(extraDoc);

                    log.info("文件转换成功，类型：{}, 内容长度：{}, 图片数：{}", convertedFileType,
                            convertedContent != null ? convertedContent.length() : 0, imageCount);

                    if (imageCount > 0) {
                        documentConversionService.saveExtractedImages(conversionResult, extraDoc.getDocId());
                        extraDoc.setTags((extraDoc.getTags() != null ? extraDoc.getTags() + "," : "") + "imageCount:" + imageCount);
                        log.info("提取并保存了 {} 张图片", imageCount);
                        knowledgeDocumentService.updateById(extraDoc);
                    }

                    if (convertedContent != null && !convertedContent.trim().isEmpty()) {
                        int extraChunkCount = performChunking(
                                extraDoc.getDocId(), convertedContent,
                                chunkingStrategy, chunkSize, overlap,
                                params, extraDoc, result);
                        log.info("文件 {} 分片完成，共 {} 个切片", extraFileName, extraChunkCount);

                        extraDoc.setStatus(1);
                        extraDoc.setChunkCount(extraChunkCount);
                        knowledgeDocumentService.updateById(extraDoc);

                        if (knowledgeBaseId != null) {
                            knowledgeBaseService.incrementDocCount(knowledgeBaseId);
                        }
                    }
                } catch (Exception e) {
                    log.error("处理文件失败：{}", fileUrl, e);
                }
            }

            doc.setStatus(0);
            if (tags != null && !tags.trim().isEmpty()) {
                doc.setTags(tags);
            }
            doc.setChunkingStrategy(chunkingStrategy);
            doc.setChunkSize(chunkSize);
            knowledgeDocumentService.updateById(doc);

            int chunkCount = 0;
            if (content != null && !content.trim().isEmpty() &&
                    !content.startsWith("PDF") && !content.startsWith("DOC")) {
                chunkCount = performChunking(doc.getDocId(), content, chunkingStrategy, chunkSize, overlap, params, doc, result);
                log.info("文档分片完成，共 {} 个切片", chunkCount);
            }

            doc.setStatus(1);
            doc.setChunkCount(chunkCount);
            knowledgeDocumentService.updateById(doc);

            if (knowledgeBaseId != null) {
                knowledgeBaseService.incrementDocCount(knowledgeBaseId);
            }

            result.put("docId", doc.getDocId());
            result.put("chunkCount", chunkCount);
            result.put("doc", doc);
            return R.ok(result);
        } catch (Exception e) {
            log.error("保存文档失败", e);
            return R.fail("保存失败：" + e.getMessage());
        }*/
        return R.ok();
    }

    /**
     * 上传文件并带切片
     */
    @PostMapping("/documents/upload-with-chunks")
    @Operation(summary = "上传文件并带切片", description = "上传文件并选择性地保存切片")
    public R<Map<String, Object>> uploadWithChunks(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam String strategy,
            @RequestParam(required = false, defaultValue = "500") Integer chunkSize,
            @RequestParam(required = false, defaultValue = "50") Integer overlap,
            @RequestParam(required = false) List<String> chunkIndexes,
            @RequestParam(required = false) String tags) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return R.fail("文件名不能为空");
            }

            String content;
            String type;
            String source = null;

            if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
                type = fileName.endsWith(".md") || fileName.endsWith(".markdown") ? "markdown" : "text";
                content = new String(file.getBytes(), "UTF-8");
            } else {
                type = fileName.endsWith(".pdf") ? "pdf" : (fileName.endsWith(".doc") || fileName.endsWith(".docx") ? "word" : "file");
                source = localFileService.storeFile(file);
                content = null;
            }

            List<Map<String, Object>> allChunks = textChunkingService.chunkWithMeta(content, strategy, chunkSize, overlap);

            List<Map<String, Object>> selectedChunks = new ArrayList<>();
            if (chunkIndexes != null && !chunkIndexes.isEmpty()) {
                for (String idx : chunkIndexes) {
                    idx = idx.trim();
                    idx = idx.replaceAll("[^0-9]", "");
                    if (Integer.parseInt(idx) >= 0 && Integer.parseInt(idx) < allChunks.size()) {
                        selectedChunks.add(allChunks.get(Integer.parseInt(idx)));
                    }
                }
            } else {
                selectedChunks = allChunks;
            }

            KnowledgeDocument doc = knowledgeDocumentService.saveDocument(
                    null, knowledgeBaseId, fileName, content, type, source);
            doc.setStatus(0);
            if (tags != null) doc.setTags(tags);
            knowledgeDocumentService.updateById(doc);

            int savedCount = 0;
            for (Map<String, Object> chunkData : selectedChunks) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .docId(doc.getDocId())
                        .content((String) chunkData.get("content"))
                        .chunkIndex(savedCount)
                        .status(1)
                        .build();
                documentChunkService.save(chunk);
                savedCount++;
            }

            doc.setStatus(1);
            knowledgeDocumentService.updateById(doc);

            if (knowledgeBaseId != null) {
                knowledgeBaseService.incrementDocCount(knowledgeBaseId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("docId", doc.getDocId());
            result.put("savedChunks", savedCount);
            return R.ok(result);
        } catch (Exception e) {
            log.error("上传失败", e);
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    /**
     * 保存文档带切片
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/documents/save-with-chunks")
    @Operation(summary = "保存文档带切片", description = "保存文档及选中的切片")
    public R<Map<String, Object>> saveWithChunks(@RequestBody Map<String, Object> params) {
        try {
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            String title = (String) params.get("title");
            String content = (String) params.get("content");
            String source = (String) params.get("source");
            String type = (String) params.get("type");
            List<String> chunks = (List<String>) params.get("chunks");
            String tags = (String) params.get("tags");

            KnowledgeDocument doc = knowledgeDocumentService.saveDocument(
                    null, knowledgeBaseId, title, content, type, source);
            doc.setStatus(0);
            if (tags != null) doc.setTags(tags);
            knowledgeDocumentService.updateById(doc);

            int savedCount = 0;
            if (chunks != null && !chunks.isEmpty()) {
                for (int i = 0; i < chunks.size(); i++) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .docId(doc.getDocId())
                            .content(chunks.get(i))
                            .chunkIndex(i)
                            .status(1)
                            .build();
                    documentChunkService.save(chunk);
                    savedCount++;
                }
            }

            doc.setStatus(1);
            knowledgeDocumentService.updateById(doc);

            if (knowledgeBaseId != null) {
                knowledgeBaseService.incrementDocCount(knowledgeBaseId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("docId", doc.getDocId());
            result.put("savedChunks", savedCount);
            return R.ok(result);
        } catch (Exception e) {
            log.error("保存失败", e);
            return R.fail("保存失败：" + e.getMessage());
        }
    }

    /**
     * 重新分片文档
     */
    @PostMapping("/documents/{docId}/rechunk")
    @Operation(summary = "重新分片文档", description = "使用新的配置重新分片文档")
    public R<Map<String, Object>> rechunkDocument(@PathVariable String docId, @RequestBody Map<String, Object> params) {
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

            String chunkingStrategy = (String) params.getOrDefault("chunkingStrategy", "intelligent");
            Integer chunkSize = params.get("chunkSize") != null ? Integer.valueOf(params.get("chunkSize").toString()) : 1000;
            Integer overlap = params.get("overlap") != null ? Integer.valueOf(params.get("overlap").toString()) : 50;

            documentChunkService.deleteChunksByDocId(docId);
            log.info("已删除旧切片，docId={}", docId);

            Map<String, Object> chunkResult = new HashMap<>();
            int chunkCount = performChunking(docId, content, chunkingStrategy, chunkSize, overlap, params, doc, chunkResult);
            log.info("重新分片完成，共 {} 个切片", chunkCount);

            doc.setChunkingStrategy(chunkingStrategy);
            doc.setChunkSize(chunkSize);
            doc.setChunkCount(chunkCount);
            doc.setStatus(1);
            knowledgeDocumentService.updateById(doc);

            Map<String, Object> result = new HashMap<>();
            result.put("chunkCount", chunkCount);
            return R.ok(result);
        } catch (Exception e) {
            log.error("重新分片失败，docId={}", docId, e);
            return R.fail("重新分片失败：" + e.getMessage());
        }
    }

    // ==================== 向量化管理 ====================

    /**
     * 异步批量处理文档向量化
     */
    @PostMapping("/documents/embed/async")
    @Operation(summary = "异步批量向量化", description = "异步批量处理文档向量化")
    public R<Map<String, Object>> embedDocumentsAsync(@RequestBody List<String> docIds) {
        log.info("接收到异步批量文档向量化请求，共 {} 个文档", docIds.size());

        List<KnowledgeDocument> documents = new ArrayList<>();
        for (String docId : docIds) {
            KnowledgeDocument doc = knowledgeDocumentService.getById(docId);
            if (doc != null) {
                documents.add(doc);
            } else {
                log.warn("文档 {} 不存在，跳过", docId);
            }
        }

        try {
        // 注意：异步方法需要在实现类中提供，当前直接使用同步方法
            Map<String, Object> result = vectorStoreService.embedDocuments(documents);

//            Map<String, Object> result = new HashMap<>();
            result.put("totalDocs", documents.size());
            result.put("status", "processing");
            result.put("taskId", String.valueOf(System.currentTimeMillis()));

            return R.ok(result);
        } catch (Exception e) {
            return R.fail("异步处理失败：" + e.getMessage());
        }
    }

    /**
     * 对指定文档进行分割和向量化
     */
    @PostMapping("/documents/{docId}/embed")
    @Operation(summary = "文档向量化", description = "对指定文档进行分割和向量化")
    public R<Map<String, Object>> embedDocument(@PathVariable String docId) {
        log.info("接收到文档向量化请求，文档 ID: {}", docId);
        return R.ok(documentEmbeddingService.embedDocument(docId));
    }

    /**
     * 批量处理多个文档的向量化
     */
    @PostMapping("/documents/embed/batch")
    @Operation(summary = "批量向量化", description = "批量处理多个文档的向量化")
    public R<Map<String, Object>> embedDocuments(@RequestBody List<String> docIds) {
        log.info("接收到批量文档向量化请求，共 {} 个文档", docIds.size());
        return R.ok(documentEmbeddingService.embedDocuments(docIds));
    }

    /**
     * 配置文档分块参数
     */
    @PostMapping("/config/splitter")
    @Operation(summary = "配置分块参数", description = "配置文档分块参数")
    public R<Map<String, Object>> configureSplitter(
            @RequestParam int segmentSize,
            @RequestParam int overlap) {
        documentEmbeddingService.configureSplitter(segmentSize, overlap);

        Map<String, Object> result = new HashMap<>();
        result.put("segmentSize", segmentSize);
        result.put("overlap", overlap);

        return R.ok(result);
    }

    /**
     * 批量向量化文档
     */
    @PostMapping("/documents/{docId}/vectorize")
    @Operation(summary = "向量化文档", description = "批量向量化单个文档")
    public R<Map<String, Object>> vectorizeDocument(@PathVariable String docId) {
        try {
            log.info("开始向量化文档：{}", docId);

            Map<String, Object> embedResult = documentEmbeddingService.embedDocument(docId);

            return R.ok(embedResult);

        } catch (Exception e) {
            log.error("文档向量化失败：{}", docId, e);
            return R.fail("向量化失败：" + e.getMessage());
        }
    }

    /**
     * 批量向量化知识库中的所有文档
     */
    @PostMapping("/bases/{baseId}/vectorize-all")
    @Operation(summary = "向量化知识库所有文档", description = "批量向量化知识库中的所有文档")
    public R<Map<String, Object>> vectorizeAllDocuments(@PathVariable String baseId) {
        try {
            log.info("开始向量化知识库所有文档：{}", baseId);

            List<KnowledgeDocument> documents = knowledgeDocumentService.getDocumentsByKnowledgeBaseId(baseId);

            if (documents.isEmpty()) {
                return R.fail("知识库中没有文档");
            }

            int successCount = 0;
            int failCount = 0;
            int totalChunks = 0;

            for (KnowledgeDocument doc : documents) {
                try {
                    Map<String, Object> embedResult = documentEmbeddingService.embedDocument(doc.getDocId());
                    if ((Boolean) embedResult.get("success")) {
                        successCount++;
                        totalChunks += (Integer) embedResult.get("chunkCount");
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("向量化文档失败：{}", doc.getDocId(), e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalDocs", documents.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalChunks", totalChunks);

            return R.ok(result);

        } catch (Exception e) {
            log.error("批量向量化失败：{}", baseId, e);
            return R.fail("批量向量化失败：" + e.getMessage());
        }
    }

    // ==================== 向量检索 ====================

    /**
     * 向量检索相关文档片段 (支持过滤、分页)
     */
    @GetMapping("/search")
    @Operation(summary = "向量检索", description = "根据查询向量的量检索相关文档片段")
    public R<Map<String, Object>> search(
            @Parameter(description = "查询文本", required = true) @RequestParam String query,
            @Parameter(description = "最大结果数", example = "5") @RequestParam(defaultValue = "5") int maxResults,
            @Parameter(description = "用户 ID（可选）") @RequestParam(required = false) Long userId,
            @Parameter(description = "知识库 ID（可选）") @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page) {

        log.info("搜索请求：query={}, maxResults={}, userId={}, knowledgeBaseId={}, page={}",
                query, maxResults, userId, knowledgeBaseId, page);

        List<Map<String, Object>> results = documentEmbeddingService.searchRelevantSegments(
                query, maxResults, userId, knowledgeBaseId, page);

        Map<String, Object> response = new HashMap<>();
        response.put("list", results);
        response.put("total", results.size());
        response.put("page", page);
        response.put("pageSize", maxResults);
        response.put("hasMore", results.size() == maxResults);

        return R.ok(response);
    }

    /**
     * 混合检索 (关键词 + 向量)
     */
    @GetMapping("/search/hybrid")
    @Operation(summary = "混合检索", description = "关键词 + 向量混合检索")
    public R<Map<String, Object>> hybridSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int vectorTopK,
            @RequestParam(defaultValue = "10") int keywordTopK,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam(defaultValue = "1") int page) {

        log.info("混合检索：query={}, vectorTopK={}, keywordTopK={}", query, vectorTopK, keywordTopK);

        List<Map<String, Object>> vectorResults = documentEmbeddingService.searchRelevantSegments(
                query, vectorTopK, userId, knowledgeBaseId, page);

        Map<String, Object> response = new HashMap<>();
        response.put("vectorResults", vectorResults);
        response.put("keywordResults", Collections.emptyList());
        response.put("mergedResults", vectorResults);
        response.put("page", page);
        response.put("pageSize", vectorTopK);

        return R.ok(response);
    }

    /**
     * 向量检索
     */
    @PostMapping("/search")
    @Operation(summary = "向量检索", description = "根据查询文本检索相关文档片段")
    public R<Map<String, Object>> search(@RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            Integer maxResults = params.get("maxResults") != null ? Integer.valueOf(params.get("maxResults").toString()) : 10;

            if (query == null || query.trim().isEmpty()) {
                return R.fail("查询内容不能为空");
            }

            log.info("接收到检索请求，query={}, knowledgeBaseId={}, maxResults={}", query, knowledgeBaseId, maxResults);

            List<Map<String, Object>> searchResults = documentEmbeddingService.searchRelevantSegments(
                    query, maxResults, userId, knowledgeBaseId, 1);

            Map<String, Object> result = new HashMap<>();
            result.put("results", searchResults);
            result.put("total", searchResults.size());

            return R.ok(result);

        } catch (Exception e) {
            log.error("检索失败", e);
            return R.fail("检索失败：" + e.getMessage());
        }
    }

    /**
     * RAG 知识库问答 - 流式响应（SSE + TokenStream Flex API）
     */
    @GetMapping(value = "/rag/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG 流式问答", description = "基于知识库的流式问答接口（SSE）")
    public SseEmitter ragChatStream(
            @Parameter(description = "用户问题", required = true) @RequestParam String query,
            @Parameter(description = "知识库 ID（可选）") @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "最大检索结果数", example = "5") @RequestParam(defaultValue = "5") Integer maxResults) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (query == null || query.trim().isEmpty()) {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"success\":false,\"message\":\"问题不能为空\"}"));
                    emitter.complete();
                    return;
                }
                log.info("RAG 流式问答请求：query={}, knowledgeBaseId={}", query, knowledgeBaseId);

                List<Map<String, Object>> searchResults = documentEmbeddingService.searchRelevantSegments(
                        query, maxResults, null, knowledgeBaseId, 1);
                emitter.send(SseEmitter.event()
                        .name("search_results")
                        .data(searchResults));

                List<String> searchContents = searchResults.stream()
                        .map(r -> (String) r.get("content"))
                        .filter(c -> c != null && !c.trim().isEmpty())
                        .toList();

                emitter.send(SseEmitter.event().name("start").data("{\"type\":\"start\"}"));

                String userMessage = buildRagUserMessage(query, searchContents);

                ragChatAIStreamService.chat(userMessage)
                        .onPartialResponse(token -> {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (IOException e) {
                                log.error("发送流式 token 失败", e);
                            }
                        })
                        .onCompleteResponse(response -> {
                            try {
                                String fullResponse = response.aiMessage().text();
                                log.info("RAG 流式对话完成，回答长度：{}", fullResponse.length());
                                emitter.send(SseEmitter.event().name("complete").data(fullResponse));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("发送完成事件失败", e);
                            }
                        })
                        .onError(error -> {
                            log.error("RAG 流式对话出错", error);
                            try {
                                emitter.send(SseEmitter.event().name("error")
                                        .data("{\"error\":\"" + error.getMessage() + "\"}"));
                            } catch (IOException e) {
                                log.error("发送错误事件失败", e);
                            }
                            emitter.complete();
                        })
                        .start();

            } catch (Exception e) {
                log.error("RAG 流式问答失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送失败", ex);
                }
            }
        }, ragChatExecutor);

        emitter.onCompletion(() -> log.info("RAG 流式问答完成"));
        emitter.onTimeout(() -> {
            log.error("RAG 流式问答超时");
            emitter.complete();
        });
        emitter.onError(e -> log.error("RAG 流式问答错误", e));
        return emitter;
    }

    /**
     * RAG 知识库问答 - 普通响应（保留兼容）
     */
    @PostMapping("/rag/chat")
    @Operation(summary = "RAG 问答", description = "基于知识库的问答接口")
    public R<Map<String, Object>> ragChat(@RequestBody Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            String knowledgeBaseId = (String) params.get("knowledgeBaseId");
            Integer maxResults = params.get("maxResults") != null ? Integer.valueOf(params.get("maxResults").toString()) : 10;

            if (query == null || query.trim().isEmpty()) {
                return R.fail("问题不能为空");
            }

            log.info("RAG 问答请求：query={}, knowledgeBaseId={}", query, knowledgeBaseId);

            List<Map<String, Object>> searchResults = documentEmbeddingService.searchRelevantSegments(
                    query, maxResults, null, knowledgeBaseId, 1);

            log.info("检索到 {} 条相关信息", searchResults.size());

            List<String> searchContents = searchResults.stream()
                    .map(r -> (String) r.get("content"))
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            String answer;
            boolean hasKnowledge;
            if (searchContents.isEmpty()) {
                log.info("未检索到相关信息，直接调用大模型");
                answer = weavingCharService.chat(query);
                hasKnowledge = false;
            } else {
                log.info("使用 RAG 模式生成回答");
                answer = weavingCharService.ragChat(query, searchContents);
                hasKnowledge = true;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("sources", searchResults);
            result.put("sourceCount", searchResults.size());
            result.put("hasKnowledge", hasKnowledge);

            return R.ok(result);

        } catch (Exception e) {
            log.error("RAG 问答失败", e);
            return R.fail("RAG 问答失败：" + e.getMessage());
        }
    }

    // ==================== 文档与标签关联 ====================

    @PutMapping("/documents/{docId}/tags")
    @Operation(summary = "设置文档标签", description = "为文档设置标签关联")
    public R<Boolean> setDocTags(@PathVariable String docId, @RequestBody List<String> tagIds) {
        knowledgeTagService.setDocTags(docId, tagIds);
        return R.ok(true);
    }

    // ==================== 切片管理 ====================

    @GetMapping("/documents/{docId}/chunks")
    @Operation(summary = "获取文档切片", description = "获取文档的所有切片")
    public R<List<DocumentChunk>> getChunks(@PathVariable String docId) {
        return R.ok(documentChunkService.getChunksByDocId(docId));
    }

    @GetMapping("/documents/{docId}/chunks/page")
    @Operation(summary = "获取文档切片列表", description = "通过文档 ID 获取切片列表（分页）")
    public R<PageDataResult> getChunksPaged(@PathVariable String docId) {
        PageUtils.startPage();
        List<DocumentChunk> chunks = documentChunkService.pageListByDocId(docId);
        return R.ok(PageUtils.generatePageDataResult(chunks));
    }

    @PostMapping("/documents/{docId}/chunk")
    @Operation(summary = "添加切片", description = "为文档添加单个切片")
    public R<DocumentChunk> addChunk(@PathVariable String docId, @RequestBody Map<String, String> params) {
        String content = params.get("content");
        DocumentChunk chunk = DocumentChunk.builder()
                .docId(docId)
                .content(content)
                .chunkIndex(documentChunkService.getChunksByDocId(docId).size())
                .status(1)
                .build();
        documentChunkService.save(chunk);
        return R.ok(chunk);
    }

    @DeleteMapping("/documents/{docId}/chunks")
    @Operation(summary = "删除文档切片", description = "删除文档的所有切片")
    public R<Boolean> deleteChunks(@PathVariable String docId) {
        documentChunkService.deleteChunksByDocId(docId);
        return R.ok(true);
    }

    /**
     * 对单个切片进行向量化
     */
    @PostMapping("/chunks/{chunkId}/vectorize")
    @Operation(summary = "切片向量化", description = "对单个切片进行向量化")
    public R<Map<String, Object>> vectorizeChunk(@PathVariable String chunkId) {
        try {
            DocumentChunk chunk = documentChunkService.getById(chunkId);
            if (chunk == null) {
                return R.fail("切片不存在");
            }

            log.info("开始向量化切片：{}", chunkId);

            KnowledgeDocument doc = knowledgeDocumentService.getById(chunk.getDocId());
            if (doc == null) {
                return R.fail("文档不存在");
            }

            List<String> texts = Arrays.asList(chunk.getContent());
            List<String> vectorIds = vectorStoreService.addVectors(
                    texts,
                    chunk.getDocId(),
                    doc.getKnowledgeBaseId() != null ? doc.getKnowledgeBaseId() : "default",
                    doc.getUserId() != null ? doc.getUserId() : 0L,
                    doc.getTitle() != null ? doc.getTitle() : ""
            );

            String vectorId = vectorIds.get(0);
            chunk.setVectorId(vectorId);
            documentChunkService.updateById(chunk);

            Map<String, Object> result = new HashMap<>();
            result.put("vectorId", vectorId);

            log.info("切片向量化完成：{}", chunkId);

            return R.ok(result);

        } catch (Exception e) {
            log.error("切片向量化失败：{}", chunkId, e);
            return R.fail("向量化失败：" + e.getMessage());
        }
    }

    // ==================== 分片策略管理 ====================

    @GetMapping("/chunking/strategies")
    @Operation(summary = "获取内置分片策略", description = "获取所有内置的分片策略")
    public R<Map<String, Object>> getChunkingStrategies() {
        return R.ok(textChunkingService.getChunkingStrategies());
    }

    @GetMapping("/chunking/custom-strategies")
    @Operation(summary = "获取自定义分片策略", description = "获取自定义分片策略列表")
    public R<List<CustomChunkingStrategy>> getCustomStrategies(@RequestParam(required = false) Long userId) {
        List<CustomChunkingStrategy> strategies;
        if (userId != null) {
            strategies = customChunkingStrategyService.getByUserId(userId);
        } else {
            strategies = customChunkingStrategyService.list();
        }
        return R.ok(strategies);
    }

    @GetMapping("/chunking/custom-strategies/{id}")
    @Operation(summary = "获取自定义分片策略详情", description = "根据 ID 获取自定义分片策略")
    public R<CustomChunkingStrategy> getCustomStrategy(@PathVariable String id) {
        return R.ok(customChunkingStrategyService.getById(id));
    }

    @PostMapping("/chunking/custom-strategies")
    @Operation(summary = "创建自定义分片策略", description = "创建新的自定义分片策略")
    public R<CustomChunkingStrategy> createCustomStrategy(@RequestBody CustomChunkingStrategy strategy) {
        return R.ok(customChunkingStrategyService.createStrategy(strategy));
    }

    @PutMapping("/chunking/custom-strategies/{id}")
    @Operation(summary = "更新自定义分片策略", description = "更新自定义分片策略")
    public R<CustomChunkingStrategy> updateCustomStrategy(@PathVariable String id, @RequestBody CustomChunkingStrategy strategy) {
        return R.ok(customChunkingStrategyService.updateStrategy(id, strategy));
    }

    @DeleteMapping("/chunking/custom-strategies/{id}")
    @Operation(summary = "删除自定义分片策略", description = "删除自定义分片策略")
    public R<Boolean> deleteCustomStrategy(@PathVariable String id) {
        return R.ok(customChunkingStrategyService.deleteStrategy(id));
    }

    @GetMapping("/chunking/all-strategies")
    @Operation(summary = "获取所有分片策略", description = "获取内置和自定义分片策略")
    public R<Map<String, Object>> getAllStrategies(@RequestParam(required = false) Long userId) {
        Map<String, Object> builtIn = textChunkingService.getChunkingStrategies();
        List<CustomChunkingStrategy> custom = userId != null
                ? customChunkingStrategyService.getByUserId(userId)
                : customChunkingStrategyService.list();

        Map<String, Object> result = new HashMap<>();
        result.put("builtIn", builtIn.get("list"));
        result.put("custom", custom);
        return R.ok(result);
    }

    @PostMapping("/chunking/preview")
    @Operation(summary = "分片预览", description = "预览文本分片效果")
    public R<Map<String, Object>> previewChunking(@RequestBody Map<String, Object> params) {
        try {
            String content = (String) params.get("content");
            String strategy = (String) params.getOrDefault("strategy", "char");
            int chunkSize = params.get("chunkSize") != null ? ((Number) params.get("chunkSize")).intValue() : 500;
            int overlap = params.get("overlap") != null ? ((Number) params.get("overlap")).intValue() : 50;
            String separators = (String) params.getOrDefault("separators", "\n|,|;|。");

            if (strategy.startsWith("custom_")) {
                String strategyId = strategy.replace("custom_", "");
                CustomChunkingStrategy customStrategy = customChunkingStrategyService.getById(strategyId);
                if (customStrategy != null) {
                    Map<String, Object> customParams = new HashMap<>();
                    customParams.put("chunkSize", customStrategy.getChunkSize() != null ? customStrategy.getChunkSize() : 500);
                    customParams.put("overlap", customStrategy.getOverlap() != null ? customStrategy.getOverlap() : 50);
                    if (customStrategy.getSeparators() != null) {
                        customParams.put("separators", customStrategy.getSeparators().replace(",", "|"));
                    }
                    if (customStrategy.getMaxChars() != null) {
                        customParams.put("maxChars", customStrategy.getMaxChars());
                    }
                    List<Map<String, Object>> chunks = textChunkingService.chunkCustom(
                            content, customStrategy.getType(), customParams);

                    Map<String, Object> result = new HashMap<>();
                    result.put("chunks", chunks);
                    result.put("total", chunks.size());
                    result.put("strategy", "custom:" + customStrategy.getName());
                    return R.ok(result);
                } else {
                    return R.fail("自定义策略不存在");
                }
            } else {
                List<Map<String, Object>> chunks = textChunkingService.chunkWithMeta(content, strategy, chunkSize, overlap);
                Map<String, Object> result = new HashMap<>();
                result.put("chunks", chunks);
                result.put("total", chunks.size());
                result.put("strategy", strategy);
                return R.ok(result);
            }
        } catch (Exception e) {
            log.error("分片预览失败", e);
            return R.fail(e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 执行文档分片
     */
    private int performChunking(String docId, String content, String strategy, int chunkSize, int overlap,
                                 Map<String, Object> params, KnowledgeDocument doc, Map<String, Object> result) {
        try {
            String actualStrategy = mapChunkingStrategy(strategy);

            List<Map<String, Object>> chunks;
            if ("paragraph".equals(actualStrategy)) {
                chunks = textChunkingService.chunkWithMeta(content, "paragraph", chunkSize, 0);
            } else if ("sentence".equals(actualStrategy)) {
                chunks = textChunkingService.chunkWithMeta(content, "sentence", chunkSize, 0);
            } else if ("line".equals(actualStrategy)) {
                chunks = textChunkingService.chunkWithMeta(content, "line", chunkSize, 0);
            } else if ("separator".equals(actualStrategy)) {
                chunks = textChunkingService.chunkWithMeta(content, "separator", chunkSize, 0);
            } else {
                chunks = textChunkingService.chunkWithMeta(content, "char", chunkSize, overlap);
            }

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

            Boolean enableEmbedding = params.get("enableEmbedding") != null ? (Boolean) params.get("enableEmbedding") : true;
            if (enableEmbedding) {
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
                        log.info("向量化完成，docId={}, 向量数={}", docId, vectorIds.size());

                        for (int i = 0; i < savedChunks.size() && i < vectorIds.size(); i++) {
                            DocumentChunk chunk = savedChunks.get(i);
                            chunk.setVectorId(vectorIds.get(i));
                            documentChunkService.updateById(chunk);
                        }
                    }
                } catch (Exception e) {
                    log.error("向量化失败，docId={}", docId, e);
                    result.put("embeddingError", "向量化失败：" + e.getMessage());
                }
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

    /**
     * 构建 RAG 用户消息（包含检索上下文）
     */
    private String buildRagUserMessage(String question, List<String> searchContents) {
        StringBuilder sb = new StringBuilder();

        if (!searchContents.isEmpty()) {
            sb.append("【检索到的相关信息】\n");
            for (int i = 0; i < searchContents.size(); i++) {
                sb.append(String.format("[%d] %s\n", i + 1, searchContents.get(i)));
            }
            sb.append("\n");
        }

        sb.append("【用户问题】\n");
        sb.append(question);
        sb.append("\n\n请根据以上信息回答用户的问题：");

        return sb.toString();
    }
}