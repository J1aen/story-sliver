package com.storysliver.controller;

import com.storysliver.auth.RequireRole;
import com.storysliver.auth.UserContext;
import com.storysliver.pojo.Admin.HandleReportRequest;
import com.storysliver.pojo.CommentReport;
import com.storysliver.pojo.Result;
import com.storysliver.pojo.User;
import com.storysliver.service.AdminService;
import com.storysliver.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论管理接口（v2.0 Task 21）：待处理举报列表 + 处理举报。
 * 权限：管理员和站长可用（@RequireRole），普通用户访问返回 403。
 */
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;//评论业务（举报查询与下架）

    @Autowired
    private AdminService adminService;//用户封禁复用现有逻辑

    /** 待处理举报列表（管理员/站长） */
    @GetMapping("/reports")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result reports() {
        return Result.success(commentService.listPendingReports());
    }

    /**
     * 处理举报（管理员/站长）。
     * @param id 举报 id
     * @param req { action, banDays, banReason }：dismiss=不下架 / delete=下架评论 / ban=下架+封禁用户
     */
    @PostMapping("/reports/{id}/handle")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result handle(@PathVariable Long id, @RequestBody HandleReportRequest req) {
        CommentReport r = commentService.getReport(id);// 先取举报详情（含评论者 id）
        String action = req.getAction();// 处理动作
        if ("delete".equals(action) || "ban".equals(action)) {// 下架评论（硬删除）
            commentService.deleteCommentByAdmin(r.getCommentId());
        }
        if ("ban".equals(action)) {// 封禁该评论用户（操作者角色从 UserContext 取，与现有封禁逻辑一致）
            adminService.banUser(UserContext.getRole(), r.getCommenterId(), req.getBanDays(), req.getBanReason());
        }
        commentService.markReportHandled(id);// 最后标记举报已处理（无论哪种动作）
        return Result.success();
    }
}
