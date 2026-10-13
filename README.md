# 懂车帝风格 AI 汽车助手

一个前后端分离的汽车内容 + AI 问答项目：前端是移动端 H5（首页信息流、选车、视频库、视频详情），后端用 Spring AI 接入 DeepSeek，另有登录注册 / 账号体系，以及一个把本地工具以 MCP 协议暴露出去的示范模块。

## 目录结构

```
VueTest/
├── myaichat/myaichat/      # 前端：Vue 3 + Vite
└── springAI/springAI/      # 后端：Maven 多模块
    ├── springDemo/         # 主服务：8123，提供 AI 对话 + 登录注册 + 视频/评论接口
    └── ChatMcpModel/       # MCP Server：8082，暴露数学/时间/邮件工具
```

## 技术栈

| 端 | 技术 |
| --- | --- |
| 前端 | Vue 3.5、Vite 8、Vue Router 4、Element Plus、axios |
| 后端 | Java 17、Spring Boot 3.5、Spring AI 1.0.7、Spring JDBC、H2、SpringDoc OpenAPI |
| 模型 | DeepSeek（通过 OpenAI 兼容协议调用） |
| 检索（RAG） | 自研内存向量库 + OpenAI 兼容 Embedding（默认 Ollama bge-m3），零新增依赖 |

## 快速开始

### 1. 配置密钥（必做）

密钥不再写在代码里，必须通过环境变量提供。**原仓库里出现过的 DeepSeek Key 与 163 邮箱授权码已视为泄露，请先到对应平台重新生成。**

```powershell
# 主服务（AI 对话）
$env:DEEPSEEK_API_KEY = "sk-你的新key"

# MCP 模块（邮件工具）可选
$env:MAIL_USERNAME = "你的邮箱@163.com"
$env:MAIL_PASSWORD = "邮箱授权码"
```

| 变量 | 模块 | 必填 | 默认值 |
| --- | --- | --- | --- |
| `DEEPSEEK_API_KEY` | springDemo | 是 | 无，缺失时启动失败 |
| `DEEPSEEK_BASE_URL` | springDemo | 否 | `https://api.deepseek.com` |
| `DEEPSEEK_CHAT_MODEL` | springDemo | 否 | `deepseek-chat` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | ChatMcpModel | 是 | 无 |
| `MAIL_HOST` / `MAIL_PORT` | ChatMcpModel | 否 | `smtp.163.com` / `465` |

> 用户账号用的是内嵌 H2 文件库，不需要额外安装数据库，也无需配置账号密码。

### 2. 启动后端

用 IDEA 直接运行 `com.hkd.Main`，或命令行：

```bash
cd springAI/springAI
mvn -pl springDemo spring-boot:run     # 主服务 http://localhost:8123/api
mvn -pl ChatMcpModel spring-boot:run   # MCP 服务 http://localhost:8082/sse（可选）
```

接口文档：<http://localhost:8123/api/swagger-ui.html>

### 3. 启动前端

```bash
cd myaichat/myaichat
npm install
npm run dev        # http://localhost:5000
```

### 4. 启用知识库（RAG，可选）

RAG 需要额外的 embedding 服务（DeepSeek 只有 chat、没有 embedding）。默认接**本地 Ollama**，零成本：

```bash
ollama pull bge-m3     # 中文效果好的 embedding 模型
ollama serve           # 默认监听 127.0.0.1:11434
```

不想装 Ollama，就换成任何 OpenAI 兼容的 embedding 服务（硅基流动 / 智谱 / OpenAI），改环境变量即可：

```powershell
$env:RAG_EMBEDDING_BASE_URL = "https://api.siliconflow.cn"
$env:RAG_EMBEDDING_API_KEY  = "sk-你的key"
$env:RAG_EMBEDDING_MODEL    = "BAAI/bge-m3"
```

## 接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册（入参 `{ username, password, displayName }`），成功后自动登录 |
| POST | `/api/auth/login` | 登录（入参 `{ username, password }`） |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/me` | 当前登录用户，未登录返回 `data: null` |
| POST | `/api/web/aichat` | 同步对话，入参 `{ message }`（会话身份走 Cookie） |
| GET | `/api/web/chatstream` | 流式对话（SSE），参数 `message`，结束时下发 `[COMPLETE]` |
| DELETE | `/api/web/aichat/memory` | 清空当前浏览器会话的上下文 |
| GET | `/api/videos` | 视频列表（公开，带 `mine` 标记） |
| POST | `/api/videos/upload` | 上传视频（multipart，**需登录**） |
| PUT/DELETE | `/api/videos/{id}` | 修改 / 删除视频（**需登录且仅限作者**） |
| GET | `/api/video-comments/{videoId}` | 某视频的评论树（公开，带 `mine` 标记） |
| POST | `/api/video-comments` | 发表评论 / 回复（**需登录**，昵称取当前用户） |
| DELETE | `/api/video-comments/{commentId}` | 删除评论（**需登录且仅限作者**） |
| GET | `/api/knowledge/documents` | 已入库资料列表 |
| POST | `/api/knowledge/documents` | 新增资料入库（**需登录**） |
| DELETE | `/api/knowledge/documents/{docId}` | 删除单份资料（**需登录**） |
| DELETE | `/api/knowledge/documents` | 清空知识库（**需登录**） |
| GET | `/api/knowledge/search?q=&topK=` | 向量召回调试（看检索到了什么） |
| POST | `/api/knowledge/ask` | RAG 问答，返回答案 + 引用来源 |
| GET | `/api/files/**` | 上传的视频与封面静态访问 |

## 登录 / 注册与资源归属

项目有账号体系，**身份由服务端判定**：

- 服务端在浏览器首次访问时签发一个随机会话 ID，写进 **HttpOnly Cookie**（`dcar_sid`，见 `SessionFilter`）；前端 JS 读不到、也改不了。
- 登录成功后，服务端在内存里建立 `sessionId → userId` 映射（`AuthService`）。**Cookie 里只放随机 ID、不放 userId**，所以改 Cookie 无法伪造身份。
- 账号持久化在 **H2 文件库**（`./data/myaichat.mv.db`，重启不丢）；密码用 `PBKDF2WithHmacSHA256` 加盐哈希（`PasswordHasher`），只存哈希、不存明文。
- 视频、评论登记 `ownerId`（= 用户 ID，用 `@JsonIgnore` 保证不返回前端）：**上传视频 / 发表评论要求登录**，修改 / 删除只允许作者本人，越权返回 **403**、未登录返回 **401**。列表 / 详情带 `mine` 字段，前端据此决定是否显示编辑 / 删除按钮。
- 评论者昵称统一取登录用户的 `displayName`，不再信任前端传的昵称（否则可冒充他人）。
- AI 对话记忆：登录用户按 `user-{id}` 隔离，未登录按匿名会话隔离。

> 想换成 JWT / OAuth / MySQL 时，只需替换 `AuthService` 与 `UserRepository` 的实现，Controller 与前端不用动。

## RAG 知识库

流程：

```
入库：资料文本 → 切块 → 向量化 → 存入向量库（并落盘）
问答：问题 → 向量化 → 召回 recallK 条 → 重排精选 topK → 拼进 Prompt → 模型作答（附引用）
```

关键取舍：

- **DeepSeek 没有 embedding 接口**，所以向量化单独配（默认 Ollama `bge-m3`，中文效果好）；换成任何 OpenAI 兼容服务只改 `RAG_EMBEDDING_*` 环境变量。
- 向量库 = **内存暴力余弦检索 + JSON 落盘**（`InMemoryVectorStore`）：零新增依赖、几千块毫秒级；索引默认存 `./data/rag-vectors.json`，**重启不丢**。数据量大了换 PGVector / Milvus，方法签名不变。
- **重排（Rerank）**：先向量召回 `recall-k` 条，再用「向量分 × 0.7 + 中文词面匹配 × 0.3」混排取 `top-k`。向量擅长语义，但对型号 / 数字不敏感；混入词面后问"SU7 续航"会明显更准。
- RAG 用独立的 `ragChatClient`，**不带会话记忆与 MCP 工具**，避免和历史对话串味；System Prompt 强制"只依据资料、没有就说没有、标注 [1][2]"。

可调参数（`application.yml` 的 `app.rag.*`）：

| 参数 | 作用 |
| --- | --- |
| `chunk-size` / `chunk-overlap` | 切块大小与重叠：决定召回"粒度"，太碎丢语义、太大稀释相关性 |
| `recall-k` | 重排前的候选数量：宁滥勿缺，靠后面的重排精选 |
| `rerank-enabled` / `rerank-vector-weight` | 重排开关与向量分权重（其余给词面匹配） |
| `top-k` | 最终喂给模型的资料条数：越大信息越全，但噪音与 token 成本越高 |
| `similarity-threshold` | 相似度下限：低于它的候选直接丢弃，防止"硬凑"不相关内容 |
| `max-context-chars` | 拼进 Prompt 的资料总字数上限：控制 token 成本与上下文长度 |
| `max-document-chars` | 单份资料字数上限：防止一篇长文把服务打挂 |
| `store-path` | 向量索引落盘路径；设为 `none` 则只在内存 |
| `embedding.base-url / model / batch-size` | 向量化服务地址、模型、单请求批量（超出自动分批） |

代码位置：`com.hkd.rag`（`TextChunker` 切块 / `EmbeddingClient` 向量化+分批 / `InMemoryVectorStore` 检索+落盘 / `RagService` 召回+重排+问答），接口见 `KnowledgeController`。
## 数据存储说明

- **用户账号**：存 H2 文件库（`springDemo/data/myaichat.mv.db`），重启保留；表结构见 `springDemo/src/main/resources/schema.sql`。
- **视频 / 评论**：仍在**内存**里，文件存在 `springDemo/uploads` 目录；重启后列表会清空，但已上传的文件仍在磁盘上。要落库时替换 `VideoServiceImpl` / `CommentServiceImpl` 里的 `Map` 存储即可，接口与前端都不用改。

`uploads/` 与 `data/` 已加入 `.gitignore`。

## 已知限制与后续计划

- 视频 / 评论仍是内存存储（账号已落库）：重启后视频与评论会清空，但账号保留；后续可照 `UserRepository` 的方式把它们也迁到数据库。
- 没有 CSRF 防护、登录失败限流与验证码：生产环境建议补上 CSRF Token + 登录失败次数限制。
- 聊天记忆存在内存里且按会话累积，生产环境建议换成 Redis / JDBC 版 `ChatMemoryRepository`。
- MCP 模块会暴露发邮件工具，服务默认只监听 `127.0.0.1`；确需对外时请自行加鉴权。
- 仓库已 `git init`，当前提交历史从一次初始快照开始，后续建议按功能拆分提交（feat / fix / docs）。
- 前端目前没有单元测试与 ESLint，可按需补充 Vitest + ESLint。