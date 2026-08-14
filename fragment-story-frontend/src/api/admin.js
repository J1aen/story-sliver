import request from './request'

// 管理列表 / 审核队列（status：0 待审核 / 1 已发布 / 不传全部）
export const getAdminFragments = (status, pageNum, pageSize) => request.get('/admin/fragments', { params: { status, pageNum, pageSize } })
export const approveFragment = (id) => request.post(`/admin/fragments/${id}/approve`)
export const adminDeleteFragment = (id) => request.delete(`/admin/fragments/${id}`)

// 头像审核队列
export const getPendingAvatars = (pageNum, pageSize) => request.get('/admin/avatars', { params: { pageNum, pageSize } })
export const approveAvatar = (userId) => request.post(`/admin/avatars/${userId}/approve`)
export const rejectAvatar = (userId, reason) => request.post(`/admin/avatars/${userId}/reject`, { reason })

// 用户管理（仅站长）
export const getUsers = (pageNum, pageSize) => request.get('/admin/users', { params: { pageNum, pageSize } })
export const updateUserRole = (id, role) => request.put(`/admin/users/${id}/role`, { role })
export const banUser = (id, days, reason) => request.post(`/admin/users/${id}/ban`, { days, reason })
export const unbanUser = (id) => request.post(`/admin/users/${id}/unban`)

// 系统配置（仅站长）
export const updateAdminCode = (newCode) => request.put('/admin/config/admin-register-code', { newCode })

// 公告管理（v1.2，仅站长）
export const getAnnouncements = () => request.get('/admin/announcements')
export const createAnnouncement = (data) => request.post('/admin/announcements', data)
export const updateAnnouncement = (id, data) => request.put(`/admin/announcements/${id}`, data)
export const updateAnnouncementStatus = (id, status) => request.put(`/admin/announcements/${id}/status`, null, { params: { status } })
export const deleteAnnouncement = (id) => request.delete(`/admin/announcements/${id}`)
export const uploadAnnouncementImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/announcements/upload-image', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// 敏感词管理（v1.2，仅站长）
export const getSensitiveWords = () => request.get('/admin/sensitive-words')
export const addSensitiveWord = (word) => request.post('/admin/sensitive-words', { word })
export const deleteSensitiveWord = (id) => request.delete(`/admin/sensitive-words/${id}`)
