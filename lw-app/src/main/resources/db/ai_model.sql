-- ================================================
-- AI 模型服务商与模型配置表
-- MySQL 8.0+
-- ================================================

-- 1. 服务商表
CREATE TABLE IF NOT EXISTS `ai_providers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `provider_code` VARCHAR(50) NOT NULL COMMENT '服务商代码，如 openai, anthropic',
    `provider_name` VARCHAR(100) NOT NULL COMMENT '服务商显示名称',
    `website` VARCHAR(200) DEFAULT NULL COMMENT '官网地址',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '服务商图标CDN地址',
    `description` TEXT COMMENT '服务商简介',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_code` (`provider_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI服务商表';

-- 2. 模型主表
CREATE TABLE IF NOT EXISTS `ai_models` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model_identifier` VARCHAR(100) NOT NULL COMMENT '模型唯一标识，如 gpt-4o, claude-3-5-sonnet',
    `display_name` VARCHAR(100) NOT NULL COMMENT '模型显示名称，如 GPT-4o',
    `description` TEXT COMMENT '模型描述（展示给用户）',
    `model_badge` VARCHAR(100) DEFAULT NULL COMMENT '模型徽章标签，如"多模态"、"代码增强"',
    `context_length` INT DEFAULT NULL COMMENT '最大上下文长度（千tokens）',
    `multimodal` TINYINT DEFAULT 0 COMMENT '是否支持多模态 0-否 1-是',
    `input_price` DECIMAL(10,6) DEFAULT NULL COMMENT '参考输入价格（元/千tokens），仅供参考',
    `output_price` DECIMAL(10,6) DEFAULT NULL COMMENT '参考输出价格（元/千tokens），仅供参考',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否全局可用 0-停用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_identifier` (`model_identifier`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型主表（独立于服务商）';

-- 3. 模型服务配置表（关联表）
CREATE TABLE IF NOT EXISTS `ai_model_services` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `provider_id` BIGINT NOT NULL COMMENT '关联服务商ID（ai_providers.id）',
    `model_id` BIGINT NOT NULL COMMENT '关联模型ID（ai_models.id）',
    `api_type` VARCHAR(50) NOT NULL DEFAULT 'openai' COMMENT '接口类型：openai, anthropic, gemini, zhipu, minimax, moonshot, qwen, deepseek, hunyuan, doubao',
    `api_key` VARCHAR(255) NOT NULL COMMENT 'API密钥（AES-256-GCM加密存储）',
    `url` VARCHAR(500) NOT NULL COMMENT 'API端点URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `priority` INT DEFAULT 0 COMMENT '优先级，数字越小优先级越高（负载均衡）',
    `description` TEXT COMMENT '该配置的备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_model` (`provider_id`, `model_id`),
    KEY `idx_status_priority` (`status`, `priority`),
    CONSTRAINT `fk_ai_model_services_provider` FOREIGN KEY (`provider_id`) REFERENCES `ai_providers` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_model_services_model` FOREIGN KEY (`model_id`) REFERENCES `ai_models` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型服务接入配置表（关联服务商与模型）';

-- ================================================
-- 示例数据
-- ================================================

-- 1. 插入服务商
INSERT INTO `ai_providers` (`provider_code`, `provider_name`, `website`, `icon`, `description`, `status`) VALUES
('openai', 'OpenAI', 'https://openai.com', '/icons/providers/openai.png', 'OpenAI 官方服务商', 1),
('anthropic', 'Anthropic', 'https://www.anthropic.com', '/icons/providers/anthropic.png', 'Claude 模型提供商', 1),
('google', 'Google', 'https://ai.google.dev', '/icons/providers/google.png', 'Google Gemini 模型', 1),
('zhipu', '智谱AI', 'https://zhipuai.cn', '/icons/providers/zhipu.png', '智谱 AI 大模型', 1),
('minimax', 'MiniMax', 'https://www.minimaxi.com', '/icons/providers/minimax.png', 'MiniMax 开放平台', 1),
('moonshot', '月之暗面', 'https://www.moonshot.cn', '/icons/providers/moonshot.png', 'Kimi 大模型', 1),
('alibaba', '阿里云', 'https://www.aliyun.com', '/icons/providers/alibaba.png', '通义千问大模型', 1),
('deepseek', '深度求索', 'https://www.deepseek.com', '/icons/providers/deepseek.png', 'DeepSeek 模型', 1),
('tencent', '腾讯云', 'https://cloud.tencent.com', '/icons/providers/tencent.png', '腾讯混元大模型', 1),
('bytedance', '字节跳动', 'https://www.volcengine.com', '/icons/providers/bytedance.png', '豆包大模型', 1);

-- 2. 插入模型主表
INSERT INTO `ai_models`
(`model_identifier`, `display_name`, `description`, `model_badge`, `context_length`, `multimodal`, `input_price`, `output_price`, `is_active`)
VALUES
('gpt-4o', 'GPT-4o', 'OpenAI多模态旗舰模型，"omni"全能设计，支持文本、图像、音频输入', '多模态·全能旗舰', 128, 1, 0.0050, 0.0150, 1),
('claude-3-5-sonnet', 'Claude 3.5 Sonnet', '代码修复成功率较前代提升68%，在GPQA推理与编程测试中表现卓越', '代码专家·高性能', 200, 0, 0.0030, 0.0150, 1),
('gemini-3.1-pro', 'Gemini 3.1 Pro', '谷歌最强模型，ARC-AGI-2得分77.1%突破记录，针对复杂多步Agent工作流优化', '推理王者·Agent优化', 1024, 1, 0.0040, 0.0120, 1),
('glm-5', '智谱GLM-5', '智谱AI旗舰开源模型，具备强大的推理能力和长上下文处理能力', '旗舰模型·128K上下文', 128, 0, 0.0025, 0.0075, 1),
('minimax-m2.5', 'MiniMax M2.5', '专为Agent场景原生设计的模型，在Excel处理、深度调研、PPT等办公场景表现优异', 'Agent原生·办公增强', 200, 1, 0.0020, 0.0060, 1),
('kimi-k2.5', 'Kimi K2.5', '编程能力和Agent能力大幅提升的长文本模型，擅长代码生成、Agent协同任务', '代码增强·长文本', 1024, 0, 0.0018, 0.0055, 1),
('qwen3.5', '通义千问3.5', '阿里云自研旗舰模型，多模态与长文本能力突出', '多模态·旗舰级', 256, 1, 0.0022, 0.0066, 1),
('deepseek-v4', 'DeepSeek V4', '极高性价比模型，兼容OpenAI接口规范，百万Tokens输入最低1元', '超低成本·高性价比', 128, 0, 0.0005, 0.0010, 1),
('hunyuan-turbo', '腾讯混元Turbo', '腾讯自研通用大模型，主打快速响应与稳定推理', '快速响应·企业级', 128, 0, 0.0015, 0.0045, 1),
('doubao-5.0', '豆包5.0', '字节跳动旗舰模型，通用大模型第一梯队，多模态能力全面覆盖', '多模态·第一梯队', 128, 1, 0.0019, 0.0057, 1);

-- 3. 插入模型服务配置（关联）
INSERT INTO `ai_model_services`
(`provider_id`, `model_id`, `api_type`, `api_key`, `url`, `status`, `priority`, `description`)
VALUES
-- OpenAI -> GPT-4o
((SELECT id FROM ai_providers WHERE provider_code='openai'),
 (SELECT id FROM ai_models WHERE model_identifier='gpt-4o'),
 'openai', '${OPENAI_API_KEY}', 'https://api.openai.com/v1/chat/completions', 1, 0, '官方接入'),

-- Anthropic -> Claude 3.5 Sonnet
((SELECT id FROM ai_providers WHERE provider_code='anthropic'),
 (SELECT id FROM ai_models WHERE model_identifier='claude-3-5-sonnet'),
 'anthropic', '${ANTHROPIC_API_KEY}', 'https://api.anthropic.com/v1/messages', 1, 0, '官方API'),

-- Google -> Gemini 3.1 Pro
((SELECT id FROM ai_providers WHERE provider_code='google'),
 (SELECT id FROM ai_models WHERE model_identifier='gemini-3.1-pro'),
 'gemini', '${GOOGLE_API_KEY}', 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro:generateContent', 1, 0, 'Vertex AI 接入'),

-- 智谱 -> GLM-5
((SELECT id FROM ai_providers WHERE provider_code='zhipu'),
 (SELECT id FROM ai_models WHERE model_identifier='glm-5'),
 'zhipu', '${ZHIPU_API_KEY}', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 1, 0, '智谱官方'),

-- MiniMax -> minimax-m2.5
((SELECT id FROM ai_providers WHERE provider_code='minimax'),
 (SELECT id FROM ai_models WHERE model_identifier='minimax-m2.5'),
 'minimax', '${MINIMAX_API_KEY}', 'https://api.minimax.chat/v1/text/chatcompletion_pro', 1, 0, '官方v1'),

-- 月之暗面 -> kimi-k2.5
((SELECT id FROM ai_providers WHERE provider_code='moonshot'),
 (SELECT id FROM ai_models WHERE model_identifier='kimi-k2.5'),
 'moonshot', '${MOONSHOT_API_KEY}', 'https://api.moonshot.cn/v1/chat/completions', 1, 0, '官方API'),

-- 阿里云 -> qwen3.5
((SELECT id FROM ai_providers WHERE provider_code='alibaba'),
 (SELECT id FROM ai_models WHERE model_identifier='qwen3.5'),
 'qwen', '${ALIYUN_API_KEY}', 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', 1, 0, 'DashScope 兼容模式'),

-- 深度求索 -> deepseek-v4
((SELECT id FROM ai_providers WHERE provider_code='deepseek'),
 (SELECT id FROM ai_models WHERE model_identifier='deepseek-v4'),
 'deepseek', '${DEEPSEEK_API_KEY}', 'https://api.deepseek.com/v1/chat/completions', 1, 0, 'DeepSeek官方'),

-- 腾讯云 -> hunyuan-turbo
((SELECT id FROM ai_providers WHERE provider_code='tencent'),
 (SELECT id FROM ai_models WHERE model_identifier='hunyuan-turbo'),
 'hunyuan', '${TENCENT_API_KEY}', 'https://api.hunyuan.cloud.tencent.com/v1/chat/completions', 1, 0, '混元API'),

-- 字节跳动 -> doubao-5.0
((SELECT id FROM ai_providers WHERE provider_code='bytedance'),
 (SELECT id FROM ai_models WHERE model_identifier='doubao-5.0'),
 'doubao', '${BYTE_DANCE_API_KEY}', 'https://ark.cn-beijing.volces.com/api/v3/chat/completions', 1, 0, '火山方舟');
