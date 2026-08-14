package com.storysliver.pojo.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称请求体（v1.2）。
 * 干什么用：设置下拉里「修改昵称」弹窗提交的数据；@Valid 在 Controller 自动校验。
 * 为什么放 pojo.Auth 包：与 LoginRequest/RegisterRequest 同属「账号相关请求」，聚在一起好找。
 */
@Data
public class UpdateNicknameRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长 32 位")
    private String nickname;//新昵称（需唯一且不含敏感词）
}
