package com.storysliver.service.impl;

import com.storysliver.service.UserService;

/**
 * 用户服务实现（Task 5，待实现）。
 * 干什么用：注册 / 登录的核心业务逻辑，实现 UserService 接口。
 * 待实现（对照「后端套路速查」第 3 节的 8 个动作）：
 *   验证码 → 限流 → 密码长度 → 用户名查重 → 管理员密码 → 加密 → 存库 → 返回 token
 * 需要的依赖（@Autowired 字段注入）：
 *   UserMapper、SystemConfigMapper、CaptchaService、RegisterRateLimiter、PasswordEncoder、JwtUtil
 */
public class UserServiceImpl implements UserService {
}
