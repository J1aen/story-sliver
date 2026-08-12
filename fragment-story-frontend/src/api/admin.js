import request from './request'

// 管理列表 / 审核队列（status：0 待审核 / 1 已发布 / 不传全部）
export const getAdminFragments = (status, pageNum, pageSize) => request.get('/admin/fragments', { params: { status, pageNum, pageSize } })
export const approveFragment = (id) => request.post(`/admin/fragments/${id}/approve`)
export const adminDeleteFragment = (id) => request.delete(`/admin/fragments/${id}`)

// 头像审核队列
export const getPendingAvatars = (pageNum, pageSize) => request.get('/admin/avatars', { params: { pageNum, pageSize } })
export const approveAvatar = (userId) => request.post(`/admin/avatars/${userId}/approve`)
export const rejectAvatar = (userId) => request.delete(`/admin/avatars/${userId}/reject`)

// 用户管理（仅站长）
export const getUsers = (pageNum, pageSize) => request.get('/admin/users', { params: { pageNum, pageSize } })
export const updateUserRole = (id, role) => request.put(`/admin/users/${id}/role`, { role })

// 系统配置（仅站长）
export const updateAdminCode = (newCode) => request.put('/admin/config/admin-register-code', { newCode })
