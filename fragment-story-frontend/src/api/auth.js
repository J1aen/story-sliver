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
// 修改昵称 / 修改密码（v1.2，设置下拉里）
export const updateNickname = (nickname) => request.put('/users/nickname', { nickname })
export const updatePassword = (data) => request.put('/users/password', data)
// 他人主页（v2.0 Task 20）：公开接口，游客可看；返回用户信息 + 非匿名已发布碎片分页
export const getUserProfile = (userId, pageNum, pageSize) =>
  request.get(`/users/${userId}/profile`, { params: { pageNum, pageSize } })
