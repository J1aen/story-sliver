package com.storysliver.controller;

import com.storysliver.auth.UserContext;
import com.storysliver.pojo.Result;
import com.storysliver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import com.storysliver.pojo.Auth.UpdateNicknameRequest;
import com.storysliver.pojo.Auth.UpdatePasswordRequest;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 用户接口：当前登录用户信息。
 * 这个接口需要登录——JWT 拦截器会自动校验（/api/** 默认都要 token，公开接口在 WebConfig 排除），
 * 所以这里不用写任何登录判断代码。
 */
@RestController
@Slf4j
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;//用户业务

    /**
     * 返回当前用户信息。
     * @return User（id/昵称/角色等），前端登录后立即调用一次填充界面
     */
    @GetMapping("/me")
    public Result me() {
        // UserContext 由 AuthInterceptor 在请求开始时写入，这里直接取，不用从参数传
        return Result.success(userService.me(UserContext.getUserId()));
    }

    /**
     * 上传头像（进入待审核）。
     * @param file 图片文件（multipart 表单字段名 file）
     * @return { avatarPending, text }，前端提示「等待管理员审核」
     */
    @PostMapping("/avatar")
    public Result avatar(@RequestParam("file") MultipartFile file) {
        String pending = userService.uploadAvatar(UserContext.getUserId(), file);
        return Result.success(Map.of("avatarPending", pending, "text", "头像已提交，等待管理员审核"));
    }

    /**
     * 修改昵称（v1.2）：设置下拉里「修改昵称」调用。
     * @param request 新昵称（唯一、不含敏感词、≤32 字）
     */
    @PutMapping("/nickname")
    public Result updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        userService.updateNickname(UserContext.getUserId(), request.getNickname());
        return Result.success(Map.of("text", "昵称已更新"));
    }

    /**
     * 修改密码（v1.2）：设置下拉里「修改密码」调用；改完前端强制重新登录。
     * @param request 旧密码 + 新密码
     */
    @PutMapping("/password")
    public Result updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(UserContext.getUserId(), request.getOldPassword(), request.getNewPassword());
        return Result.success(Map.of("text", "密码已修改，请重新登录"));
    }
}
