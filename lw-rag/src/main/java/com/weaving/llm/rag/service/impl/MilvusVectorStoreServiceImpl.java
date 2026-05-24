package com.weaving.llm.rag.service.impl;

import com.weaving.llm.common.domain.DocumentChunk;
import com.weaving.llm.rag.service.DocumentChunkService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.ConnectParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Milvus 向量存储服务实现
 * 使用 Milvus Java SDK 连接 Milvus 服务
 *
 * @Author: 依梦
 * @Date: 2025/10/27
 */
@Slf4j
@Service("milvusVectorStoreService")
@Profile("milvus")
public class MilvusVectorStoreServiceImpl implements com.weaving.llm.rag.service.VectorStoreService {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private Integer port;

    @Value("${milvus.collection:knowledge_chunks}")
    private String collectionName;

    @Value("${milvus.dimension:1024}")
    private Integer dimension;

    private MilvusServiceClient milvusClient;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private EmbeddingModel embeddingModel;

    private static final String ID_FIELD = "id";
    private static final String VECTOR_FIELD = "vector";
    private static final String CHUNK_ID_FIELD = "chunk_id";
    private static final String DOC_ID_FIELD = "doc_id";
    private static final String CONTENT_FIELD = "content";
    private static final String CHUNK_INDEX_FIELD = "chunk_index";

    @PostConstruct
    public void init() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            milvusClient = new MilvusServiceClient(connectParam);
            log.info("Milvus 客户端初始化成功，host={}, port={}", host, port);

            ensureCollection();
        } catch (Exception e) {
            log.error("Milvus 客户端初始化失败：{}", e.getMessage(), e);
            milvusClient = null;
        }
    }

    private void ensureCollection() {
        try {
            boolean hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()).getData();
            if (!hasCollection) {
                createCollection();
            } else {
                milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build());
            }
            log.info("Collection '{}' 已就绪", collectionName);
        } catch (Exception e) {
            log.error("检查/创建 collection 失败：{}", e.getMessage(), e);
        }
    }

    private void createCollection() {
        try {
            FieldType idField = FieldType.newBuilder()
                    .withName(ID_FIELD)
                    .withDataType(io.milvus.grpc.DataType.Int64)
                    .withPrimaryKey(true)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName(VECTOR_FIELD)
                    .withDataType(io.milvus.grpc.DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            FieldType chunkIdField = FieldType.newBuilder()
                    .withName(CHUNK_ID_FIELD)
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(256)
                    .build();

            FieldType docIdField = FieldType.newBuilder()
                    .withName(DOC_ID_FIELD)
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(256)
                    .build();

            FieldType contentField = FieldType.newBuilder()
                    .withName(CONTENT_FIELD)
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType chunkIndexField = FieldType.newBuilder()
                    .withName(CHUNK_INDEX_FIELD)
                    .withDataType(io.milvus.grpc.DataType.Int64)
                    .build();

            List<FieldType> fieldList = Arrays.asList(idField, vectorField, chunkIdField, docIdField, contentField, chunkIndexField);

            milvusClient.createCollection(io.milvus.param.collection.CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldTypes(fieldList)
                    .build());

            milvusClient.createIndex(io.milvus.param.index.CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(VECTOR_FIELD)
//                    .withIndexTypeName("IVF_FLAT")
//                    .withMetricTypeName("L2")
                    .build());

            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());

            log.info("Collection '{}' 创建成功", collectionName);
        } catch (Exception e) {
            log.error("创建 collection 失败：{}", e.getMessage(), e);
            throw new RuntimeException("创建 Milvus collection 失败", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return milvusClient != null;
    }

    @Override
    public void configureSplitter(int segmentSizeInTokens, int maxOverlapInTokens) {
    }

    @Override
    public int getVectorDimension() {
        return dimension;
    }

    @Override
    public void embedDocumentChunks(List<DocumentChunk> chunks) {
        if (!isAvailable()) {
            log.warn("Milvus 向量存储服务不可用，跳过切片向量化");
            return;
        }

        if (chunks == null || chunks.isEmpty()) {
            log.warn("切片列表为空，跳过向量化");
            return;
        }

        try {
            log.info("开始向量化切片，共 {} 个切片", chunks.size());

            // 1. 生成向量
            List<TextSegment> segments = new ArrayList<>();
            for (DocumentChunk chunk : chunks) {
                segments.add(TextSegment.from(chunk.getContent()));
            }
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            // 2. 准备插入数据
            List<Long> ids = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();
            List<String> chunkIds = new ArrayList<>();
            List<String> docIds = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<Long> chunkIndexes = new ArrayList<>();

            long baseId = System.currentTimeMillis();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                ids.add(baseId + i);

                float[] vec = embeddings.get(i).vector();
                List<Float> vectorList = new ArrayList<>();
                for (float v : vec) {
                    vectorList.add(v);
                }
                vectors.add(vectorList);

                chunkIds.add(chunk.getId() != null ? chunk.getId() : (chunk.getDocId() + "_" + chunk.getChunkIndex()));
                docIds.add(chunk.getDocId());
                contents.add(chunk.getContent());
                chunkIndexes.add((long) chunk.getChunkIndex());
            }

            // 3. 构建 InsertParam.Field 列表
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field(ID_FIELD, ids),
                    new InsertParam.Field(VECTOR_FIELD, vectors),
                    new InsertParam.Field(CHUNK_ID_FIELD, chunkIds),
                    new InsertParam.Field(DOC_ID_FIELD, docIds),
                    new InsertParam.Field(CONTENT_FIELD, contents),
                    new InsertParam.Field(CHUNK_INDEX_FIELD, chunkIndexes)
            );

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            milvusClient.insert(insertParam);
            log.info("Milvus 插入完成，数量={}", ids.size());

            // 4. 更新切片的 vectorId
            List<String> vectorIds = new ArrayList<>();
            for (Long id : ids) {
                vectorIds.add(String.valueOf(id));
            }
            updateEmbedResult(chunks, vectorIds);

            log.info("切片向量化完成，共 {} 个", ids.size());

        } catch (Exception e) {
            log.error("切片向量化失败：{}", e.getMessage(), e);
            throw new RuntimeException("切片向量化失败：" + e.getMessage(), e);
        }
    }

    private void updateEmbedResult(List<DocumentChunk> chunks, List<String> vectorIds) {
        try {
            for (int i = 0; i < chunks.size() && i < vectorIds.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                chunk.setVectorId(vectorIds.get(i));
                documentChunkService.updateById(chunk);
                log.debug("更新切片 vectorId: chunkId={}, vectorId={}", chunk.getId(), vectorIds.get(i));
            }
            log.info("切片 vectorId 更新完成，共 {} 个", vectorIds.size());
        } catch (Exception e) {
            log.error("更新切片 vectorId 失败：{}", e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> embedDocument(com.weaving.llm.common.domain.KnowledgeDocument doc) {
        return null;
    }

    @Override
    public Map<String, Object> embedDocuments(List<com.weaving.llm.common.domain.KnowledgeDocument> documents) {
        return null;
    }

    @Override
    public List<String> addVectors(List<String> texts, String docId, String knowledgeBaseId, Long userId, String title) {
        return null;
    }

    @Override
    public List<String> addVectors(List<String> texts, String docId) {
        return null;
    }

    @Override
    public void deleteVectorsByDocId(String docId) {
        if (!isAvailable()) {
            return;
        }
        try {
            milvusClient.delete(DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(DOC_ID_FIELD + " == \"" + docId + "\"")
                    .build());
            log.info("删除 docId={} 的向量成功", docId);
        } catch (Exception e) {
            log.error("删除向量失败：{}", e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> search(String query, int maxResults, Long userId, String knowledgeBaseId, Integer offset) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (!isAvailable()) {
            return results;
        }
        try {
            float[] queryVector = embeddingModel.embed(query).content().vector();
            List<Float> queryVectorList = new ArrayList<>();
            for (float v : queryVector) {
                queryVectorList.add(v);
            }

//            SearchParam searchParam = SearchParam.newBuilder()
//                    .withCollectionName(collectionName)
//                    .withVectorFieldName(VECTOR_FIELD)
//                    .withVectors(Collections.singletonList(queryVectorList))
//                    .withTopK(maxResults)
//                    .withMetricTypeName("L2")
//                    .build();
//
//            io.milvus.grpc.SearchResults searchResult = milvusClient.search(searchParam).getData();
//
//            for (int i = 0; i < searchResult.getRowCount(); i++) {
//                Map<String, Object> item = new HashMap<>();
//                for (io.milvus.grpc.SearchResults.Queries.Field field : searchResult.getFieldsDataList()) {
//                    String fieldName = field.getField();
//                    Object value = null;
//                    if (field.getScalars() != null && field.getScalars().getDataCount() > i) {
//                        value = field.getScalars().getData(i);
//                    }
//                    item.put(fieldName, value);
//                }
//                results.add(item);
//            }

        } catch (Exception e) {
            log.error("Milvus 搜索失败：{}", e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> search(String query, int maxResults) {
        return search(query, maxResults, null, null, null);
    }

    @Override
    public List<Map<String, Object>> searchWithFilter(String query, int maxResults, Long userId, String knowledgeBaseId) {
        return search(query, maxResults, userId, knowledgeBaseId, null);
    }

    @Override
    public List<Map<String, Object>> searchWithPagination(String query, int maxResults, int page) {
        int offset = (page - 1) * maxResults;
        return search(query, maxResults, null, null, offset);
    }

    @Override
    public void clearAll() {
        if (!isAvailable()) {
            return;
        }
        try {
            milvusClient.dropCollection(DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            log.info("清空 Milvus collection: {}", collectionName);
        } catch (Exception e) {
            log.error("清空 collection 失败：{}", e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        if (!isAvailable()) {
            stats.put("available", false);
            return stats;
        }
        try {
            stats.put("available", true);
            stats.put("collection", collectionName);
            stats.put("dimension", dimension);
            stats.put("status", "active");
        } catch (Exception e) {
            stats.put("available", false);
            stats.put("error", e.getMessage());
        }
        return stats;
    }
}