-- 知识库文档表添加切片策略字段
ALTER TABLE knowledge_document 
ADD COLUMN chunking_strategy VARCHAR(50) DEFAULT 'intelligent' COMMENT '切片策略 (intelligent, char, page, heading, regex, separator)',
ADD COLUMN chunk_count INT DEFAULT 0 COMMENT '切片数量';

-- 文档切片表已存在，无需修改
-- 如果需要查看切片表结构：
-- DESC document_chunk;
