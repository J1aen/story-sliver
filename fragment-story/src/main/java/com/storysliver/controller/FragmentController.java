package com.storysliver.controller;

/**
 * 碎片接口（Task 7，待实现）。
 * 端点：
 *   POST   /api/fragments             发布（进入待审核，需要登录）
 *   GET    /api/fragments             首页列表（公开；带 token 时解析 likedByMe）
 *   GET    /api/fragments/my          我的碎片（需要登录）
 *   POST   /api/fragments/{id}/like   点赞（需要登录）
 *   DELETE /api/fragments/{id}/like   取消点赞（需要登录）
 *   PUT    /api/fragments/{id}/hide   隐藏（需要登录，仅作者）
 *   PUT    /api/fragments/{id}/unhide 取消隐藏（需要登录，仅作者）
 *   DELETE /api/fragments/{id}        作者硬删除（需要登录，前端弹窗确认）
 * 注意：GET /api/fragments 已在 WebConfig 排除登录拦截（游客可看），
 * 所以 Controller 里要「可选解析」token（参考实现计划 Task 7 的 optionalUserId）。
 */
public class FragmentController {
}
