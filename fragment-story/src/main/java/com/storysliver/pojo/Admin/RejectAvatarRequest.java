package com.storysliver.pojo.Admin;

import lombok.Data;

/**
 * 头像审核拒绝请求体。
 * 干什么用：管理员拒绝头像时填写原因，用户端能看到「为什么被拒」。
 * 为什么 reason 可空：不填时后端用默认文案「头像不符合要求」。
 */
@Data
public class RejectAvatarRequest {
    private String reason;//拒绝原因（可选）
}
