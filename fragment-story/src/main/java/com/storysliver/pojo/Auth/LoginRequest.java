package com.storysliver.pojo.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体：用户名 + 密码。
 * 干什么用：@RequestBody 接收登录表单，@NotBlank 在进 Service 前拦截空值。
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;//用户名

    @NotBlank(message = "密码不能为空")
    private String password;//密码
}
