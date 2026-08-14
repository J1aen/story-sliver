package com.storysliver.auth;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 评论频率限流器（Task 21，Q8：1 分钟 10 条）。
 * 干什么用：防止同一用户短时间刷评论刷屏。
 * 为什么用内存队列实现：单实例部署够用、零依赖；以后多实例再换 Redis。
 * 为什么队列而不是「上次时间」：要支持 1 分钟内最多 10 条，需要记录最近 10 条的时间点来判断。
 */
@Service
public class CommentRateLimiter {

    // userId -> 该用户的评论时间戳队列
    // 为什么用 ConcurrentHashMap：限频接口可能并发，ConcurrentHashMap 线程安全
    private final Map<Long, Deque<Long>> hits = new ConcurrentHashMap<>();

    private static final long WINDOW_MS = 60_000L;// 1 分钟的毫秒数（Q8）
    private static final int MAX_COMMENTS = 10;// 1 分钟内最多 10 条（Q8）

    /**
     * 尝试放行一次评论。
     * @param userId 评论者 id
     * @return true=允许；false=1 分钟内已满 10 条，拒绝
     * 为什么用 Deque（双端队列）：队头是最早的记录，方便从队头淘汰过期时间；队尾追加新记录
     */
    public boolean tryAcquire(Long userId) {
        long now = System.currentTimeMillis();// 当前时间（毫秒）

        // 取该用户的评论时间队列；第一次评论时 computeIfAbsent 自动创建空队列
        Deque<Long> queue = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());

        // 对队列加锁：同一用户的并发评论串行统计，保证不超过 10 条
        synchronized (queue) {
            // 先把 1 分钟前的旧记录从队头清掉，防止队列无限增长，也保证统计的是「最近 1 分钟」
            while (!queue.isEmpty() && now - queue.peekFirst() > WINDOW_MS) {
                queue.pollFirst();
            }

            // 最近 1 分钟内已满 10 条：拒绝本次评论
            if (queue.size() >= MAX_COMMENTS) {
                return false;
            }

            // 放行并记录本次评论时间
            queue.addLast(now);
            return true;
        }
    }
}
