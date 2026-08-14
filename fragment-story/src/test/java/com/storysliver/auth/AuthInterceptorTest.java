package com.storysliver.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import com.storysliver.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 认证拦截器单元测试。
 * 干什么用：验证有效 token 放行并写入 UserContext；缺失 token 返回 401。
 */
class AuthInterceptorTest {

    private AuthInterceptor interceptor;
    private JwtUtil jwtUtil;
    private UserMapper userMapper;
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
        // 封禁校验会查 userMapper，单测里 mock 掉（返回 null 表示用户不存在/未封禁）
        userMapper = mock(UserMapper.class);
        ReflectionTestUtils.setField(interceptor, "userMapper", userMapper);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    /** 带有效 token：放行，且 UserContext 能取到 userId 和 role；请求结束被清理 */
    @Test
    void validTokenPassesAndSetsContext() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken(7L, 2));
        when(userMapper.selectById(7L)).thenReturn(null);

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

    /** 公开接口：GET /api/fragments 不带 token 也放行（游客可看墙） */
    @Test
    void publicListGetPassesWithoutToken() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/api/fragments");
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    /** 发布接口：POST /api/fragments 不带 token 必须被拦（防止发布拿不到登录用户） */
    @Test
    void submitPostRequiresToken() throws Exception {
        request.setMethod("POST");
        request.setRequestURI("/api/fragments");
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }
}
