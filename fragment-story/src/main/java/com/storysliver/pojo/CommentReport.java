package com.storysliver.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论举报实体：对应 comment_report 表（v2.0 Task 21）。
 * 干什么用：记录「谁举报了哪条评论、为什么」，管理端据此处理（下架/封禁）。
 * 为什么带冗余字段：管理端列表要一次查出评论内容、评论者昵称等，用 join 拼好，展示时不再逐条查库。
 */
@Data
public class CommentReport {
    private Long id;//举报 id
    private Long commentId;//被举报评论 id
    private Long reporterId;//举报人 id
    private String reason;//举报理由（必填）
    private Integer status;//0 待处理 1 已处理
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;//处理时间（处理完成后写入）

    // —— 以下为管理端列表的冗余字段（join 查出来，仅展示/操作使用，不落库）——
    private String commentContent;//被举报评论的内容
    private Long fragmentId;//被举报评论所属碎片 id
    private Long commenterId;//评论者用户 id（「封禁该用户」时用）
    private String commenterName;//评论者昵称
}
