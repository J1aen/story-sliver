import request from './request'

// 首页分页列表（公开，带 token 时返回 likedByMe）
export const getFragments = (pageNum, pageSize) => request.get('/fragments', { params: { pageNum, pageSize } })
// 发布碎片（进入待审核）
export const submitFragment = (content, isAnonymous) => request.post('/fragments', { content, isAnonymous })
// 点赞 / 取消点赞
export const likeFragment = (id) => request.post(`/fragments/${id}/like`)
export const unlikeFragment = (id) => request.delete(`/fragments/${id}/like`)
// 我的碎片（个人主页统计用）
export const getMyFragments = () => request.get('/fragments/my')
// 隐藏 / 取消隐藏 / 删除
export const hideFragment = (id) => request.put(`/fragments/${id}/hide`)
export const unhideFragment = (id) => request.put(`/fragments/${id}/unhide`)
export const deleteFragment = (id) => request.delete(`/fragments/${id}`)
