package com.storysliver.pojo.Admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告编辑请求体（v1.2）。
 * 干什么用：站长新建/编辑公告时提交的数据（标题/正文/图片），校验在注解上完成。
 */
@Data
public class AnnouncementRequest {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题最长 50 字")
    private String title;//公告标题

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 2000, message = "公告内容最长 2000 字")
    private String content;//公告正文

    private String imageUrl;//公告图片 URL（可空）
}
