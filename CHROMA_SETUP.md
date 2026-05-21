# Chroma 向量数据库部署说明

## 1. 使用 Docker 部署 Chroma（推荐）

### 使用 docker-compose 部署

```bash
# 启动 Chroma
docker-compose -f docker-compose.chroma.yml up -d
```

### 或使用单机命令

```bash
docker run -d -p 8000:8000 \
  --name chroma \
  -v chroma_data:/chroma \
  chromadb/chroma:latest
```

访问地址：http://localhost:8000

## 2. 本地开发模式（无需 Docker）

如果你只是想快速测试，可以在本地直接运行 Chroma：

```bash
# 安装 chromadb
pip install chromadb

# 启动服务
chroma run --path ./chroma-data --host 0.0.0.0 --port 8000
```

## 3. 配置说明

### application.yml 配置

```yaml
chroma:
  enabled: true
  host: localhost
  port: 8000
  collection-name: knowledge_vectors
```

### 环境变量（可选）

```bash
export CHROMA_HOST=localhost
export CHROMA_PORT=8000
export CHROMA_COLLECTION=knowledge_vectors
```

## 4. 验证部署

```bash
# 检查 Chroma 是否运行
curl http://localhost:8000/api/v1/healthcheck

# 查看集合列表
curl http://localhost:8000/api/v1/collections
```

## 5. 与 Milvus 的对比

| 特性 | Milvus | Chroma |
|------|--------|--------|
| 部署复杂度 | 高（需要 etcd、MinIO） | 低（单容器） |
| 内存占用 | ~500MB+ | ~200MB |
| 磁盘占用 | ~2GB+ | ~500MB |
| 启动时间 | ~2 分钟 | ~10 秒 |
| 适用场景 | 大规模生产 | 开发/中小规模 |

## 6. 数据持久化

Chroma 数据默认存储在 Docker volume 中：
- Linux/Mac: `/var/lib/docker/volumes/chroma_data`
- Windows: `\\wsl$\docker-desktop-data\...`

如需备份，可以导出 volume：
```bash
docker run --rm -v chroma_data:/data -v $(pwd):/backup alpine tar czf /backup/chroma-backup.tar.gz /data
```

## 7. 故障排查

### 连接失败
```bash
# 检查容器状态
docker ps | grep chroma

# 查看日志
docker logs chroma
```

### 端口冲突
```bash
# 修改端口
docker run -d -p 8001:8000 chromadb/chroma:latest
# 然后修改 application.yml 中的端口
```
