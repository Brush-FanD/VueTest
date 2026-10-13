import http from './http'

/** 已入库的资料列表 */
export const listDocuments = () => http.get('/knowledge/documents')

/** 新增资料（需登录） */
export const addDocument = (payload) => http.post('/knowledge/documents', payload)

/** 删除单份资料（需登录） */
export const removeDocument = (docId) => http.delete(`/knowledge/documents/${docId}`)

/** 清空知识库（需登录） */
export const clearDocuments = () => http.delete('/knowledge/documents')

/** 只做向量召回，调试检索效果 */
export const searchKnowledge = (q, topK) => http.get('/knowledge/search', { params: { q, topK } })

/** 知识库问答：返回答案 + 引用来源 */
export const askKnowledge = (question) => http.post('/knowledge/ask', { question })