# AI 模型服务商与模型配置

## 概述

本文档描述了 AI 模型服务商、模型和模型服务配置三张表的设计。

## 表结构

### 1. ai_providers - AI服务商表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| provider_code | VARCHAR(50) | 服务商代码，如 openai, anthropic |
| provider_name | VARCHAR(100) | 服务商显示名称 |
| website | VARCHAR(200) | 官网地址 |
| icon | VARCHAR(500) | 服务商图标CDN地址 |
| description | TEXT | 服务商简介 |
| status | TINYINT | 状态 0-禁用 1-启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2. ai_models - 模型主表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| model_identifier | VARCHAR(100) | 模型唯一标识，如 gpt-4o |
| display_name | VARCHAR(100) | 模型显示名称 |
| description | TEXT | 模型描述 |
| model_badge | VARCHAR(100) | 模型徽章标签 |
| context_length | INT | 最大上下文长度（千tokens） |
| multimodal | TINYINT | 是否支持多模态 0-否 1-是 |
| input_price | DECIMAL(10,6) | 参考输入价格（元/千tokens） |
| output_price | DECIMAL(10,6) | 参考输出价格（元/千tokens） |
| is_active | TINYINT | 是否全局可用 0-停用 1-启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3. ai_model_services - 模型服务配置表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| provider_id | BIGINT | 关联服务商ID |
| model_id | BIGINT | 关联模型ID |
| api_type | VARCHAR(50) | 接口类型 |
| api_key | VARCHAR(255) | API密钥（AES-256-GCM加密存储） |
| url | VARCHAR(500) | API端点URL |
| status | TINYINT | 状态 0-禁用 1-启用 |
| priority | INT | 优先级，数字越小优先级越高 |
| description | TEXT | 配置备注 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 关联关系

```
┌─────────────────┐       ┌─────────────────────────┐       ┌─────────────────┐
│   ai_providers  │       │   ai_model_services     │       │    ai_models    │
├─────────────────┤       ├─────────────────────────┤       ├─────────────────┤
│ id (PK)         │◄──────│ provider_id (FK)        │       │ id (PK)         │
│ provider_code   │       │ model_id (FK)           │──────►│ model_identifier│
│ provider_name   │       │ api_type                │       │ display_name    │
│ website         │       │ api_key                 │       │ description     │
│ icon            │       │ url                     │       │ model_badge     │
│ description     │       │ status                  │       │ context_length  │
│ status          │       │ priority                │       │ multimodal      │
└─────────────────┘       │ description             │       │ input_price     │
                          └─────────────────────────┘       │ output_price    │
                                                             │ is_active       │
                                                             └─────────────────┘
```

`ai_model_services` 是关联表，一个服务商可以提供多个模型，一个模型也可以被多个服务商提供。

## 服务商列表

| 代码 | 名称 | 官网 |
|------|------|------|
| openai | OpenAI | https://openai.com |
| anthropic | Anthropic | https://www.anthropic.com |
| google | Google | https://ai.google.dev |
| zhipu | 智谱AI | https://zhipuai.cn |
| minimax | MiniMax | https://www.minimaxi.com |
| moonshot | 月之暗面 | https://www.moonshot.cn |
| alibaba | 阿里云 | https://www.aliyun.com |
| deepseek | 深度求索 | https://www.deepseek.com |
| tencent | 腾讯云 | https://cloud.tencent.com |
| bytedance | 字节跳动 | https://www.volcengine.com |

## 模型列表

| 标识符 | 显示名称 | 徽章 | 上下文 | 多模态 | 输入价格 | 输出价格 |
|--------|----------|------|--------|--------|----------|----------|
| gpt-4o | GPT-4o | 多模态·全能旗舰 | 128K | 是 | 0.0050 | 0.0150 |
| claude-3-5-sonnet | Claude 3.5 Sonnet | 代码专家·高性能 | 200K | 否 | 0.0030 | 0.0150 |
| gemini-3.1-pro | Gemini 3.1 Pro | 推理王者·Agent优化 | 1024K | 是 | 0.0040 | 0.0120 |
| glm-5 | 智谱GLM-5 | 旗舰模型·128K上下文 | 128K | 否 | 0.0025 | 0.0075 |
| minimax-m2.5 | MiniMax M2.5 | Agent原生·办公增强 | 200K | 是 | 0.0020 | 0.0060 |
| kimi-k2.5 | Kimi K2.5 | 代码增强·长文本 | 1024K | 否 | 0.0018 | 0.0055 |
| qwen3.5 | 通义千问3.5 | 多模态·旗舰级 | 256K | 是 | 0.0022 | 0.0066 |
| deepseek-v4 | DeepSeek V4 | 超低成本·高性价比 | 128K | 否 | 0.0005 | 0.0010 |
| hunyuan-turbo | 腾讯混元Turbo | 快速响应·企业级 | 128K | 否 | 0.0015 | 0.0045 |
| doubao-5.0 | 豆包5.0 | 多模态·第一梯队 | 128K | 是 | 0.0019 | 0.0057 |

## 环境变量

API密钥通过环境变量注入，SQL 中的占位符：

| 服务商 | 环境变量 |
|--------|----------|
| OpenAI | ${OPENAI_API_KEY} |
| Anthropic | ${ANTHROPIC_API_KEY} |
| Google | ${GOOGLE_API_KEY} |
| 智谱AI | ${ZHIPU_API_KEY} |
| MiniMax | ${MINIMAX_API_KEY} |
| 月之暗面 | ${MOONSHOT_API_KEY} |
| 阿里云 | ${ALIYUN_API_KEY} |
| DeepSeek | ${DEEPSEEK_API_KEY} |
| 腾讯云 | ${TENCENT_API_KEY} |
| 字节跳动 | ${BYTE_DANCE_API_KEY} |