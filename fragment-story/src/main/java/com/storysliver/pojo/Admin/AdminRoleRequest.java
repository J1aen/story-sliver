package com.storysliver.pojo.Admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户角色请求体。
 * 干什么用：站长在管理页把普通用户设为管理员、或撤销管理员时提交的 body。
 * 为什么 @NotNull：role 必填，防止传空导致 Service 里出现 null 判断遗漏。
 * 注意：role 只允许 0（普通）或 1（管理员），站长（2）不可被修改——由 Service 校验。
 */
@Data
public class AdminRoleRequest {
    @NotNull(message = "角色不能为空")
    private Integer role;//目标角色：0 普通用户 / 1 管理员
}
