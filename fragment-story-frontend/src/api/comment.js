import request from './request'

// —— 评论 API（v2.0 Task 21）——
// 干什么用：前端所有评论/举报接口都走这里，统一走 request（自动带 token、解包 Result）

/** 分页查某个碎片的评论（时间正序；游客可看） */
export const getComments = (fragmentId, pageNum, pageSize) =>
  request.get('/comments', { params: { fragmentId, pageNum, pageSize } })

/** 发评论（免审核，发布即显示；需登录） */
export const addComment = (fragmentId, content) =>
  request.post('/comments', { fragmentId, content })

/** 删自己的评论（硬删除，弹窗确认；需登录） */
export const deleteComment = (id) => request.delete(`/comments/${id}`)

/** 举报评论（理由必填；需登录） */
export const reportComment = (id, reason) => request.post(`/comments/${id}/report`, { reason })

/** 管理端：待处理举报列表（管理员/站长） */
export const getCommentReports = () => request.get('/admin/comments/reports')

/** 管理端：处理举报（action: dismiss 不下架 / delete 下架 / ban 下架并封禁） */
export const handleCommentReport = (id, action, banDays, banReason) =>
  request.post(`/admin/comments/reports/${id}/handle`, { action, banDays, banReason })
