package com.storysliver.config;

import com.storysliver.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：拦截器注册 + 跨域。
 * 为什么排除 /api/auth/* 和 GET /api/fragments：验证码/注册/登录是公开接口；
 * 首页列表游客可见，登录用户在 Controller 里「可选解析」token（见 Task 7 FragmentController）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /** 注册 JWT 拦截器：/api/** 都拦截，公开接口排除 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/captcha",//验证码：注册前必须先拿
                        "/api/auth/register",//注册：还没有账号
                        "/api/auth/login");//登录：还没有 token
        // 注意：首页列表 GET /api/fragments 的「游客放行」在 AuthInterceptor 里按方法判断，
        // 不能在这里排除路径——否则 POST 发布接口也会被放行（没有登录用户）
    }

    /** 开发阶段前后端分端口运行，允许任意来源；上线后同端口托管，此配置不再生效 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
