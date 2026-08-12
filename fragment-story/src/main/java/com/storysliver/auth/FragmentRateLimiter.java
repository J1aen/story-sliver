package com.storysliver.auth;

/**
 * 发布频率限流器（Task 7，待实现——你自己写）。
 * 干什么用：每个用户 5 分钟只能发 1 条碎片，防止刷墙。
 * 待实现：
 *   - 类上标 @Service
 *   - 字段 Map&lt;Long, Long&gt; lastSubmit（userId -> 上次发布时间，用 ConcurrentHashMap 保证线程安全）
 *   - 常量 INTERVAL_MS = 5 * 60_000L
 *   - 方法 public boolean tryAcquire(Long userId)：第一次放行；5 分钟内第二次返回 false；过了 5 分钟更新时间放行
 * 为什么独立成类：限频逻辑只存在于这一个文件，想改间隔、以后换 Redis 只动它。
 */
public class FragmentRateLimiter {
}
