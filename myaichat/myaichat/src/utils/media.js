const MEDIA_BASE_URL = (import.meta.env.VITE_MEDIA_BASE_URL || '').replace(/\/$/, '')

/**
 * 把后端返回的媒体路径（如 /api/files/video-xxx.mp4）拼成可访问地址。
 *
 * 默认走当前域名的 Vite 代理 / Nginx，因此留空即可；
 * 需要直连后端（例如前端部署在静态托管上）时在 .env 里配置 VITE_MEDIA_BASE_URL。
 */
export const mediaUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return `${MEDIA_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}
