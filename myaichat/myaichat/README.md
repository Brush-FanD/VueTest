# 懂车帝风格 H5（前端）

移动端页面（`max-width: 480px`），包含首页信息流、AI 问答、选车、视频库与视频详情。

## 启动

```bash
npm install
npm run dev      # http://localhost:5000
npm run build    # 产物在 dist/
```

后端需先启动在 8123 端口（见仓库根目录 README），开发服务器会把 `/api` 代理过去。

## 环境变量

复制 `.env.example` 为 `.env.local` 后按需修改：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | 请求前缀 | `/api` |
| `VITE_PROXY_TARGET` | 开发代理的后端地址 | `http://localhost:8123` |
| `VITE_MEDIA_BASE_URL` | 视频/封面域名前缀，留空表示同源 | 空 |

## 目录结构

```
src/
├── api/          # 请求层：http 实例 + chat/video/comment 接口
├── components/   # AiChat（AI 助手页）、CommentItem（递归评论）
├── composables/  # useChat：同步/流式对话逻辑
├── data/         # 尚未接后端的 mock 数据（车型、信息流）
├── utils/        # mediaUrl（媒体地址拼接）、nickname（本地昵称）
├── views/        # 页面
└── router.js     # 路由（懒加载 + 404 + 标题）
```

## 路由

| 路径 | 页面 |
| --- | --- |
| `/home` | 首页信息流 + AI 弹层 |
| `/cars`、`/car/:carId` | 选车、车型详情 |
| `/videos`、`/video/:videoId` | 视频库、视频详情与评论 |
| `/ai` | 懂车AI 助手（支持 `?q=` 直接提问） |
| 其他 | 404 页面 |
