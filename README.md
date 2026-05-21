# WeavingRAG

[English](#english) | [中文](#中文)

---

## English

### Overview

**WeavingRAG** is an enterprise-grade "Knowledge Weaver" that precisely weaves scattered private data with large language model capabilities, eliminating hallucinations and delivering traceable, high-reliability intelligent answers.

### Features

- **RAG (Retrieval-Augmented Generation)**: Combines vector database retrieval with LLM generation for accurate, context-aware responses
- **Multi-Vector Store Support**: Supports Chroma, Milvus, PgVector and other vector databases
- **Multi-Model Support**: Compatible with Qwen, OpenAI, Ollama and other LLM providers
- **Enterprise-Grade Security**: API keys and sensitive configurations managed via environment variables
- **Document Intelligence**: Automatic document parsing, chunking, and embedding
- **Knowledge Base Management**: Visual management of knowledge bases and documents

### Tech Stack

- **Backend**: Java (Spring Boot), MyBatis-Plus
- **Frontend**: Vue.js
- **Vector Databases**: Chroma, Milvus, PgVector
- **LLM Providers**: DashScope (Qwen), OpenAI, Ollama

### Quick Start

```bash
# Clone the repository
git clone https://github.com/yimeng99/WeavingRAG.git
cd WeavingRAG

# Configure environment variables
cp lw-app/src/main/resources/application.example.yml lw-app/src/main/resources/application.yml
# Edit application.yml with your API keys and database configuration

# Start with Docker Compose
docker-compose up -d

# Or run locally with Maven
./mvnw spring-boot:run
```

### Configuration

Configure via `application.yml`:

```yaml
aliyun:
  api-key: ${ALIYUN_API_KEY}
  model-name: qwen3.5-flash
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1

# Ollama (optional, for embeddings)
ollama:
  base-url: http://localhost:11434
  embedding-model: quentinz/bge-large-zh-v1.5

# Chroma Vector Database
chroma:
  enabled: true
  host: localhost
  port: 8000
```

### Project Structure

```
WeavingRAG/
├── lw-app/          # Main application module
├── lw-common/       # Common utilities and services
├── lw-rag/          # RAG core functionality
├── lw-file/         # File handling module
├── lw-agent/        # Agent capabilities
├── frontend/        # Vue.js frontend
└── database/        # SQL scripts
```

### License

MIT License

---

## 中文

### 概述

**WeavingRAG** 是一款企业级"知识织机"，将分散的私有数据与大模型能力精密编织，彻底告别幻觉，交付可溯源、高可信的智能答案。

### 核心功能

- **RAG（检索增强生成）**：结合向量数据库检索与大模型生成，提供准确、上下文相关的回答
- **多向量存储支持**：支持 Chroma、Milvus、PgVector 等向量数据库
- **多模型支持**：兼容通义千问、OpenAI、Ollama 等大模型
- **企业级安全保障**：API 密钥和敏感配置通过环境变量管理
- **文档智能处理**：自动文档解析、分块和向量化
- **知识库可视化管理**：支持知识库和文档的可视化管理

### 技术栈

- **后端**：Java (Spring Boot), MyBatis-Plus
- **前端**：Vue.js
- **向量数据库**：Chroma, Milvus, PgVector
- **大模型**：通义千问 (DashScope), OpenAI, Ollama

### 快速开始

```bash
# 克隆仓库
git clone https://github.com/yimeng99/WeavingRAG.git
cd WeavingRAG

# 配置环境变量
cp lw-app/src/main/resources/application.example.yml lw-app/src/main/resources/application.yml
# 编辑 application.yml，填入您的 API 密钥和数据库配置

# 使用 Docker Compose 启动
docker-compose up -d

# 或使用 Maven 本地运行
./mvnw spring-boot:run
```

### 配置说明

通过 `application.yml` 进行配置：

```yaml
aliyun:
  api-key: ${ALIYUN_API_KEY}
  model-name: qwen3.5-flash
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1

# Ollama 配置（可选，用于嵌入模型）
ollama:
  base-url: http://localhost:11434
  embedding-model: quentinz/bge-large-zh-v1.5

# Chroma 向量数据库配置
chroma:
  enabled: true
  host: localhost
  port: 8000
```

### 项目结构

```
WeavingRAG/
├── lw-app/          # 主应用模块
├── lw-common/       # 公共工具和服务
├── lw-rag/          # RAG 核心功能
├── lw-file/         # 文件处理模块
├── lw-agent/        # Agent 能力
├── frontend/        # Vue.js 前端
└── database/        # SQL 脚本
```

### 许可证

MIT License