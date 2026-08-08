package com.storysliver.pojo.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求体：承载前端注册表单的完整字段。
 * 干什么用：Controller 用 @RequestBody 接收 JSON 后自动转成这个对象；
 * 校验注解会在进入 Service 之前自动拦截非法输入。
 * 为什么在字段上加校验注解而不是在 Service 里手写 if：
 * Spring 的 @Valid 会自动执行这些注解，代码更少、错误信息更统一。
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名最长 32 位")
    private String username;//登录用户名（唯一，注册时查重）

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长 32 位")
    private String nickname;//展示昵称（非匿名碎片对外显示）

    @NotBlank(message = "密码不能为空")
    private String password;//明文密码（Service 里 BCrypt 加密后落库，绝不存明文）

    @NotBlank(message = "验证码不能为空")
    private String captchaKey;//验证码 key（来自 GET /api/auth/captcha 接口）

    @NotBlank(message = "验证码答案不能为空")
    private String captchaAnswer;//用户输入的验证码答案（算术题结果）

    private Boolean isAdmin = false;//是否勾选「注册为管理员」，默认普通用户

    private String adminCode;//勾选管理员时必填：站长才知道的特殊注册密码
}
