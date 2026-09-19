/** 时间与体积格式化：只做纯计算，方便复用与测试 */

/** 后端返回的 LocalDateTime 形如 2026-09-18T15:04:05，直接 new Date 在部分浏览器会 NaN */
export const toDate = (value) => {
  if (!value) return null
  if (value instanceof Date) return value
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const pad = (num) => String(num).padStart(2, '0')

/** 绝对时间：2026-09-18 15:04 */
export const formatDateTime = (value) => {
  const date = toDate(value)
  if (!date) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 相对时间：刚刚 / 5分钟前 / 3小时前 / 2天前 / 具体日期 */
export const timeAgo = (value) => {
  const date = toDate(value)
  if (!date) return ''
  const diff = Date.now() - date.getTime()
  if (diff < 0) return formatDateTime(date)
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  return formatDateTime(date).slice(0, 10)
}

/** 秒 -> 12:34 / 1:02:03 */
export const formatDuration = (seconds) => {
  if (!Number.isFinite(seconds) || seconds <= 0) return ''
  const total = Math.floor(seconds)
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`
}

/** 字节 -> 1.2 MB */
export const formatSize = (bytes) => {
  if (!Number.isFinite(bytes) || bytes <= 0) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value >= 10 || index === 0 ? Math.round(value) : value.toFixed(1)} ${units[index]}`
}