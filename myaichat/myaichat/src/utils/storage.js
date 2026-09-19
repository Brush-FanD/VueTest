/**
 * 本地轻量状态：收藏、点赞、播放进度、最近搜索。
 * 这些都只影响本机体验，不写后端，避免为了展示效果去伪造服务端数据。
 */
const PREFIX = 'dcar:'

const read = (key, fallback) => {
  try {
    const raw = localStorage.getItem(PREFIX + key)
    return raw === null ? fallback : JSON.parse(raw)
  } catch {
    return fallback
  }
}

const write = (key, value) => {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value))
  } catch {
    // 隐私模式下 localStorage 可能不可写，静默降级
  }
}

/** 通用「id 集合」游标：用于收藏 / 点赞 */
const idSet = (key) => ({
  all: () => read(key, []),
  has: (id) => read(key, []).map(String).includes(String(id)),
  toggle: (id) => {
    const list = read(key, []).map(String)
    const target = String(id)
    const next = list.includes(target) ? list.filter((item) => item !== target) : [target, ...list]
    write(key, next)
    return next.includes(target)
  }
})

export const favoriteStore = idSet('favorites')
export const likeStore = idSet('likes')

/** 播放进度：只记录秒数，进入页面时从上次位置继续 */
export const progressStore = {
  get: (id) => Number(read(`progress:${id}`, 0)) || 0,
  set: (id, seconds) => write(`progress:${id}`, Math.max(0, Math.floor(seconds || 0))),
  clear: (id) => write(`progress:${id}`, 0)
}

/** 最近搜索词，最多保留 8 条 */
export const historyStore = {
  all: () => read('search-history', []),
  push: (keyword) => {
    const word = String(keyword || '').trim()
    if (!word) return
    const next = [word, ...read('search-history', []).filter((item) => item !== word)].slice(0, 8)
    write('search-history', next)
  },
  clear: () => write('search-history', [])
}