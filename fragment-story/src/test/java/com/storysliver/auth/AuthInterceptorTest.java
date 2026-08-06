package com.storysliver.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 认证拦截器单元测试。
 * 干什么用：验证有效 token 放行并写入 UserContext；缺失 token 返回 401。
 */
class AuthInterceptorTest {

    private AuthInterceptor interceptor;
    private JwtUtil jwtUtil;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-test-secret-test-secret-123456");
        properties.setExpireDays(30);
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "properties", properties);

        interceptor = new AuthInterceptor();
        // @Autowired 字段注入，单测里用反射注入 mock 好的 JwtUtil
        ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    /** 带有效 token：放行，且 UserContext 能取到 userId 和 role；请求结束被清理 */
    @Test
    void validTokenPassesAndSetsContext() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken(7L, 2));

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(7L, UserContext.getUserId());
        assertEquals(2, UserContext.getRole());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(UserContext.getUserId());
    }

    /** 不带 token：拦截并返回 401 */
    @Test
    void missingTokenReturns401() throws Exception {
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }
}
