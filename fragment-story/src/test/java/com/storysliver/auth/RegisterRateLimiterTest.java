package com.storysliver.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注册 IP 限流单元测试。
 * 验证：同一 IP 第 6 次注册被拦（1 小时 5 个上限）；不同 IP 互不影响。
 */
class RegisterRateLimiterTest {

    /** 同一 IP 连注册 5 次成功，第 6 次被拦；换个 IP 又能注册 */
    @Test
    void sixthRegisterFromSameIpIsBlocked() {
        RegisterRateLimiter limiter = new RegisterRateLimiter();

        // 前 5 次都放行
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed("1.1.1.1"), "第 " + (i + 1) + " 次应该放行");
        }

        // 第 6 次被拦
        assertFalse(limiter.isAllowed("1.1.1.1"), "同一 IP 第 6 次应该被拦");

        // 不同 IP 不受影响
        assertTrue(limiter.isAllowed("2.2.2.2"), "不同 IP 应该放行");
    }
}
