package com.storysliver.pojo.Admin;

import lombok.Data;

/**
 * 封禁账号请求体。
 * 干什么用：管理员封禁时指定天数；不填或 ≤0 表示永久封禁。
 */
@Data
public class BanUserRequest {
    private Integer days;//封禁天数（null 或 ≤0 = 永久）
}
