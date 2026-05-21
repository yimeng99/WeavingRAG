package com.weaving.llm.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weaving.llm.common.domain.KnowledgeTag;
import com.weaving.llm.rag.mapper.KnowledgeTagMapper;
import com.weaving.llm.rag.service.KnowledgeTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeTagServiceImpl extends ServiceImpl<KnowledgeTagMapper, KnowledgeTag> implements KnowledgeTagService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<KnowledgeTag> getTagsByBaseId(String knowledgeBaseId) {
        LambdaQueryWrapper<KnowledgeTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeTag::getKnowledgeBaseId, knowledgeBaseId)
               .eq(KnowledgeTag::getDeleted, 0)
               .orderByAsc(KnowledgeTag::getSortOrder);
        return list(wrapper);
    }

    @Override
    @Transactional
    public KnowledgeTag createTag(String knowledgeBaseId, String name, String color) {
        KnowledgeTag tag = KnowledgeTag.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .name(name)
                .color(color != null ? color : "#409eff")
                .sortOrder(0)
                .build();
        save(tag);
        log.info("创建标签: {}", name);
        return tag;
    }

    @Override
    @Transactional
    public boolean updateTag(String id, String name, String color) {
        KnowledgeTag tag = getById(id);
        if (tag == null) return false;
        if (name != null) tag.setName(name);
        if (color != null) tag.setColor(color);
        return updateById(tag);
    }

    @Override
    @Transactional
    public boolean deleteTag(String id) {
        jdbcTemplate.update("DELETE FROM document_tag_relation WHERE tag_id = ?", id);
        return removeById(id);
    }

    @Override
    public List<String> getTagIdsByDocId(String docId) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT tag_id FROM document_tag_relation WHERE doc_id = ?", docId);
        return results.stream()
                .map(r -> (String) r.get("tag_id"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setDocTags(String docId, List<String> tagIds) {
        jdbcTemplate.update("DELETE FROM document_tag_relation WHERE doc_id = ?", docId);
        if (tagIds != null && !tagIds.isEmpty()) {
            for (String tagId : tagIds) {
                jdbcTemplate.update("INSERT INTO document_tag_relation (doc_id, tag_id) VALUES (?, ?)", docId, tagId);
            }
        }
    }
}
