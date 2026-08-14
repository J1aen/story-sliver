package com.storysliver.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 评论限频单元测试（Q8：1 分钟 10 条）。
 * 验证：同一用户 1 分钟内第 11 次被拦；不同用户互不影响。
 */
class CommentRateLimiterTest {

    /** 同一用户连发 10 次放行，第 11 次被拦；换用户又能发 */
    @Test
    void eleventhCommentWithinOneMinuteIsBlocked() {
        CommentRateLimiter limiter = new CommentRateLimiter();

        // 前 10 次都放行
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire(1L), "第 " + (i + 1) + " 次评论应该放行");
        }

        // 第 11 次被拦
        assertFalse(limiter.tryAcquire(1L), "1 分钟内第 11 次评论应该被拦");

        // 不同用户不受影响
        assertTrue(limiter.tryAcquire(2L), "不同用户应该放行");
    }
}
