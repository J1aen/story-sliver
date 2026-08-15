package com.storysliver.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.storysliver.auth.CommentRateLimiter;
import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.CommentMapper;
import com.storysliver.mapper.CommentReportMapper;
import com.storysliver.mapper.StoryFragmentMapper;
import com.storysliver.mapper.UserMapper;
import com.storysliver.pojo.Comment;
import com.storysliver.pojo.CommentReport;
import com.storysliver.pojo.CommentVO;
import com.storysliver.pojo.PageBean;
import com.storysliver.pojo.StoryFragment;
import com.storysliver.pojo.User;
import com.storysliver.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现（v2.0 Task 21）。
 * 核心规则：
 *   - 发评论：内容非空且 ≤100 字 → 碎片存在且已发布 → 限频 1 分钟 10 条（Q8）→ 免审核落库
 *   - 列表：只查正常评论，时间正序（Q5），PageHelper 分页
 *   - 删除：本人才能删自己的评论（Q7，硬删除）；不存在时幂等
 *   - 举报：理由必填（Q6）、评论必须存在、同一人不能重复举报
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;//评论表操作（增删查）

    @Autowired
    private CommentReportMapper reportMapper;//举报表操作

    @Autowired
    private UserMapper userMapper;//查评论者昵称/头像/角色

    @Autowired
    private StoryFragmentMapper fragmentMapper;//校验碎片存在且已发布

    @Autowired
    private CommentRateLimiter commentRateLimiter;//评论限频（Q8：1 分钟 10 条）

    @Override
    public void add(Long userId, Long fragmentId, String content) {
        // 先判空再 trim：null 会空指针，先转成空串再统一处理
        content = content == null ? "" : content.trim();
        if (content.isEmpty()) {// 空内容（含全空格）直接拒绝
            throw new BusinessException(ResultCode.COMMENT_EMPTY);
        }
        if (content.length() > 100) {// 100 字上限（Q5），超长拒绝
            throw new BusinessException(ResultCode.COMMENT_TOO_LONG);
        }
        // 先查碎片：不存在或还没上架（待审核/隐藏）都拒绝，防止挂空评论或评论不可见内容
        StoryFragment fragment = fragmentMapper.selectById(fragmentId);
        if (fragment == null || fragment.getStatus() != StoryFragment.STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.FRAGMENT_NOT_PUBLISHED);
        }
        // 限频（Q8）：1 分钟 10 条，超出拒绝；放在内容校验之后，非法请求不占用评论额度
        if (!commentRateLimiter.tryAcquire(userId)) {
            throw new BusinessException(ResultCode.COMMENT_TOO_FREQUENT);
        }
        // 组装评论对象并落库：免审核，发布即显示
        Comment c = new Comment();
        c.setFragmentId(fragmentId);// 所属碎片
        c.setUserId(userId);// 评论者
        c.setContent(content);// 内容（已 trim）
        commentMapper.insert(c);// status 由数据库默认 0（正常）
    }

    @Override
    public PageBean listByFragment(Long userId, Long fragmentId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);// 开启分页：下一条查询自动拼 LIMIT
        List<Comment> list = commentMapper.selectByFragment(fragmentId);// 按碎片查评论（时间正序）
        Page<Comment> page = (Page<Comment>) list;// PageHelper 把结果包装成 Page，带 total

        // 实体 → VO：拼昵称/头像/角色，标记「是否自己的评论」，格式化时间
        List<CommentVO> vos = list.stream().map(c -> {
            CommentVO vo = new CommentVO();// 新建展示对象
            vo.setId(c.getId());// 评论 id
            vo.setContent(c.getContent());// 内容
            User author = userMapper.selectById(c.getUserId());// 查评论者（昵称/头像/角色）
            vo.setAuthorName(author == null ? "已注销" : author.getNickname());// 昵称（用户被删则显示已注销）
            vo.setAuthorAvatar(author == null ? null : author.getAvatar());// 头像（可空）
            vo.setAuthorUserId(author == null ? null : author.getId());// 评论者 id（跳转他人主页用）
            vo.setAuthorRole(author == null ? null : author.getRole());// 角色（前端显示铭牌）
            vo.setMine(userId != null && userId.equals(c.getUserId()));// 是否自己的评论（游客恒为 false）
            vo.setCreatedAt(c.getCreatedAt() == null ? null : c.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));// 时间转前端好读格式
            return vo;// 返回组装好的 VO
        }).collect(Collectors.toList());// 收集成本页列表

        return new PageBean(page.getTotal(), vos);// total 给前端判断还有没有下一页
    }

    @Override
    public void deleteOwn(Long userId, Long commentId) {
        Comment c = commentMapper.selectById(commentId);// 先查评论
        if (c == null) {// 不存在（已被删）：幂等返回，不报错也不重复删
            return;
        }
        if (!c.getUserId().equals(userId)) {// 不是自己的评论不能删
            throw new BusinessException(ResultCode.NOT_AUTHOR);
        }
        commentMapper.deleteById(commentId);// 硬删除（Q7：弹窗确认，不可恢复）
    }

    @Override
    public void report(Long userId, Long commentId, String reason) {
        reason = reason == null ? "" : reason.trim();// 理由去空格，null 转空串
        if (reason.isEmpty()) {// 理由必填（Q6）
            throw new BusinessException(ResultCode.REPORT_REASON_REQUIRED);
        }
        if (commentMapper.selectById(commentId) == null) {// 评论不存在不能举报
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (reportMapper.selectByCommentAndReporter(commentId, userId) != null) {// 同一人重复举报拦截
            throw new BusinessException(ResultCode.REPORT_DUPLICATE);
        }
        CommentReport r = new CommentReport();// 组装举报记录
        r.setCommentId(commentId);// 被举报评论
        r.setReporterId(userId);// 举报人
        r.setReason(reason);// 理由
        reportMapper.insert(r);// 落库，进入待处理队列
    }

    @Override
    public List<CommentReport> listPendingReports() {
        return reportMapper.selectPending();// 直接返回待处理列表（已 join 好展示字段）
    }

    @Override
    public CommentReport getReport(Long reportId) {
        CommentReport r = reportMapper.selectById(reportId);// 查举报
        if (r == null) {// 不存在：抛 404，防止处理不存在的举报
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        return r;
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        commentMapper.deleteById(commentId);// 管理端下架=硬删除
    }

    @Override
    public void markReportHandled(Long reportId) {
        reportMapper.markHandled(reportId);// 标记举报已处理（写状态和处理时间）
    }
}
