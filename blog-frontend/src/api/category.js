import request from '@/utils/request'

export const getCategoryList = () => {
  return request.get('/category/list')
}

export const createCategory = (data) => {
  return request.post('/category', data)
}

export const updateCategory = (id, data) => {
  return request.put(`/category/${id}`, data)
}

export const deleteCategory = (id) => {
  return request.delete(`/category/${id}`)
}
