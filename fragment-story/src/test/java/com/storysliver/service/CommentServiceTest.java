package com.storysliver.service;

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
import com.storysliver.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评论服务单元测试（Task 21）。
 * 验证（Mockito 模拟 Mapper/限流器）：
 *   - 发评论：空内容/超 100 字/碎片不存在或未上架/超限频都被拒；成功时内容去空格落库
 *   - 列表：返回 VO 带「是否自己的评论」、作者角色、格式化时间；游客 mine=false
 *   - 删自己的评论：非本人被拒、不存在幂等、本人硬删除
 *   - 举报：理由必填、评论必须存在、不能重复举报
 *   - 管理端：待处理列表、单条举报、下架评论、标记已处理
 */
class CommentServiceTest {

    private CommentMapper commentMapper;
    private CommentReportMapper reportMapper;
    private UserMapper userMapper;
    private StoryFragmentMapper fragmentMapper;
    private CommentRateLimiter rateLimiter;
    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        reportMapper = mock(CommentReportMapper.class);
        userMapper = mock(UserMapper.class);
        fragmentMapper = mock(StoryFragmentMapper.class);
        rateLimiter = mock(CommentRateLimiter.class);
        service = new CommentServiceImpl();
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "reportMapper", reportMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "fragmentMapper", fragmentMapper);
        ReflectionTestUtils.setField(service, "commentRateLimiter", rateLimiter);
    }

    @AfterEach
    void tearDown() {
        // 测试里没有真正走 MyBatis 拦截器，手动清掉 PageHelper 的 ThreadLocal，避免串到其他用例
        PageHelper.clearPage();
    }

    private StoryFragment publishedFragment() {
        StoryFragment f = new StoryFragment();
        f.setId(1L);
        f.setStatus(StoryFragment.STATUS_PUBLISHED);
        return f;
    }

    /** 空内容（全空格也算空）直接拒绝，不落库 */
    @Test
    void addRejectsEmptyContent() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.add(1L, 1L, "   "));
        assertEquals(ResultCode.COMMENT_EMPTY, e.getResultCode());
        verify(commentMapper, never()).insert(any());
    }

    /** 超过 100 字拒绝（Q5） */
    @Test
    void addRejectsOver100Chars() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.add(1L, 1L, "a".repeat(101)));
        assertEquals(ResultCode.COMMENT_TOO_LONG, e.getResultCode());
    }

    /** 碎片不存在拒绝，防止挂空评论 */
    @Test
    void addRejectsMissingFragment() {
        when(fragmentMapper.selectById(1L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class, () -> service.add(1L, 1L, "你好"));
        assertEquals(ResultCode.FRAGMENT_NOT_PUBLISHED, e.getResultCode());
    }

    /** 碎片存在但没上架（待审核/隐藏）也拒绝 */
    @Test
    void addRejectsNotPublishedFragment() {
        StoryFragment f = new StoryFragment();
        f.setStatus(StoryFragment.STATUS_PENDING);
        when(fragmentMapper.selectById(1L)).thenReturn(f);
        BusinessException e = assertThrows(BusinessException.class, () -> service.add(1L, 1L, "你好"));
        assertEquals(ResultCode.FRAGMENT_NOT_PUBLISHED, e.getResultCode());
    }

    /** Q8 限频：1 分钟内第 11 条被拒 */
    @Test
    void addBlockedByRateLimit() {
        when(fragmentMapper.selectById(1L)).thenReturn(publishedFragment());
        when(rateLimiter.tryAcquire(1L)).thenReturn(false);
        BusinessException e = assertThrows(BusinessException.class, () -> service.add(1L, 1L, "你好"));
        assertEquals(ResultCode.COMMENT_TOO_FREQUENT, e.getResultCode());
        verify(commentMapper, never()).insert(any());
    }

    /** 合法评论：内容去空格、免审核直接落库 */
    @Test
    void addSuccessTrimsAndInserts() {
        when(fragmentMapper.selectById(1L)).thenReturn(publishedFragment());
        when(rateLimiter.tryAcquire(1L)).thenReturn(true);

        service.add(1L, 1L, "  你好世界  ");

        org.mockito.ArgumentCaptor<Comment> captor = org.mockito.ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals("你好世界", captor.getValue().getContent());
        assertEquals(1L, captor.getValue().getFragmentId());
        assertEquals(1L, captor.getValue().getUserId());
    }

    /** 列表：VO 带昵称/角色/格式化时间，登录用户自己的评论 mine=true */
    @Test
    void listMarksMineAndFillsRoleAndTime() {
        Comment c = new Comment();
        c.setId(5L);
        c.setFragmentId(1L);
        c.setUserId(2L);
        c.setContent("旧书店的纸香很治愈");
        c.setCreatedAt(LocalDateTime.of(2026, 8, 14, 21, 0, 0));
        Page<Comment> page = new Page<>();
        page.add(c);
        page.setTotal(1);
        when(commentMapper.selectByFragment(1L)).thenReturn(page);

        User author = new User();
        author.setNickname("阿澈");
        author.setRole(User.ROLE_ADMIN);
        author.setAvatar("a.png");
        when(userMapper.selectById(2L)).thenReturn(author);

        PageBean result = service.listByFragment(2L, 1L, 1, 5);
        List<CommentVO> vos = result.getList();

        assertEquals(1L, result.getTotal());
        assertEquals(5L, vos.get(0).getId());
        assertEquals("阿澈", vos.get(0).getAuthorName());
        assertEquals("a.png", vos.get(0).getAuthorAvatar());
        assertEquals(User.ROLE_ADMIN, vos.get(0).getAuthorRole());
        assertTrue(vos.get(0).getMine());
        assertEquals("2026-08-14 21:00:00", vos.get(0).getCreatedAt());
    }

    /** 游客（userId 为 null）看列表：不标记「自己的评论」 */
    @Test
    void guestListHasMineFalse() {
        Comment c = new Comment();
        c.setId(5L);
        c.setUserId(2L);
        Page<Comment> page = new Page<>();
        page.add(c);
        page.setTotal(1);
        when(commentMapper.selectByFragment(1L)).thenReturn(page);
        when(userMapper.selectById(2L)).thenReturn(new User());

        PageBean result = service.listByFragment(null, 1L, 1, 5);
        assertFalse(((CommentVO) result.getList().get(0)).getMine());
    }

    /** 删别人的评论被拒（403） */
    @Test
    void deleteOwnRejectsNonAuthor() {
        Comment c = new Comment();
        c.setId(9L);
        c.setUserId(2L);
        when(commentMapper.selectById(9L)).thenReturn(c);

        BusinessException e = assertThrows(BusinessException.class, () -> service.deleteOwn(1L, 9L));
        assertEquals(ResultCode.NOT_AUTHOR, e.getResultCode());
        verify(commentMapper, never()).deleteById(9L);
    }

    /** 评论已被删（不存在）：幂等，不报错 */
    @Test
    void deleteOwnMissingCommentIsNoop() {
        when(commentMapper.selectById(9L)).thenReturn(null);
        service.deleteOwn(1L, 9L);
        verify(commentMapper, never()).deleteById(any());
    }

    /** 本人删自己的评论：硬删除（Q7） */
    @Test
    void deleteOwnSuccess() {
        Comment c = new Comment();
        c.setId(9L);
        c.setUserId(2L);
        when(commentMapper.selectById(9L)).thenReturn(c);

        service.deleteOwn(2L, 9L);
        verify(commentMapper).deleteById(9L);
    }

    /** 举报理由必填（Q6） */
    @Test
    void reportRequiresReason() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.report(1L, 3L, "  "));
        assertEquals(ResultCode.REPORT_REASON_REQUIRED, e.getResultCode());
    }

    /** 举报不存在的评论被拒 */
    @Test
    void reportRejectsMissingComment() {
        when(commentMapper.selectById(3L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class, () -> service.report(1L, 3L, "广告"));
        assertEquals(ResultCode.COMMENT_NOT_FOUND, e.getResultCode());
    }

    /** 同一人不能重复举报同一评论 */
    @Test
    void reportRejectsDuplicate() {
        Comment c = new Comment();
        c.setId(3L);
        when(commentMapper.selectById(3L)).thenReturn(c);
        when(reportMapper.selectByCommentAndReporter(3L, 1L)).thenReturn(new CommentReport());

        BusinessException e = assertThrows(BusinessException.class, () -> service.report(1L, 3L, "广告"));
        assertEquals(ResultCode.REPORT_DUPLICATE, e.getResultCode());
    }

    /** 合法举报：理由去空格后入库 */
    @Test
    void reportSuccessTrimsReason() {
        Comment c = new Comment();
        c.setId(3L);
        when(commentMapper.selectById(3L)).thenReturn(c);
        when(reportMapper.selectByCommentAndReporter(3L, 1L)).thenReturn(null);

        service.report(1L, 3L, "  广告刷屏  ");

        org.mockito.ArgumentCaptor<CommentReport> captor = org.mockito.ArgumentCaptor.forClass(CommentReport.class);
        verify(reportMapper).insert(captor.capture());
        assertEquals("广告刷屏", captor.getValue().getReason());
        assertEquals(3L, captor.getValue().getCommentId());
        assertEquals(1L, captor.getValue().getReporterId());
    }

    /** 管理端：待处理举报列表直接返回 */
    @Test
    void listPendingReportsDelegates() {
        when(reportMapper.selectPending()).thenReturn(List.of(new CommentReport()));
        assertEquals(1, service.listPendingReports().size());
    }

    /** 管理端：取单条举报，不存在抛 404 */
    @Test
    void getReportMissingThrows() {
        when(reportMapper.selectById(7L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class, () -> service.getReport(7L));
        assertEquals(ResultCode.COMMENT_NOT_FOUND, e.getResultCode());
    }

    /** 管理端：下架评论=硬删除 */
    @Test
    void deleteCommentByAdminDeletes() {
        service.deleteCommentByAdmin(3L);
        verify(commentMapper).deleteById(3L);
    }

    /** 管理端：标记举报已处理 */
    @Test
    void markReportHandledMarks() {
        service.markReportHandled(7L);
        verify(reportMapper).markHandled(7L);
    }
}
