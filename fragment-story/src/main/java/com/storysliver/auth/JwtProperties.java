package com.storysliver.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性：读取 application.properties 里 app.jwt.* 开头的配置。
 * 干什么用：把签名密钥和有效期从代码里抽出来，部署时只改配置、不改代码。
 * 为什么这样设计：密钥是敏感信息，放在配置文件里方便按环境切换。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;//签名密钥：HS256 要求至少 32 字节，生产环境必须改成随机长字符串
    private int expireDays;//token 有效期（天），到期需重新登录
}
