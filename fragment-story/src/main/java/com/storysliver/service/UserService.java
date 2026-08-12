package com.storysliver.service;

import com.storysliver.pojo.Auth.RegisterRequest;
import com.storysliver.pojo.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口：注册 / 登录 / 当前用户信息。
 * 为什么定义接口而不是直接写实现类：
 * Controller 只依赖接口，换实现或做单元测试（Mockito 打桩）都方便。
 */
public interface UserService {

    /**
     * 注册用户。
     * @param request 注册表单（用户名/昵称/密码/验证码/是否管理员）
     * @param ip 客户端 IP，用于注册限流
     * @return JWT token（注册成功即自动登录）
     */
    String register(RegisterRequest request, String ip);

    /**
     * 登录。
     * @param username 用户名
     * @param password 明文密码
     * @return JWT token
     */
    String login(String username, String password);

    /**
     * 查询当前登录用户信息。
     * @param userId 用户 id（来自 UserContext）
     * @return 用户信息
     */
    User me(Long userId);

    /**
     * 上传头像（进入待审核状态，管理员通过后才生效）。
     * @param userId 当前用户 id
     * @param file 上传的图片（jpg/png，≤2MB）
     * @return 待审核头像 URL
     */
    String uploadAvatar(Long userId, MultipartFile file);
}
