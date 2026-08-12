package com.storysliver.common;

import com.storysliver.pojo.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 全局异常处理单元测试。
 * 验证：业务错误码能映射成正确的 HTTP 状态码（400/401/403/404/429）。
 * 为什么要测：前端靠 HTTP 状态码区分「未登录跳登录页」「限流提示等待」，映射错了体验就坏了。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 验证码错误（400）→ HTTP 400 */
    @Test
    void captchaErrorMapsTo400() {
        ResponseEntity<Result> resp = handler.handleBusinessException(new BusinessException(ResultCode.CAPTCHA_ERROR));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(400, resp.getBody().getCode());
    }

    /** 注册限流（429）→ HTTP 429 */
    @Test
    void rateLimitMapsTo429() {
        ResponseEntity<Result> resp = handler.handleBusinessException(new BusinessException(ResultCode.REGISTER_TOO_FREQUENT));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.getStatusCode());
    }

    /** 登录失败（401）→ HTTP 401 */
    @Test
    void loginFailedMapsTo401() {
        ResponseEntity<Result> resp = handler.handleBusinessException(new BusinessException(ResultCode.LOGIN_FAILED));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    /** 权限不足（403）→ HTTP 403 */
    @Test
    void forbiddenMapsTo403() {
        ResponseEntity<Result> resp = handler.handleBusinessException(new BusinessException(ResultCode.FORBIDDEN));
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
