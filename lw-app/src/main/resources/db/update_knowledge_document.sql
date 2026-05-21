-- 更新已有数据库，添加切片相关字段（不删除数据）
-- 在 MySQL 中执行: mysql -u root -p llm_weaving < db/update_knowledge_document.sql

-- 检查并添加 chunking_strategy 字段
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = 'llm_weaving' 
    AND table_name = 'knowledge_document' 
    AND column_name = 'chunking_strategy'
);

SET @add_chunking_strategy = IF(@column_exists = 0, 
    'ALTER TABLE knowledge_document ADD COLUMN chunking_strategy VARCHAR(50) DEFAULT "intelligent" COMMENT "切片策略 (intelligent, char, page, heading, regex, separator)"', 
    'SELECT "chunking_strategy 字段已存在"'
);
PREPARE stmt1 FROM @add_chunking_strategy;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 检查并添加 chunk_count 字段
SET @column_exists2 = (
    SELECT COUNT(*) 
    FROM information_schema.columns 
    WHERE table_schema = 'llm_weaving' 
    AND table_name = 'knowledge_document' 
    AND column_name = 'chunk_count'
);

SET @add_chunk_count = IF(@column_exists2 = 0, 
    'ALTER TABLE knowledge_document ADD COLUMN chunk_count INT DEFAULT 0 COMMENT "切片数量"', 
    'SELECT "chunk_count 字段已存在"'
);
PREPARE stmt2 FROM @add_chunk_count;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SELECT '知识库文档表更新完成' AS result;
