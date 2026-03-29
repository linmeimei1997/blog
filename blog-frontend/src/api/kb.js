import request from '@/utils/request'

export const getKbList = (params) => {
  return request.get('/kb/list', { params })
}

export const getKbById = (id) => {
  return request.get(`/kb/${id}`)
}

export const uploadKb = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/kb/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const deleteKb = (id) => {
  return request.delete(`/kb/${id}`)
}

export const searchKb = (keyword) => {
  return request.get('/kb/search', { params: { keyword } })
}

// 导出文档为指定格式 (word, pdf, txt, md)
export const exportKb = (id, format) => {
  return `/api/kb/${id}/export?format=${format}`
}