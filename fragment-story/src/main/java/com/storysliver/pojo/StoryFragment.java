package com.storysliver.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事碎片实体：对应 story_fragment 表。
 * 干什么用：承载用户发布的故事/生活碎片及其生命周期状态。
 * 为什么这样设计：user_id 与 is_anonymous 分离——匿名只影响展示，数据库始终记录真实发布者，管理员可追溯。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoryFragment {
    /** 状态常量：0 待审核（提交后默认）、1 已发布（管理员通过后）、2 已隐藏（作者隐藏，可恢复） */
    public static final int STATUS_PENDING = 0, STATUS_PUBLISHED = 1, STATUS_HIDDEN = 2;

    private Long id;//主键
    private Long userId;//真实发布者id（匿名只影响展示）
    private String content;//碎片内容（1000字封顶）
    private Integer likeCount;//点赞数（冗余计数）
    private Integer isAnonymous;//是否匿名：0显示昵称 1显示「匿名用户」
    private Integer status;//生命周期状态：0待审核 1已发布 2已隐藏
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;//创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;//更新时间
}
