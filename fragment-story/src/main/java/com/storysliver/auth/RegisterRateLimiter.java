package com.storysliver.auth;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册 IP 限流器：防止批量注册脚本。
 * 干什么用：同一 IP 1 小时最多注册 5 个、24 小时最多 10 个，防止批量注册脚本。
 * 为什么放宽到 5/10：防止同一 WiFi 下的真实小团体被误伤（验证码是主防线，这里只是兜底）。
 * 为什么用内存实现：单实例部署够用、零依赖；以后多实例再换 Redis。
 */
@Service
public class RegisterRateLimiter {

    // ip -> 该 IP 的注册时间戳队列
    // 为什么用 ConcurrentHashMap：限流接口可能被并发请求，ConcurrentHashMap 线程安全
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    private static final long HOUR_MS = 3_600_000L;//1 小时的毫秒数
    private static final long DAY_MS = 86_400_000L;//24 小时的毫秒数

    /**
     * 尝试放行一次注册。
     * @param ip 客户端 IP（AuthController 里取）
     * @return true=允许注册；false=太频繁，拒绝
     * 为什么用 Deque（双端队列）：队头是最早的记录，方便把过期的从队头清掉；队尾追加新记录
     */
    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();//当前时间（毫秒）

        // 拿到该 IP 的队列；第一次注册时 computeIfAbsent 会自动创建空队列
        Deque<Long> queue = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());

        // 对队列加锁：同一 IP 的并发注册串行处理，保证统计准确
        synchronized (queue) {
            // 先把 24 小时前的旧记录从队头清掉，防止内存无限增长
            while (!queue.isEmpty() && now - queue.peekFirst() > DAY_MS) {
                queue.pollFirst();
            }

            // 统计 1 小时内的注册次数：遍历队列，数出没超过 1 小时的记录
            long hourCount = 0;
            for (Long t : queue) {
                if (now - t <= HOUR_MS) {
                    hourCount++;
                }
            }

            // 1 小时超过 5 次，或 24 小时超过 10 次，都拒绝
            if (hourCount >= 5 || queue.size() >= 10) {
                return false;
            }

            // 放行并记录本次注册时间
            queue.addLast(now);
            return true;
        }
    }
}
