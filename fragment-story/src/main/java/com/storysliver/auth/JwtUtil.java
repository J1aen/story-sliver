package com.storysliver.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 工具类：负责签发和解析 token。
 * 干什么用：登录/注册成功后签发 token；每次请求由 AuthInterceptor 调用 parse 校验身份。
 * 为什么用 JWT：无状态，服务重启不失效，也不用 Redis 存会话；token 里带 userId 和 role。
 */
@Component
public class JwtUtil {
    @Autowired
    private JwtProperties properties;

    /**
     * 由配置里的密钥构造 HS256 签名密钥。
     * 为什么单独抽这个方法：签发和解析都要用同一个密钥，避免两处各写一遍。
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 token。
     * @param userId 用户主键，放进 subject，拦截器解析后直接拿
     * @param role 角色（0普通 1管理员 2站长），放进自定义 claim，权限判断不用再查库
     * @return JWT 字符串
     */
    public String generateToken(Long userId, Integer role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))//主体：用户 id
                .claim("role", role)//自定义声明：角色
                .issuedAt(Date.from(now))//签发时间
                .expiration(Date.from(now.plus(properties.getExpireDays(), ChronoUnit.DAYS)))//过期时间
                .signWith(key())//用密钥签名，防篡改
                .compact();
    }

    /**
     * 解析并校验签名。
     * @param token 前端传来的 JWT
     * @return token 里的声明（含 userId、role）
     * @throws io.jsonwebtoken.JwtException token 无效、过期或被篡改时抛出
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())//用同一个密钥验签
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
