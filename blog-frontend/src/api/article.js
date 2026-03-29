import request from '@/utils/request'

export const getArticleList = (params) => {
  return request.get('/article/list', { params })
}

export const getArticleById = (id) => {
  return request.get(`/article/${id}`)
}

export const createArticle = (data) => {
  return request.post('/article', data)
}

export const updateArticle = (id, data) => {
  return request.put(`/article/${id}`, data)
}

export const deleteArticle = (id) => {
  return request.delete(`/article/${id}`)
}

export const getLatestArticles = (limit = 5) => {
  return request.get('/article/latest', { params: { limit } })
}

// 导出文章为指定格式 (word, pdf, txt, md)
export const exportArticle = (id, format) => {
  return `/api/article/${id}/export?format=${format}`
}
