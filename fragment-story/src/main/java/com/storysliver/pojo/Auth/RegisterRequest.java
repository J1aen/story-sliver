package com.storysliver.pojo.Auth;

/**
 * 注册请求体（Task 5，待实现）。
 * 干什么用：承载前端注册表单的完整字段。
 * 待实现的字段：
 *   - username 用户名（@NotBlank @Size(max=32)）
 *   - nickname 昵称（@NotBlank @Size(max=32)）
 *   - password 明文密码（@NotBlank，Service 里会 BCrypt 加密）
 *   - captchaKey 验证码 key（@NotBlank，来自 GET /api/auth/captcha）
 *   - captchaAnswer 用户输入的验证码答案（@NotBlank）
 *   - isAdmin 是否勾选「注册为管理员」（Boolean，默认 false）
 *   - adminCode 管理员注册特殊密码（勾选管理员时必填）
 */
public class RegisterRequest {
}
