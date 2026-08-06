package com.storysliver.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 工具单元测试。
 * 干什么用：验证「签发 → 解析」能取回 userId 和 role；篡改过的 token 必须被拒绝。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // 用测试专用密钥构造 JwtUtil（不依赖 Spring 容器，直接 new）
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-test-secret-test-secret-123456");
        properties.setExpireDays(30);
        jwtUtil = new JwtUtil();
        // JwtUtil 里是 @Autowired 字段注入，单测里用反射注入
        org.springframework.test.util.ReflectionTestUtils.setField(jwtUtil, "properties", properties);
    }

    /** 正常签发后能解析出 userId 和 role */
    @Test
    void tokenRoundTripKeepsSubjectAndRole() {
        String token = jwtUtil.generateToken(42L, 1);
        Claims claims = jwtUtil.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals(1, claims.get("role", Integer.class));
    }

    /** 篡改 token（末尾加字符）必须解析失败 */
    @Test
    void tamperedTokenThrows() {
        String token = jwtUtil.generateToken(1L, 0) + "x";
        assertThrows(JwtException.class, () -> jwtUtil.parse(token));
    }
}
