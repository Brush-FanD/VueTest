const NICKNAME_KEY = 'aichat:nickname'

/**
 * 未登录场景的本地昵称：生成一次后存在 localStorage，让评论能区分不同用户。
 */
export const localNickname = () => {
  let nickname = localStorage.getItem(NICKNAME_KEY)
  if (!nickname) {
    nickname = `车友${Math.floor(1000 + Math.random() * 9000)}`
    localStorage.setItem(NICKNAME_KEY, nickname)
  }
  return nickname
}
