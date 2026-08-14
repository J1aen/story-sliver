package com.storysliver.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告实体：对应 announcement 表（v1.2）。
 * 干什么用：承载站长发布的公告（标题/正文/图片/上下架状态），进站弹窗展示。
 */
@Data
public class Announcement {
    private Long id;//主键
    private String title;//公告标题
    private String content;//公告正文
    private String imageUrl;//公告图片 URL（可空）
    private Integer status;//0下架 1上架
    private LocalDateTime createdAt;//创建时间
    private LocalDateTime updatedAt;//更新时间
}
