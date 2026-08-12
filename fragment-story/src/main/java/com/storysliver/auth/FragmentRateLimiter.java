package com.storysliver.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

@Service
public class FragmentRateLimiter {
    // userId -> 上次发布时间（毫秒时间戳）
    // 为什么用 ConcurrentHashMap：发布接口可能并发，ConcurrentHashMap 线程安全
    private final Map<Long, Long> lastSubmit = new ConcurrentHashMap<>();

    // 5 分钟的毫秒数：5 * 60 * 1000
    private static final long INTERVAL_MS = 5 * 60_000L;

    public boolean tryAcquire(Long userId) {
//       获取当前时间
        long now = System.currentTimeMillis();
//       获取用户上次发布时间
        Long prev = lastSubmit.putIfAbsent(userId,now);
//      如果是空的，说明是第一次发布 直接放行
        if (prev == null){
            return true;
        }
//        现在 - 上次发布时间 < 5 分钟：返回 false
        if (now - prev < INTERVAL_MS){
            return false;
        }
//        更新用户上次发布时间
        lastSubmit.put(userId, now);
        return true;
    }

}
