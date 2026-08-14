package com.storysliver.controller;

import com.storysliver.auth.JwtUtil;
import com.storysliver.auth.UserContext;
import com.storysliver.pojo.CommentRequest;
import com.storysliver.pojo.Result;
import com.storysliver.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 评论接口（v2.0 Task 21）：发评论、列表、删自己的、举报。
 * 除 GET /api/comments（游客可看列表）外，其余接口都需登录——JWT 拦截器自动校验。
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;//评论业务

    @Autowired
    private JwtUtil jwtUtil;//列表做「可选登录解析」用（标记是否自己的评论）

    /** 发评论：需登录（拦截器保证），免审核直接显示 */
    @PostMapping
    public Result add(@RequestBody CommentRequest req) {
        commentService.add(UserContext.getUserId(), req.getFragmentId(), req.getContent());
        return Result.success();
    }

    /**
     * 评论列表：游客可看（拦截器按 GET 放行）。
     * 为什么手动解析 token：和碎片列表一样做「可选登录」——有 token 标记「是否自己的评论」，没有就当游客。
     */
    @GetMapping
    public Result list(@RequestParam Long fragmentId,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "5") int pageSize,
                       HttpServletRequest http) {
        return Result.success(commentService.listByFragment(optionalUserId(http), fragmentId, pageNum, pageSize));
    }

    /** 删自己的评论（Q7 硬删除）：需登录 */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        commentService.deleteOwn(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 举报评论（理由必填）：需登录 */
    @PostMapping("/{id}/report")
    public Result report(@PathVariable Long id, @RequestBody Map<String, String> body) {
        commentService.report(UserContext.getUserId(), id, body.get("reason"));
        return Result.success();
    }

    /**
     * 可选登录解析：token 合法就返回 userId，否则返回 null（游客）。
     * 为什么 try-catch 静默忽略：token 过期/损坏时按游客展示，不打断浏览。
     */
    private Long optionalUserId(HttpServletRequest http) {
        String auth = http.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            return Long.valueOf(jwtUtil.parse(auth.substring(7)).getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
