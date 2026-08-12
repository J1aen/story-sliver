package com.storysliver.pojo.Admin;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 封禁账号请求体。
 * 干什么用：管理员封禁时指定天数；不填或 ≤0 表示永久封禁。
 */
@Data
public class BanUserRequest {
    private Integer days;//封禁天数（null 或 ≤0 = 永久）
    @NotBlank(message = "封禁理由不能为空")
    private String reason;//封禁理由（必填，被封禁用户可见）
}
