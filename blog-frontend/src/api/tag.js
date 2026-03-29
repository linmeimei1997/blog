import request from '@/utils/request'

export const getTagList = () => {
  return request.get('/tag/list')
}

export const createTag = (data) => {
  return request.post('/tag', data)
}

export const updateTag = (id, data) => {
  return request.put(`/tag/${id}`, data)
}

export const deleteTag = (id) => {
  return request.delete(`/tag/${id}`)
}
