package com.storysliver.service;

/**
 * 用户服务接口（Task 5，待实现）。
 * 干什么用：定义注册 / 登录 / 当前用户信息三个业务方法。
 * 待实现的方法：
 *   String register(RegisterRequest request, String ip) —— 注册（成功自动登录，返回 JWT）
 *   String login(String username, String password) —— 登录，返回 JWT
 *   User me(Long userId) —— 查询当前用户信息
 */
public interface UserService {
}
