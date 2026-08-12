package com.storysliver.service;

import com.storysliver.pojo.Fragment.FragmentVO;
import com.storysliver.pojo.PageBean;
import com.storysliver.pojo.StoryFragment;

import java.util.List;

/**
 * 碎片服务接口：发布、列表、点赞、隐藏、删除。
 * 为什么定义接口：Controller 依赖接口，测试用 Mockito 打桩方便。
 */
public interface FragmentService {

    /** 发布碎片：先限频再校验，落库时 status=0（待审核），返回刚插入的碎片（含回填 id） */
    StoryFragment submit(Long userId, String content, boolean anonymous);

    /** 首页分页列表：只返回已发布（status=1），PageHelper 分页；currentUserId 为 null 表示游客 */
    PageBean list(int pageNum, int pageSize, Long currentUserId);

    /** 我的碎片：返回自己全部状态（待审核/已发布/已隐藏，含匿名的） */
    List<FragmentVO> my(Long userId);

    /** 点赞：只能赞已发布，每人每条一次 */
    void like(Long userId, Long fragmentId);

    /** 取消点赞：先确认赞过，再删记录并减计数 */
    void unlike(Long userId, Long fragmentId);

    /** 隐藏：仅作者，且只允许隐藏已发布（1→2） */
    void hide(Long userId, Long fragmentId);

    /** 取消隐藏：仅作者，且只允许恢复已隐藏（2→1） */
    void unhide(Long userId, Long fragmentId);

    /** 作者硬删除：删行并清理该碎片全部点赞（前端弹窗确认不可恢复） */
    void deleteByAuthor(Long userId, Long fragmentId);

    /** 实体转 VO：匿名显示「匿名用户」、实名查昵称；管理端也要复用这个方法 */
    FragmentVO toVO(StoryFragment fragment, boolean likedByMe);
}
