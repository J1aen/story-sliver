package com.storysliver.controller;

/**
 * 认证接口（Task 5，待实现）。
 * 干什么用：提供验证码 / 注册 / 登录三个公开接口。
 * 待实现：
 *   @RestController @RequestMapping("/api/auth")
 *   GET  /api/auth/captcha   获取验证码（调 CaptchaService.generate()）
 *   POST /api/auth/register  注册（调 UserService.register()，返回 token）
 *   POST /api/auth/login     登录（调 UserService.login()，返回 token）
 */
public class AuthController {
}
