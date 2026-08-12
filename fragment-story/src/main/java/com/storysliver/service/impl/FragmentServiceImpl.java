package com.storysliver.service.impl;

import com.storysliver.service.FragmentService;

/**
 * 碎片服务实现（Task 7，待实现）。
 * 需要 @Autowired 的依赖：
 *   - StoryFragmentMapper 碎片表操作
 *   - FragmentLikeMapper 点赞表操作
 *   - UserMapper 查昵称（拼 authorName）
 *   - FragmentRateLimiter 发布限频（5 分钟/条）
 * 核心逻辑要点：
 *   - 发布：先限频、再校验 1000 字，status=0（待审核）落库
 *   - 列表：只查 status=1，PageHelper 分页，匿名显示「匿名用户」
 *   - 点赞/取消：先查已发布，再操作点赞表和 like_count
 *   - 隐藏/取消隐藏、作者删除：先校验是作者本人
 */
public class FragmentServiceImpl implements FragmentService {
}
