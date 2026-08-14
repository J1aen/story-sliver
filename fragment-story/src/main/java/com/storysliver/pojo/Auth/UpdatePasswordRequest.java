package com.storysliver.pojo.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求体（v1.2）。
 * 干什么用：设置下拉里「修改密码」弹窗提交的数据；新密码强度在 Service 校验。
 */
@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;//旧密码（必须正确才能改）

    @NotBlank(message = "新密码不能为空")
    private String newPassword;//新密码（至少 6 位，BCrypt 加密落库）
}
