import http from './http'

/** 当前登录用户；未登录时后端返回 data:null，silent 避免刚进页面就弹提示 */
export const fetchCurrentUser = () => http.get('/auth/me', { silent: true })

/** 登录 */
export const login = (payload) => http.post('/auth/login', payload)

/** 注册（后端注册成功后会自动登录） */
export const register = (payload) => http.post('/auth/register', payload)

/** 退出登录 */
export const logout = () => http.post('/auth/logout', { silent: true })