package com.storysliver.service;

import com.storysliver.pojo.Admin.AnnouncementRequest;
import com.storysliver.pojo.Announcement;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 公告服务接口（v1.2）。
 * 干什么用：公开接口取上架公告；站长管理后台增删改/上下架/传图。
 */
public interface AnnouncementService {

    /** 最新一条上架公告（没有则返回 null，前端不弹） */
    Announcement getActive();

    /** 全部公告（管理后台列表） */
    List<Announcement> listAll();

    /** 新建公告（默认下架，站长手动上架） */
    void create(AnnouncementRequest request);

    /** 编辑公告 */
    void update(Long id, AnnouncementRequest request);

    /** 上架/下架 */
    void updateStatus(Long id, Integer status);

    /** 删除公告 */
    void delete(Long id);

    /** 上传公告图片（站长），返回可访问 URL */
    String uploadImage(MultipartFile file);
}
