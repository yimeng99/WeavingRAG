# lw-file 文件服务模块

## 功能概述

`lw-file` 模块提供统一的文件管理服务，支持：

- **本地文件存储**：将文件保存到服务器本地文件系统
- **阿里云 OSS 存储**：将文件保存到阿里云对象存储
- **文件转换**：支持常见文件格式之间的转换（待实现）
- **文件预览**：支持在浏览器中预览文件

## 快速开始

### 1. 添加依赖

在 `lw-app` 或其他模块的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.weaving.llm</groupId>
    <artifactId>lw-file</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 配置存储方式

#### 本地存储（默认）

```yaml
file:
  storage:
    local-path: uploads
    max-file-size: 104857600  # 100MB
    allowed-extensions:
      - pdf
      - doc
      - docx
      - txt
      - md
      - jpg
      - png
```

#### 阿里云 OSS

```yaml
aliyun:
  oss:
    enabled: true
    endpoint: oss-cn-hangzhou.aliyuncs.com
    bucket-name: rag-weaving
    accessKeyId: your_access_key_id
    accessKeySecret: your_access_key_secret
```

## API 接口

### 文件上传

```http
POST /api/file/upload
Content-Type: multipart/form-data

file: (binary)
userId: 123
```

响应：
```json
{
  "success": true,
  "data": {
    "fileId": "20260406/20260406120000_abc12345.pdf",
    "originalName": "test.pdf",
    "filePath": "20260406/20260406120000_abc12345.pdf",
    "fileUrl": "/api/file/20260406/20260406120000_abc12345.pdf",
    "fileSize": 102400,
    "extension": "pdf",
    "contentType": "application/pdf",
    "storageType": "LOCAL",
    "uploadTime": "2026-04-06T12:00:00.000Z"
  }
}
```

### 批量上传

```http
POST /api/file/upload/batch
Content-Type: multipart/form-data

files: (binary array)
userId: 123
```

### 下载文件

```http
GET /api/file/{fileId}
```

### 预览文件

```http
GET /api/file/preview/{fileId}
```

### 删除文件

```http
DELETE /api/file/{fileId}
```

### 获取文件信息

```http
GET /api/file/info/{fileId}
```

### 检查文件是否存在

```http
GET /api/file/exists/{fileId}
```

### 获取支持的转换格式

```http
GET /api/file/conversion/supported/{extension}
```

## 文件存储结构

### 本地存储

```
uploads/
├── 20260406/
│   ├── 20260406120000_abc12345.pdf
│   └── 20260406120001_def67890.docx
├── 20260407/
│   └── ...
```

### OSS 存储

```
rag-weaving/
├── 20260406/
│   ├── 20260406120000_abc12345.pdf
│   └── ...
```

## 代码示例

### 注入服务使用

```java
@Autowired
@Qualifier("localFileStorageService")
private FileStorageService fileStorageService;

// 上传文件
FileInfo fileInfo = fileStorageService.uploadFile(multipartFile, userId);

// 下载文件
InputStream inputStream = fileStorageService.downloadFile(fileInfo.getFileId());

// 删除文件
boolean deleted = fileStorageService.deleteFile(fileInfo.getFileId());

// 获取文件信息
FileInfo info = fileStorageService.getFileInfo(fileInfo.getFileId());
```

## 文件转换（待实现）

目前支持以下转换类型：

| 源格式 | 目标格式 |
|--------|----------|
| PDF | TXT, MD, PNG, JPG |
| DOC | DOCX, PDF, TXT, MD |
| DOCX | PDF, TXT, MD |
| XLS | XLSX, CSV, PDF |
| XLSX | CSV, PDF |
| JPG | PNG, JPEG, BMP, GIF |
| PNG | JPG, JPEG, BMP, GIF |

## 安全考虑

1. **文件类型校验**：只允许配置的扩展名上传
2. **文件大小限制**：防止上传过大文件
3. **文件名处理**：使用 UUID 生成存储文件名，防止文件名冲突和路径遍历攻击
4. **MD5 校验**：支持文件完整性校验

## TODO

- [ ] 实现文件转换功能
- [ ] 添加图片压缩/缩略图生成
- [ ] 支持断点续传
- [ ] 支持大文件分片上传
- [ ] 添加文件访问权限控制
- [ ] 集成 MinIO 作为 OSS 替代方案
