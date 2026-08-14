import request from './request'

// 获取当前上架公告（公开接口，游客也能调；没有上架公告时返回 null）
export const getActiveAnnouncement = () => request.get('/announcements/active')
