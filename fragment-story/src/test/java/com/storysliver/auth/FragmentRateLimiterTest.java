package com.storysliver.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发布限频单元测试（为「你自己写的」FragmentRateLimiter 写的测试）。
 * 验证：同用户第一次放行、5 分钟内第二次被拦；不同用户互不影响。
 */
class FragmentRateLimiterTest {

    /** 同用户连发两次：第一次放行，第二次（5 分钟内）被拦；换用户又能发 */
    @Test
    void blocksSecondSubmitWithin5Minutes() {
        FragmentRateLimiter limiter = new FragmentRateLimiter();

        assertTrue(limiter.tryAcquire(1L), "第一次发布应该放行");
        assertFalse(limiter.tryAcquire(1L), "5 分钟内第二次应该被拦");
        assertTrue(limiter.tryAcquire(2L), "不同用户不受影响");
    }
}
