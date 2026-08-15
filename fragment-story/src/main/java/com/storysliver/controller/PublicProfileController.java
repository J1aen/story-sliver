package com.storysliver.controller;

import com.storysliver.auth.JwtUtil;
import com.storysliver.pojo.Result;
import com.storysliver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 他人主页公开接口（v2.0 Task 20）。
 * 干什么用：游客/登录用户都能看任意用户的公开主页（昵称/头像/铭牌 + 非匿名已发布碎片）。
 * 为什么单独一个 Controller：和 /api/users 的「我」接口分开，路径清晰且不掺登录态逻辑。
 */
@RestController
@RequestMapping("/api/users/{userId}/profile")
public class PublicProfileController {

    @Autowired
    private UserService userService;//用户业务（getPublicProfile）

    @Autowired
    private JwtUtil jwtUtil;//可选登录解析：标记「我是否赞过」

    /**
     * 他人主页：分页返回公开碎片（默认每页 9 条，配合前端 3 列瀑布流）。
     * 为什么手动解析 token：接口公开（游客可看），但登录用户要看到「已赞」状态（同碎片列表的做法）。
     */
    @GetMapping
    public Result profile(@PathVariable Long userId,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "9") int pageSize,
                          HttpServletRequest http) {
        return Result.success(userService.getPublicProfile(userId, optionalUserId(http), pageNum, pageSize));
    }

    /** 可选登录解析：token 合法返回 userId，否则返回 null（游客） */
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
