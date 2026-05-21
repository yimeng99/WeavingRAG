-- 创建数据库
CREATE DATABASE IF NOT EXISTS `llm_weaving` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `llm_weaving`;

-- ================================
-- 用户表
-- ================================
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码 (加密存储)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像 URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识 (0:未删除，1:已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ================================
-- 聊天会话表
-- ================================
DROP TABLE IF EXISTS `chat_session`;

CREATE TABLE `chat_session` (
  `session_id` varchar(64) NOT NULL COMMENT '会话 ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
  `title` varchar(255) DEFAULT NULL COMMENT '会话标题',
  `model` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `message_count` int(11) DEFAULT 0 COMMENT '消息数量',
  `preview` varchar(500) DEFAULT NULL COMMENT '最后一条消息预览',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识 (0:未删除，1:已删除)',
  PRIMARY KEY (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- ================================
-- 聊天消息表
-- ================================
DROP TABLE IF EXISTS `chat_message`;

CREATE TABLE `chat_message` (
  `message_id` varchar(64) NOT NULL COMMENT '消息 ID',
  `session_id` varchar(64) DEFAULT NULL COMMENT '会话 ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
  `role` varchar(20) DEFAULT NULL COMMENT '消息角色 (user/assistant/system)',
  `content` text COMMENT '消息内容',
  `parent_message_id` varchar(64) DEFAULT NULL COMMENT '父消息 ID',
  `model` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `token_usage` int(11) DEFAULT NULL COMMENT 'token 使用量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识 (0:未删除，1:已删除)',
  PRIMARY KEY (`message_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ================================
-- 知识库表
-- ================================
DROP TABLE IF EXISTS `knowledge_base`;

CREATE TABLE `knowledge_base` (
  `id` varchar(64) NOT NULL COMMENT '知识库 ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
  `name` varchar(255) NOT NULL COMMENT '知识库名称',
  `description` varchar(500) DEFAULT NULL COMMENT '知识库描述',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `color` varchar(20) DEFAULT '#667eea' COMMENT '主题颜色',
  `doc_count` int(11) DEFAULT 0 COMMENT '文档数量',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态 (0:禁用，1:启用)',
  `is_public` tinyint(1) DEFAULT 0 COMMENT '是否公开 (0:私有，1:公开)',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识 (0:未删除，1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- ================================
-- 知识库标签表
-- ================================
DROP TABLE IF EXISTS `knowledge_tag`;

CREATE TABLE `knowledge_tag` (
  `id` varchar(64) NOT NULL COMMENT '标签 ID',
  `knowledge_base_id` varchar(64) NOT NULL COMMENT '知识库 ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `color` varchar(20) DEFAULT '#409eff' COMMENT '标签颜色',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库标签表';

-- ================================
-- 文档标签关联表
-- ================================
DROP TABLE IF EXISTS `document_tag_relation`;

CREATE TABLE `document_tag_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `doc_id` varchar(64) NOT NULL COMMENT '文档 ID',
  `tag_id` varchar(64) NOT NULL COMMENT '标签 ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_tag` (`doc_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档标签关联表';

-- ================================
-- 文档切片表
-- ================================
DROP TABLE IF EXISTS `document_chunk`;

CREATE TABLE `document_chunk` (
  `id` varchar(64) NOT NULL COMMENT '切片 ID',
  `doc_id` varchar(64) NOT NULL COMMENT '文档 ID',
  `content` longtext COMMENT '切片内容',
  `chunk_index` int(11) DEFAULT 0 COMMENT '切片序号',
  `vector_id` varchar(64) DEFAULT NULL COMMENT '向量 ID',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态 (0:失败, 1:成功)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

-- ================================
-- 知识库文档表
-- ================================
DROP TABLE IF EXISTS `knowledge_document`;

CREATE TABLE `knowledge_document` (
  `doc_id` varchar(64) NOT NULL COMMENT '文档 ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
  `knowledge_base_id` varchar(64) DEFAULT NULL COMMENT '知识库 ID',
  `title` varchar(255) DEFAULT NULL COMMENT '文档标题',
  `content` longtext COMMENT '文档内容',
  `type` varchar(50) DEFAULT NULL COMMENT '文档类型 (text, markdown, pdf, url 等)',
  `source` varchar(500) DEFAULT NULL COMMENT '来源 URL 或文件路径',
  `tags` varchar(500) DEFAULT NULL COMMENT '关键词标签 (逗号分隔)',
  `vector_id` varchar(64) DEFAULT NULL COMMENT '向量 ID(Milvus 中的向量 ID)',
  `chunk_index` int(11) DEFAULT NULL COMMENT '分块序号',
  `chunk_size` int(11) DEFAULT NULL COMMENT '分块大小',
  `chunking_strategy` varchar(50) DEFAULT 'intelligent' COMMENT '切片策略 (intelligent, char, page, heading, regex, separator)',
  `chunk_count` int(11) DEFAULT 0 COMMENT '切片数量',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态 (0:处理中，1:已完成，2:失败)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识 (0:未删除，1:已删除)',
  PRIMARY KEY (`doc_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- ================================
-- 插入默认数据
-- ================================
-- 插入默认用户 (密码 MD5 加密: admin123 -> 0192023a7bbd73250516f069df18b500)
INSERT INTO `user` (`username`, `password`, `email`, `avatar`) 
VALUES ('admin', '0192023a7bbd73250516f069df18b500', 'admin@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin')
ON DUPLICATE KEY UPDATE username=username;

-- ================================
-- 自定义分片策略表
-- ================================
DROP TABLE IF EXISTS `custom_chunking_strategy`;

CREATE TABLE `custom_chunking_strategy` (
  `id` varchar(64) NOT NULL COMMENT '策略 ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户 ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `description` varchar(500) DEFAULT NULL COMMENT '策略描述',
  `type` varchar(50) DEFAULT 'separator' COMMENT '策略类型 (separator, char)',
  `chunk_size` int(11) DEFAULT 500 COMMENT '分片大小',
  `overlap` int(11) DEFAULT 50 COMMENT '重叠字符数',
  `separators` varchar(255) DEFAULT NULL COMMENT '分隔符列表',
  `max_chars` int(11) DEFAULT NULL COMMENT '最大字符数',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态 (0:禁用, 1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识 (0:未删除，1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义分片策略表';
