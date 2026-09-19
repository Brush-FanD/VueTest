import http from './http'

/** 某个视频下的评论（含回复） */
export const listComments = (videoId) => http.get(`/video-comments/${videoId}`)

/** 发表评论或回复，参数：{ videoId, parentCommentId, comment, commenter } */
export const addComment = (payload) => http.post('/video-comments', payload)

/** 删除评论 */
export const removeComment = (commentId) => http.delete(`/video-comments/${commentId}`)
