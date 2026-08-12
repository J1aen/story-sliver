package com.storysliver.pojo.Admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改「管理员注册特殊密码」请求体。
 * 干什么用：站长在管理页输入新密码，改完后注册页的「注册为管理员」就用新密码校验。
 * 为什么 @NotBlank：新密码必填；长度至少 6 位由 Service 校验（和注册密码强度一致）。
 */
@Data
public class UpdateAdminCodeRequest {
    @NotBlank(message = "新密码不能为空")
    private String newCode;//新的管理员注册特殊密码
}
