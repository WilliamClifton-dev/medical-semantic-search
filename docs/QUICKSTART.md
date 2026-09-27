# 快速启动指南

## 1. 启动基础设施

```bash
# 进入项目目录
cd medical-semantic-search

# 启动 Milvus 和 MySQL
docker-compose up -d

# 查看服务状态
docker-compose ps
```

等待所有服务健康检查通过（约 1-2 分钟）。

## 2. 配置环境变量

```bash
# Windows PowerShell
$env:MYSQL_PASSWORD = "medical_pass_2024"
$env:EMBEDDING_PROVIDER = "cloud"  # 或 "local"
$env:EMBEDDING_API_KEY = "your-minimax-api-key"  # 仅云端模式需要
```

## 3. 运行应用

```bash
# 编译并启动
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## 4. 访问 API 文档

打开浏览器访问：`http://localhost:8080/swagger-ui.html`

## 5. 快速测试

### 5.1 查看统计信息

```bash
curl http://localhost:8080/api/v1/documents/stats
```

应该看到已导入 10 条示例数据。

### 5.2 关键词搜索（传统方式）

```bash
curl -X POST http://localhost:8080/api/v1/search/keyword \
  -H "Content-Type: application/json" \
  -d '{"query": "高血压"}'
```

### 5.3 语义搜索（向量数据库）

```bash
curl -X POST http://localhost:8080/api/v1/search/semantic \
  -H "Content-Type: application/json" \
  -d '{
    "query": "心脏病发作怎么办",
    "topK": 5,
    "threshold": 0.7
  }'
```

这个查询会匹配到"急性心肌梗死"相关的文档，尽管没有包含"心脏病发作"这个精确词汇。

### 5.4 对比搜索

```bash
curl -X POST http://localhost:8080/api/v1/search/compare \
  -H "Content-Type: application/json" \
  -d '{
    "query": "血糖控制",
    "topK": 5
  }'
```

这个接口会同时返回关键词搜索和语义搜索的结果，以及各自的耗时。

### 5.5 相似病例推荐

```bash
curl -X POST http://localhost:8080/api/v1/search/similar-cases \
  -H "Content-Type: application/json" \
  -d '{
    "caseDescription": "65岁男性患者，血压高，血糖也高，如何用药？",
    "topK": 3
  }'
```

应该匹配到"高血压合并糖尿病"的病例。

## 6. 访问管理界面

### Milvus 管理界面 (Attu)

访问：`http://localhost:8000`

可以可视化查看向量数据库的 Collection、索引和数据。

### MySQL 数据库

```bash
docker exec -it medical-mysql mysql -uroot -pmedical_pass_2024 medical_search
```

## 7. 停止服务

```bash
# 停止所有服务
docker-compose down

# 清理数据（慎用！会删除所有数据）
docker-compose down -v
```

## 常见问题

### Q1: Milvus 启动失败

**现象**: `milvus-standalone` 容器不断重启

**解决**: 
- 检查是否有足够的内存（至少 4GB）
- 查看日志: `docker logs milvus-standalone`
- 确保端口 19530 未被占用

### Q2: Embedding API 调用失败

**现象**: 语义搜索报错 "生成 embedding 失败"

**解决**:
- 检查 `EMBEDDING_API_KEY` 是否正确
- 检查网络连接
- 如果使用本地模式，确保已部署 BGE-M3 模型服务

### Q3: MySQL 连接失败

**现象**: 应用启动时报 "Failed to obtain JDBC Connection"

**解决**:
- 确保 MySQL 容器已启动: `docker-compose ps`
- 检查密码是否正确: `$env:MYSQL_PASSWORD`
- 等待 MySQL 完全启动（首次启动需要 30 秒左右）

### Q4: Flyway 迁移失败

**现象**: "Flyway migration failed"

**解决**:
- 清空数据库重新迁移: `docker-compose down -v && docker-compose up -d`
- 检查 SQL 文件编码是否为 UTF-8

## 下一步

1. **导入更多数据**: 使用 `/api/v1/documents/batch` 批量导入
2. **性能测试**: 对比不同数据量下的检索性能
3. **调优参数**: 调整 Milvus 索引参数（M, efConstruction）
4. **混合检索**: 实现关键词 + 语义的融合排序

## 相关链接

- [Milvus 官方文档](https://milvus.io/docs)
- [BGE-M3 模型](https://huggingface.co/BAAI/bge-m3)
- [项目 GitHub](https://github.com/your-repo/medical-semantic-search)
