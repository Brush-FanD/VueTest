# 懂车帝风格 AI 汽车助手

一个前后端分离的汽车内容 + AI 问答项目：前端是移动端 H5（首页信息流、选车、视频库、视频详情），后端用 Spring AI 接入 DeepSeek，另有一个把本地工具以 MCP 协议暴露出去的示范模块。

## 目录结构

```
VueTest/
├── myaichat/myaichat/      # 前端：Vue 3 + Vite
└── springAI/springAI/      # 后端：Maven 多模块
    ├── springDemo/         # 主服务：8123，提供 AI 对话 + 视频/评论接口
    └── ChatMcpModel/       # MCP Server：8082，暴露数学/时间/邮件工具
```

## 技术栈

| 端 | 技术 |
| --- | --- |
| 前端 | Vue 3.5、Vite 8、Vue Router 4、Element Plus、axios |
| 后端 | Java 17、Spring Boot 3.5、Spring AI 1.0.7、SpringDoc OpenAPI |
| 模型 | DeepSeek（通过 OpenAI 兼容协议调用） |

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

## 接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/web/aichat` | 同步对话，入参 `{ message, conversationId }` |
| GET | `/api/web/chatstream` | 流式对话（SSE），结束时下发 `[COMPLETE]` |
| DELETE | `/api/web/aichat/memory` | 清空指定会话的上下文 |
| GET/POST/PUT/DELETE | `/api/videos`、`/api/videos/upload`、`/api/videos/{id}` | 视频库增删改查（上传为 multipart） |
| GET/POST/DELETE | `/api/video-comments/{videoId}`、`/api/video-comments` | 视频评论与回复 |
| GET | `/api/files/**` | 上传的视频与封面静态访问 |

## 数据存储说明

视频与评论目前存在**内存**里、文件存在 `springDemo/uploads` 目录：重启后列表会清空，但已上传的文件仍在磁盘上。要落库时只需替换 `VideoServiceImpl` / `CommentServiceImpl` 里的 `Map` 存储，接口和前端都不用动。

`uploads/` 已加入 `.gitignore`。

## 已知限制与后续计划

- 评论没有登录态，昵称由前端在 localStorage 生成；接入用户体系后需要加上作者校验。
- 聊天记忆存在内存里且按会话累积，生产环境建议换成 Redis / JDBC 版 `ChatMemoryRepository`。
- MCP 模块会暴露发邮件工具，服务默认只监听 `127.0.0.1`；确需对外时请自行加鉴权。
- 仓库已 `git init`，当前提交历史从一次初始快照开始，后续建议按功能拆分提交（feat / fix / docs）。
- 前端目前没有单元测试与 ESLint，可按需补充 Vitest + ESLint。
