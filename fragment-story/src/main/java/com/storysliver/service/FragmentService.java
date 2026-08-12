package com.storysliver.service;

/**
 * 碎片服务接口（Task 7，待实现）。
 * 方法（签名参考实现计划 Task 7）：
 *   - submit(Long userId, String content, boolean anonymous) 发布（进入待审核）
 *   - list(int pageNum, int pageSize, Long currentUserId) 首页分页列表（只返回已发布，返回 PageBean）
 *   - my(Long userId) 我的碎片（全部状态，含匿名）
 *   - like / unlike 点赞与取消（每人每条一次）
 *   - hide / unhide 隐藏与取消隐藏（仅作者）
 *   - deleteByAuthor 作者硬删除（删行并清理点赞）
 * 为什么定义接口：Controller 依赖接口，测试用 Mockito 打桩方便。
 */
public interface FragmentService {
}
