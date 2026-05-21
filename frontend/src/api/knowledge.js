import { request } from '@/utils/request'

/**
 * 知识库相关 API
 */
export const knowledgeApi = {
  /**
   * 获取知识库列表
   */
  getBases(params) {
    return request.get('/knowledge/bases', params)
  },

  /**
   * 创建知识库
   */
  createBase(data) {
    return request.post('/knowledge/bases', data)
  },

  /**
   * 更新知识库
   */
  updateBase(id, data) {
    return request.put(`/knowledge/bases/${id}`, data)
  },

  /**
   * 删除知识库
   */
  deleteBase(id) {
    return request.delete(`/knowledge/bases/${id}`)
  },

  /**
   * 获取文档列表
   */
  getDocuments(params) {
    return request.get('/knowledge/documents', params)
  },

  /**
   * 创建文档
   */
  saveDocument(data) {
    return request.post('/knowledge/document/save', data)
  },

  /**
   * 更新文档
   */
  updateDocument(id, data) {
    return request.put(`/knowledge/documents/${id}`, data)
  },

  /**
   * 删除文档
   */
  deleteDocument(id) {
    return request.delete(`/knowledge/documents/${id}`)
  },

  /**
   * 获取文档分片
   */
  getChunks(docId) {
    return request.get(`/knowledge/documents/${docId}/chunks`)
  },

  /**
   * 获取标签列表
   */
  getTags(baseId) {
    return request.get(`/knowledge/tags/${baseId}`)
  },

  /**
   * 创建标签
   */
  createTag(data) {
    return request.post('/knowledge/tags', data)
  },

  /**
   * 更新标签
   */
  updateTag(id, data) {
    return request.put(`/knowledge/tags/${id}`, data)
  },

  /**
   * 删除标签
   */
  deleteTag(id) {
    return request.delete(`/knowledge/tags/${id}`)
  },

  /**
   * 上传文件
   */
  uploadFile(formData) {
    return request.upload('/file/upload', formData)
  },

  /**
   * RAG 检索
   */
  search(query, config) {
    return request.post('/knowledge/search', { query, ...config })
  },

  /**
   * RAG 知识库问答 - 流式响应（SSE）
   * @param {string} query - 查询内容
   * @param {string|null} knowledgeBaseId - 知识库ID
   * @param {number} maxResults - 最大结果数
   * @returns {string} SSE URL
   */
  ragChatStreamUrl(query, knowledgeBaseId, maxResults = 5) {
    const params = new URLSearchParams({
      query,
      maxResults: String(maxResults)
    })
    if (knowledgeBaseId) {
      params.append('knowledgeBaseId', knowledgeBaseId)
    }
    const token = localStorage.getItem('token') || ''
    if (token) {
      params.append('token', token)
    }
    return `/api/v0/knowledge/rag/chat/stream?${params}`
  }
}

export default knowledgeApi
