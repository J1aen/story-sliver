package com.storysliver.auth;

/**
 * 注册 IP 限流器（Task 5，待实现）。
 * 干什么用：同一 IP 1 小时最多注册 5 个、24 小时最多 10 个，防止批量注册脚本。
 * 待实现：
 *   public boolean isAllowed(String ip) —— 用 Map&lt;String, Deque&lt;Long&gt;&gt; 记录每个 IP 的注册时间戳
 * 为什么放宽到 5/10：防止同一 WiFi 下的真实小团体被误伤（验证码是主防线，这里只是兜底）。
 */
public class RegisterRateLimiter {
}
