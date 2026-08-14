package com.storysliver.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 以下为 Task 5 起新增的业务错误码
    CAPTCHA_ERROR(400, "验证码错误"),
    REGISTER_TOO_FREQUENT(429, "注册过于频繁，请稍后再试"),
    USERNAME_TAKEN(400, "用户名已存在"),
    USERNAME_INVALID(400, "用户名只能包含字母、数字和下划线（4-20位），不能有汉字"),
    NICKNAME_TAKEN(400, "昵称已被使用"),
    NICKNAME_SENSITIVE(400, "昵称包含敏感词，请修改"),
    PASSWORD_TOO_WEAK(400, "密码至少 6 位"),
    OLD_PASSWORD_WRONG(400, "旧密码不正确"),
    ADMIN_CODE_WRONG(400, "管理员注册密码错误"),
    LOGIN_FAILED(401, "用户名或密码错误"),
    ACCOUNT_BANNED(403, "账号已被封禁，请联系站长"),
    FRAGMENT_TOO_FREQUENT(429, "发布太频繁，5 分钟后再试"),
    FRAGMENT_TOO_LONG(400, "内容不能超过 1000 字"),
    ALREADY_LIKED(400, "已经点过赞了"),
    NOT_LIKED(400, "还没有点赞"),
    FRAGMENT_NOT_PUBLISHED(400, "碎片不存在或不可操作"),
    NOT_AUTHOR(403, "只能操作自己的碎片"),
    CANNOT_MODIFY_OWNER(400, "不能修改站长"),
    AVATAR_TOO_SMALL(400, "图片太小，请选择尺寸更大的图片"),
    ANNOUNCEMENT_NOT_FOUND(404, "公告不存在"),

    // 以下为 v2.0 Task 21 评论相关错误码
    COMMENT_EMPTY(400, "评论内容不能为空"),
    COMMENT_TOO_LONG(400, "评论不能超过 100 字"),
    COMMENT_NOT_FOUND(404, "评论不存在"),
    COMMENT_TOO_FREQUENT(429, "评论太频繁，请 1 分钟后再试"),
    REPORT_REASON_REQUIRED(400, "举报理由必填"),
    REPORT_DUPLICATE(400, "已经举报过这条评论了");

    private final int code;
    private final String message;
}
