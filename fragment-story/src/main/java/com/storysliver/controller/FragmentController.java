package com.storysliver.controller;

import com.storysliver.auth.JwtUtil;
import com.storysliver.auth.UserContext;
import com.storysliver.pojo.Fragment.FragmentSubmitRequest;
import com.storysliver.pojo.Result;
import com.storysliver.pojo.StoryFragment;
import com.storysliver.service.FragmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 碎片接口：发布、首页列表、我的碎片、点赞/取消、隐藏/取消隐藏、作者删除。
 * 除 GET /api/fragments（游客可看）外，其余接口都需登录——
 * JWT 拦截器自动校验（WebConfig 里只排除了 /api/fragments 这一个路径）。
 */
@RestController
@Slf4j
@RequestMapping("/api/fragments")
public class FragmentController {

    @Autowired
    private FragmentService fragmentService;//碎片业务

    @Autowired
    private JwtUtil jwtUtil;//首页列表做「可选登录解析」用

    /**
     * 发布碎片。
     * @param request 内容 + 是否匿名
     * @return { id, status }，status 固定为 0（待审核，管理员通过后才上墙）
     */
    @PostMapping
    public Result submit(@Valid @RequestBody FragmentSubmitRequest request) {
        // 当前用户 id 由拦截器写进 UserContext，直接取
        StoryFragment fragment = fragmentService.submit(
                UserContext.getUserId(),
                request.getContent(),
                Boolean.TRUE.equals(request.getIsAnonymous()));
        return Result.success(Map.of("id", fragment.getId(), "status", fragment.getStatus()));
    }

    /**
     * 首页列表：分页，只显示已发布。
     * 为什么这个方法要手动解析 token：该路径被拦截器排除（游客可看），
     * 但登录用户要看到「已赞」状态，所以做成「可选登录」——有 token 就解析，没有就当游客。
     */
    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       HttpServletRequest http) {
        Long userId = optionalUserId(http);
        return Result.success(fragmentService.list(pageNum, pageSize, userId));
    }

    /** 我的碎片：返回自己全部状态（待审核/已发布/已隐藏，含匿名的） */
    @GetMapping("/my")
    public Result my() {
        return Result.success(fragmentService.my(UserContext.getUserId()));
    }

    /** 点赞：登录后每人每条一次 */
    @PostMapping("/{id}/like")
    public Result like(@PathVariable Long id) {
        fragmentService.like(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 取消点赞 */
    @DeleteMapping("/{id}/like")
    public Result unlike(@PathVariable Long id) {
        fragmentService.unlike(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 隐藏：仅作者、仅已发布（1→2），他人不可见 */
    @PutMapping("/{id}/hide")
    public Result hide(@PathVariable Long id) {
        fragmentService.hide(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 取消隐藏：仅作者、仅已隐藏（2→1），恢复可见 */
    @PutMapping("/{id}/unhide")
    public Result unhide(@PathVariable Long id) {
        fragmentService.unhide(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 作者硬删除：删行并清理点赞，前端弹窗确认「删除后无法撤回」 */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        fragmentService.deleteByAuthor(UserContext.getUserId(), id);
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
