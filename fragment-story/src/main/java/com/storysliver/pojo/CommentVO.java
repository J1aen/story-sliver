package com.storysliver.pojo;

import lombok.Data;

/**
 * 评论展示对象：返回给前端的评论信息。
 * 为什么用 VO 而不是直接返回实体：不暴露 user_id 等敏感字段，还能拼好展示名与「是否自己的评论」。
 */
@Data
public class CommentVO {
    private Long id;//评论 id
    private String content;//评论内容
    private String authorName;//评论者昵称（用户不存在时显示「已注销」）
    private String authorAvatar;//评论者已审核头像 URL（可空）
    private Integer authorRole;//评论者角色：0 普通 1 管理员 2 站长（前端据此显示铭牌）
    private Boolean mine;//是否当前登录用户自己的评论（前端显示「删除」按钮）
    private String createdAt;//格式化后的创建时间（yyyy-MM-dd HH:mm:ss）
}
