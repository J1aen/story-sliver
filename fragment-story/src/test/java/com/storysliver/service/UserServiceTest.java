package com.storysliver.service;

/**
 * 用户服务单元测试（Task 5，待实现）。
 * 干什么用：用 Mockito 模拟 Mapper，验证注册/登录的关键行为：
 *   - 注册普通用户 → 角色 0、密码是 BCrypt 哈希、返回 token
 *   - 验证码错误 / 限流 / 用户名重复 / 密码太短 → 抛 BusinessException
 *   - 第一个用特殊密码注册的管理员 → 角色 2（站长）
 *   - 登录成功返回 token；密码错误抛异常
 */
public class UserServiceTest {
}
