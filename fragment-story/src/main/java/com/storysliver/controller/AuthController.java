package com.storysliver.controller;

import com.storysliver.auth.CaptchaService;
import com.storysliver.pojo.Auth.LoginRequest;
import com.storysliver.pojo.Auth.RegisterRequest;
import com.storysliver.pojo.Result;
import com.storysliver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口：验证码 / 注册 / 登录。
 * 这三个接口是公开的——WebConfig 里被 JWT 拦截器排除，因为注册/登录时用户还没有 token。
 * 为什么用 @Slf4j：照 sims 项目风格，Controller 都带上日志对象，排查问题时直接 log.info。
 */
@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;//注册/登录业务

    @Autowired
    private CaptchaService captchaService;//验证码生成

    /**
     * 获取注册验证码。
     * @return CaptchaResponse（captchaKey + 图片 base64），前端注册时把 key 和答案一起带回
     */
    @GetMapping("/captcha")
    public Result captcha() {
        return Result.success(captchaService.generate());
    }

    /**
     * 注册。
     * @param request 注册表单（@Valid 会自动执行字段上的校验注解）
     * @param http 原始 HTTP 请求，用来取客户端 IP（注册限流要用）
     * @return { token }，注册成功即自动登录
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        String ip = clientIp(http);// 先取真实 IP
        return Result.success(Map.of("token", userService.register(request, ip)));
    }

    /**
     * 登录。
     * @param request 用户名 + 密码
     * @return { token }，前端保存后后续请求带上
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest request) {
        return Result.success(Map.of("token", userService.login(request.getUsername(), request.getPassword())));
    }

    /**
     * 获取客户端真实 IP。
     * 为什么优先取 X-Forwarded-For：生产环境前面有 Nginx/负载均衡时，getRemoteAddr 拿到的是代理的 IP；
     * 取第一个值是因为代理链里第一个才是真实客户端（上线时要在 Nginx 配置里覆盖该头，防止伪造）。
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
