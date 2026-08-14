package com.storysliver.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体：对应 comment 表（v2.0 Task 21）。
 * 干什么用：承载评论内容与归属（挂在哪个碎片下、谁发的），在 Mapper、Service 之间传递。
 * 为什么不需要更新时间：评论发布后不可编辑，只有删除，一个 created_at 足够。
 */
@Data
public class Comment {
    private Long id;//主键
    private Long fragmentId;//所属碎片 id（评论挂在碎片上）
    private Long userId;//评论者 id（数据库记录真实身份，展示时查昵称）
    private String content;//评论内容（≤100 字）
    private Integer status;//状态：0 正常 1 已下架（当前管理端下架=硬删除，此字段预留）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;//创建时间（列表按它正序展示）
}
