package com.storysliver.pojo;

import lombok.Data;

/**
 * 发评论请求体（Task 21）。
 * 干什么用：接收前端 POST /api/comments 的 JSON：{ fragmentId, content }。
 * 为什么放在 pojo 根目录而不是 pojo/Comment 子包：实体类 Comment 已占用 pojo.Comment 这个名字，
 * 同名类与子包冲突会导致编译失败，所以请求体放到根包（HandleReportRequest 仍在 pojo/Admin 下）。
 */
@Data
public class CommentRequest {
    private Long fragmentId;//评论的碎片 id（挂在哪条碎片下）
    private String content;//评论内容（≤100 字）
}
