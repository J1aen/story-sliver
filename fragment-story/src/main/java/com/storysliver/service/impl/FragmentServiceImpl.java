package com.storysliver.service.impl;

import com.storysliver.auth.FragmentRateLimiter;
import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.FragmentLikeMapper;
import com.storysliver.mapper.StoryFragmentMapper;
import com.storysliver.mapper.UserMapper;
import com.storysliver.pojo.Fragment.FragmentVO;
import com.storysliver.pojo.FragmentLike;
import com.storysliver.pojo.PageBean;
import com.storysliver.pojo.StoryFragment;
import com.storysliver.pojo.User;
import com.storysliver.service.FragmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 碎片服务实现（Task 7，进行中：submit / list / my / toVO 已实现）。
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
@Service
public class FragmentServiceImpl implements FragmentService {

    @Autowired
    private FragmentRateLimiter fragmentRateLimiter;//发布限频（5 分钟/条）

    @Autowired
    private StoryFragmentMapper fragmentMapper;//碎片表操作（增删改查）

    @Autowired
    private FragmentLikeMapper likeMapper;//点赞表操作（已赞查询/点赞记录）

    @Autowired
    private UserMapper userMapper;//用户表操作（查昵称拼 authorName）


//  发布碎片：进入待审核状态
    @Override

    public StoryFragment submit(Long userId, String content, boolean anonymous) {
//         如果是空的话text=""   如果不是空就去掉头尾空格
        String text = content == null ? "" : content.trim();
//         判断如果是空或者文本长度大于1000字 就会抛出异常
        if (text.isEmpty() || text.length() > 1000){
            throw new BusinessException(ResultCode.FRAGMENT_TOO_LONG);
        }
//        判断用户是否在 5 分钟内发布过碎片
        if (!fragmentRateLimiter.tryAcquire(userId)) {
            throw new BusinessException(ResultCode.FRAGMENT_TOO_FREQUENT);
        }
//         创建碎片对象
        StoryFragment f = new StoryFragment();
        f.setUserId(userId); // 用户真实ID
        f.setContent(text); // 碎片内容
        f.setLikeCount(0);  // 默认点赞数为 0
        f.setIsAnonymous(anonymous ? 1 : 0);  // 匿名则为 1，否则为 0
        f.setStatus(StoryFragment.STATUS_PENDING); // 设置为待审核状态
        // insert 返回的是「影响的行数」int，不是碎片对象；
        // @Options(useGeneratedKeys=true) 会把自增 id 回填到 f 上，所以返回 f 本身
        fragmentMapper.insert(f);
        return f;
    }

    /**
     * 首页分页列表：只返回已发布（status=1）的碎片，按时间倒序。
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页条数
     * @param currentUserId 当前登录用户 id；游客为 null（不标记「已赞」）
     * @return PageBean{total, list}，list 里是 FragmentVO
     * 为什么用 PageHelper：startPage 之后紧跟着的那条查询会自动拼 LIMIT，
     * 并把结果包装成 Page 对象（里面带 total 总条数），不用自己数总数。
     */
    @Override
    public PageBean list(int pageNum, int pageSize, Long currentUserId) {
        // PageHelper：查询前调用 startPage，它会给下一条查询自动加 LIMIT 分页
        PageHelper.startPage(pageNum, pageSize);

        // 只查已发布（status=1）的碎片；分页参数由 PageHelper 注入，方法不用传 offset/limit
        List<StoryFragment> fragments = fragmentMapper.selectPublishedPage();

        // PageHelper 把结果包装成 Page：getTotal() 是总条数，getResult() 是本页数据
        Page<StoryFragment> page = (Page<StoryFragment>) fragments;

        // 登录用户：一次查出他赞过的所有碎片 id，用来标记 likedByMe
        // 为什么一次查完而不是逐条查：避免 N+1 查询（查 10 条碎片就多 10 次数据库请求）
        List<Long> likedIds = currentUserId == null ? List.of() : likeMapper.selectLikedFragmentIds(currentUserId);

        // 实体 → 展示对象：匿名显示「匿名用户」，实名查昵称
        List<FragmentVO> vos = page.getResult().stream()
                .map(f -> toVO(f, likedIds.contains(f.getId())))
                .toList();

        // total 给前端判断「还有没有下一页」，list 是本页数据
        return new PageBean(page.getTotal(), vos);
    }

    /**
     * 碎片实体 → 展示对象。
     * @param f 碎片实体
     * @param likedByMe 当前登录用户是否已赞
     * @return FragmentVO（不暴露 user_id 等敏感字段）
     * 匿名逻辑：isAnonymous==1 对外显示「匿名用户」，但数据库仍记录真实发布者（管理员可追溯）
     */
    @Override
    public FragmentVO toVO(StoryFragment f, boolean likedByMe) {
        FragmentVO vo = new FragmentVO();
        vo.setId(f.getId());
        vo.setContent(f.getContent());
        vo.setLikeCount(f.getLikeCount());
        vo.setIsAnonymous(f.getIsAnonymous());
        vo.setStatus(f.getStatus());
        vo.setLikedByMe(likedByMe);
        // 时间转成前端好读的格式
        vo.setCreatedAt(f.getCreatedAt() == null
                ? ""
                : f.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 匿名 → 「匿名用户」；实名 → 查一次用户表拿昵称
        if (f.getIsAnonymous() == 1) {
            vo.setAuthorName("匿名用户");
            vo.setAuthorRole(null);//匿名不暴露身份，也没有铭牌
        } else {
            User author = userMapper.selectById(f.getUserId());
            vo.setAuthorName(author == null ? "未知用户" : author.getNickname());
            // 带上作者角色：前端给站长/管理员显示不同铭牌（需求：非匿名碎片显示身份标识）
            vo.setAuthorRole(author == null ? null : author.getRole());
            // 带上作者已审核头像：非匿名碎片作者旁显示小圆头像
            vo.setAuthorAvatar(author == null ? null : author.getAvatar());
        }
        return vo;
    }

//    我的碎片
    @Override

    public List<FragmentVO> my(Long userId) {
//        返回用户发布的所有碎片
        return fragmentMapper.selectByUser(userId).stream() //把 List 变成「流」，方便对每条数据逐个处理
                .map(f -> toVO(f, false)) //对每条碎片 f 调 toVO 转成展示对象；第二个参数 false 表示「不标记已赞」
                .toList(); //处理完再收集回 List
    }

    /**
     * 点赞：只能赞已发布（status=1）的碎片，每人每条一次。
     * 为什么先查再赞：确保点赞的对象存在且已发布，防止给不存在的碎片点赞。
     */
    @Override
    public void like(Long userId, Long fragmentId) {
        requirePublished(fragmentId);// 前置校验：碎片存在且已发布

        // 已赞过就拒绝（数据库唯一约束也会兜底，这里先友好拦截）
        if (likeMapper.exists(fragmentId, userId)) {
            throw new BusinessException(ResultCode.ALREADY_LIKED);
        }

        // 记录「谁赞了哪条」
        FragmentLike like = new FragmentLike();
        like.setFragmentId(fragmentId); // 碎片 id
        like.setUserId(userId); // 用户 id
        likeMapper.insert(like); //插入点赞记录

        // 冗余计数 +1（首页列表直接显示，不用每次 COUNT）
        fragmentMapper.increaseLikeCount(fragmentId);
    }

    /**
     * 取消点赞：先确认赞过，再删记录并把计数减回去。
     * 为什么先查 exists：防止「没赞过却要取消」时把计数减成负数。
     */
    @Override
    public void unlike(Long userId, Long fragmentId) {
        requirePublished(fragmentId);

        if (!likeMapper.exists(fragmentId, userId)) {
            throw new BusinessException(ResultCode.NOT_LIKED);
        }

        likeMapper.deleteByFragmentAndUser(fragmentId, userId);
        fragmentMapper.decreaseLikeCount(fragmentId);
    }

    /**
     * 隐藏：仅作者本人能操作，且只允许隐藏「已发布」的碎片（1→2）。
     * 为什么限制状态：待审核的本来就没上墙，隐藏没有意义。
     */
    @Override
    public void hide(Long userId, Long fragmentId) {
        StoryFragment f = requireOwned(userId, fragmentId);// 前置校验：是自己的碎片
        if (f.getStatus() != StoryFragment.STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FRAGMENT_NOT_PUBLISHED);
        }
        fragmentMapper.updateStatus(fragmentId, StoryFragment.STATUS_HIDDEN);
    }

    /**
     * 取消隐藏：仅作者本人，且只允许恢复「已隐藏」的碎片（2→1）。
     */
    @Override
    public void unhide(Long userId, Long fragmentId) {
        StoryFragment f = requireOwned(userId, fragmentId);
        if (f.getStatus() != StoryFragment.STATUS_HIDDEN) {
            throw new BusinessException(ResultCode.FRAGMENT_NOT_PUBLISHED);
        }
        fragmentMapper.updateStatus(fragmentId, StoryFragment.STATUS_PUBLISHED);
    }

    /**
     * 作者硬删除：删掉碎片行，并清理该碎片的所有点赞记录（前端弹窗确认「不可恢复」）。
     * 为什么把点赞也删了：避免留下指向不存在碎片的孤儿点赞记录。
     */
    @Override
    public void deleteByAuthor(Long userId, Long fragmentId) {
        requireOwned(userId, fragmentId);
        fragmentMapper.deleteById(fragmentId);
        // userId 传 null 表示「删除该碎片全部点赞」（Mapper XML 里用 <if> 处理）
        likeMapper.deleteByFragmentAndUser(fragmentId, null);
    }

    /** 校验碎片存在且已发布：点赞/取消点赞的前置条件 */
    private void requirePublished(Long fragmentId) {
        StoryFragment f = fragmentMapper.selectById(fragmentId);
        if (f == null || f.getStatus() != StoryFragment.STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FRAGMENT_NOT_PUBLISHED);
        }
    }

    /** 校验碎片存在且属于当前用户：隐藏/取消隐藏/删除的前置条件（防止越权操作别人的碎片） */
    private StoryFragment requireOwned(Long userId, Long fragmentId) {
        StoryFragment f = fragmentMapper.selectById(fragmentId);
        if (f == null || !f.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_AUTHOR);
        }
        return f;
    }



}
