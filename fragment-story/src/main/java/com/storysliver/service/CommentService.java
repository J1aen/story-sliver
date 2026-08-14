package com.storysliver.service;

import com.storysliver.pojo.CommentReport;
import com.storysliver.pojo.CommentVO;
import com.storysliver.pojo.PageBean;

import java.util.List;

/**
 * 评论服务接口（v2.0 Task 21）。
 * 干什么用：定义评论的增删查、举报与管理端处理能力，实现类集中放业务规则。
 */
public interface CommentService {

    /** 发评论（免审核，发布即显示）：校验内容/碎片/限频后落库 */
    void add(Long userId, Long fragmentId, String content);

    /** 按碎片分页查评论（时间正序）：登录用户标记「是否自己的评论」，游客为 false */
    PageBean listByFragment(Long userId, Long fragmentId, int pageNum, int pageSize);

    /** 删自己的评论（Q7：硬删除，弹窗确认不可恢复） */
    void deleteOwn(Long userId, Long commentId);

    /** 举报评论（理由必填、同一人不能重复举报同一条） */
    void report(Long userId, Long commentId, String reason);

    /** 管理端：待处理举报列表 */
    List<CommentReport> listPendingReports();

    /** 管理端：取单条举报详情（不存在抛 404） */
    CommentReport getReport(Long reportId);

    /** 管理端：下架评论（硬删除） */
    void deleteCommentByAdmin(Long commentId);

    /** 管理端：标记举报已处理 */
    void markReportHandled(Long reportId);
}
