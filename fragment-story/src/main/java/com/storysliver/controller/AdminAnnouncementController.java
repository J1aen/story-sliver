package com.storysliver.controller;

import com.storysliver.auth.RequireRole;
import com.storysliver.pojo.Admin.AnnouncementRequest;
import com.storysliver.pojo.Result;
import com.storysliver.pojo.User;
import com.storysliver.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 公告管理接口（v1.2）：只有站长能用。
 * 为什么 @RequireRole(User.ROLE_OWNER)：公告是站长的「官方发声」，管理员也不能编辑（需求明确）。
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;//公告业务

    /** 全部公告列表（含下架，倒序） */
    @GetMapping
    @RequireRole(User.ROLE_OWNER)
    public Result list() {
        return Result.success(announcementService.listAll());
    }

    /** 新建公告（默认下架，需手动上架） */
    @PostMapping
    @RequireRole(User.ROLE_OWNER)
    public Result create(@Valid @RequestBody AnnouncementRequest request) {
        announcementService.create(request);
        return Result.success();
    }

    /** 编辑公告（标题/正文/图片） */
    @PutMapping("/{id}")
    @RequireRole(User.ROLE_OWNER)
    public Result update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        announcementService.update(id, request);
        return Result.success();
    }

    /** 上架/下架：status=1 上架，status=0 下架 */
    @PutMapping("/{id}/status")
    @RequireRole(User.ROLE_OWNER)
    public Result updateStatus(@PathVariable Long id, @RequestParam("status") Integer status) {
        announcementService.updateStatus(id, status);
        return Result.success();
    }

    /** 删除公告 */
    @DeleteMapping("/{id}")
    @RequireRole(User.ROLE_OWNER)
    public Result delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }

    /** 上传公告图片：返回 { url } 给前端回填到表单 */
    @PostMapping("/upload-image")
    @RequireRole(User.ROLE_OWNER)
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(Map.of("url", announcementService.uploadImage(file)));
    }
}
