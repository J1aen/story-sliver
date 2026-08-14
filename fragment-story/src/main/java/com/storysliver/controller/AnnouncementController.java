package com.storysliver.controller;

import com.storysliver.pojo.Result;
import com.storysliver.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告公开接口（v1.2）。
 * 干什么用：前端进入网站时拉取「最新一条上架公告」用于弹窗；
 * 为什么公开：游客也要能看到公告，此路径已在 WebConfig 排除 JWT 拦截。
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;//公告业务

    /**
     * 获取当前上架公告。
     * @return 最新一条上架公告；没有上架公告时 data 为 null（前端不弹窗）
     */
    @GetMapping("/active")
    public Result active() {
        return Result.success(announcementService.getActive());
    }
}
