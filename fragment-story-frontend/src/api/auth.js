import request from './request'

export const getCaptcha = () => request.get('/auth/captcha')
export const register = (data) => request.post('/auth/register', data)
export const login = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/users/me')
// 上传头像（multipart，进入待审核）
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
