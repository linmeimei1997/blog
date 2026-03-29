import request from '@/utils/request'

// 获取用户资料
export const getProfile = () => {
  return request.get('/user/profile')
}

// 更新用户资料
export const updateProfile = (data) => {
  return request.put('/user/profile', data)
}

// 上传头像
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 修改密码
export const changePassword = (data) => {
  return request.put('/user/password', data)
}
