# 本地 Embedding 模型部署指南

## 概述

本系统支持两种 Embedding 模式：

1. **云端模式**（推荐用于快速开始）: 调用 MiniMax Embedding API
2. **本地模式**（推荐用于生产环境）: 部署 BGE-M3 模型本地服务

当前代码提供了**模拟 Embedding 实现**，可用于演示和测试，但**不适合生产环境**。

## 模拟模式（当前实现）

### 工作原理

- 基于文本哈希生成确定性的 768 维向量
- 相同文本总是得到相同向量
- 向量经过归一化，可用于余弦相似度计算

### 适用场景

✅ 项目演示和功能展示  
✅ 单元测试和集成测试  
✅ 理解向量检索的原理  

❌ 不适合生产环境（语义理解能力为零）

### 使用方法

```bash
# 启动应用时设置为本地模式
$env:EMBEDDING_PROVIDER = "local"
mvn spring-boot:run
```

应用会输出警告日志：
```
WARN: 当前使用模拟 embedding，仅用于演示！生产环境请部署真实的 BGE-M3 模型服务
```

## 真实本地模型部署（生产推荐）

### 方案一: 使用 FastAPI + BGE-M3

#### 1. 安装依赖

```bash
pip install fastapi uvicorn sentence-transformers torch
```

#### 2. 创建模型服务 `embedding_service.py`

```python
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import torch

app = FastAPI()

# 加载 BGE-M3 模型
print("Loading BGE-M3 model...")
model = SentenceTransformer('BAAI/bge-m3')
print("Model loaded successfully!")

class EmbeddingRequest(BaseModel):
    text: str

class BatchEmbeddingRequest(BaseModel):
    texts: list[str]

@app.post("/embed")
async def embed(request: EmbeddingRequest):
    """生成单个文本的 embedding"""
    embedding = model.encode(request.text, normalize_embeddings=True)
    return {
        "embedding": embedding.tolist(),
        "dimension": len(embedding)
    }

@app.post("/embed-batch")
async def embed_batch(request: BatchEmbeddingRequest):
    """批量生成 embedding"""
    embeddings = model.encode(
        request.texts, 
        normalize_embeddings=True,
        batch_size=32
    )
    return {
        "embeddings": [emb.tolist() for emb in embeddings],
        "dimension": len(embeddings[0])
    }

@app.get("/health")
async def health():
    return {"status": "ok", "model": "BAAI/bge-m3"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
```

#### 3. 启动服务

```bash
python embedding_service.py
```

服务将在 `http://localhost:8001` 启动。

#### 4. 修改 Java 代码

更新 `EmbeddingService.java` 的 `embedLocal()` 方法：

```java
private List<Float> embedLocal(String text) {
    log.debug("调用本地 BGE-M3 模型服务");

    try {
        Map<String, String> requestBody = Map.of("text", text);

        Map<String, Object> response = webClient.post()
                .uri("http://localhost:8001/embed")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("本地模型服务返回为空");
        }

        List<Double> embedding = (List<Double>) response.get("embedding");
        return embedding.stream()
                .map(Double::floatValue)
                .toList();

    } catch (Exception e) {
        log.error("调用本地模型服务失败", e);
        throw new RuntimeException("生成 embedding 失败: " + e.getMessage(), e);
    }
}
```

#### 5. 更新配置

```yaml
# application.yml
embedding:
  provider: local
  local:
    service-url: http://localhost:8001
    model-name: BAAI/bge-m3
```

### 方案二: 使用 Docker 部署

创建 `docker-compose.embedding.yml`:

```yaml
version: '3.8'

services:
  embedding-service:
    image: python:3.11-slim
    working_dir: /app
    volumes:
      - ./embedding_service.py:/app/embedding_service.py
      - ./models:/root/.cache/huggingface  # 缓存模型
    ports:
      - "8001:8001"
    command: >
      sh -c "pip install fastapi uvicorn sentence-transformers torch &&
             python embedding_service.py"
    environment:
      - TRANSFORMERS_CACHE=/root/.cache/huggingface
```

启动:

```bash
docker-compose -f docker-compose.embedding.yml up -d
```

### 性能对比

| 方案 | 首次加载时间 | 单次推理 | 批量推理(32) | 内存占用 |
|------|------------|---------|-------------|---------|
| 模拟模式 | 0ms | <1ms | <5ms | 0MB |
| 本地 BGE-M3 (CPU) | 5s | 50ms | 800ms | 2GB |
| 本地 BGE-M3 (GPU) | 5s | 10ms | 150ms | 4GB |
| 云端 MiniMax API | 0ms | 200ms | 3s | 0MB |

## 云端模式配置（推荐用于开发）

```bash
# 配置环境变量
$env:EMBEDDING_PROVIDER = "cloud"
$env:EMBEDDING_API_KEY = "your-minimax-api-key"
$env:EMBEDDING_MODEL = "embo-01"

# 启动应用
mvn spring-boot:run
```

### MiniMax Embedding API 说明

- **模型**: embo-01
- **维度**: 1024 维（与 BGE-M3 不同）
- **定价**: ¥0.1 / 百万 tokens
- **速率限制**: 60 QPS

## 推荐配置

### 开发环境
- 使用**云端模式**，开箱即用
- 或使用**模拟模式**进行功能测试

### 生产环境
- 使用**本地 BGE-M3**，数据不出内网
- 考虑 GPU 加速以提升性能
- 配置模型缓存以加快启动

### 课程演示
- 使用**模拟模式**即可，展示向量检索原理
- 在演示文档中说明这是简化实现
- 强调生产环境需要真实模型

## 常见问题

### Q: 模拟模式和真实模型有什么区别？

**模拟模式**:
- ❌ 无语义理解能力
- ✅ 但能演示向量检索的技术流程
- ✅ 相同文本得到相同向量（可复现）

**真实模型**:
- ✅ 理解文本语义
- ✅ 支持同义词、近义词召回
- ✅ 支持跨语言检索

### Q: 为什么不在项目中内置模型加载？

Java 生态缺少成熟的深度学习框架，直接加载 PyTorch 模型比较困难。推荐的架构是：
- Java 应用负责业务逻辑和向量数据库操作
- Python 服务负责 Embedding 生成
- 通过 HTTP 接口解耦

### Q: 如何验证本地模型是否正常工作？

```bash
# 测试健康检查
curl http://localhost:8001/health

# 测试单个文本
curl -X POST http://localhost:8001/embed \
  -H "Content-Type: application/json" \
  -d '{"text": "测试文本"}'

# 应该返回 768 维向量
```

## 参考资料

- [BGE-M3 模型](https://huggingface.co/BAAI/bge-m3)
- [Sentence Transformers 文档](https://www.sbert.net/)
- [MiniMax Embedding API](https://api.minimax.chat/document/guides/embeddings)
