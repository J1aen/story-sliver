package com.storysliver.pojo.Fragment;

import lombok.Data;

/**
 * 碎片展示对象：返回给前端的碎片信息。
 * 为什么用 VO 而不是直接返回实体：不把 user_id 等敏感字段暴露给前端，还能拼好展示名。
 */
@Data
public class FragmentVO {
    private Long id;//主键
    private String content;//碎片内容
    private Integer likeCount;//点赞数
    private Integer isAnonymous;//是否匿名：0显示昵称 1显示「匿名用户」
    private Integer status;//生命周期：0待审核 1已发布 2已隐藏
    private String authorName;//展示名（匿名=「匿名用户」，实名=昵称）
    private Integer authorRole;//发布者角色：0普通 1管理员 2站长（匿名时为 null，不显示铭牌）
    private Boolean likedByMe;//当前登录用户是否已赞（列表接口填充）
    private String createdAt;//格式化后的创建时间（yyyy-MM-dd HH:mm:ss）
}
