package com.storysliver.controller;

import com.storysliver.auth.RequireRole;
import com.storysliver.pojo.Result;
import com.storysliver.pojo.User;
import com.storysliver.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 敏感词管理接口（v1.2）：只有站长能用。
 * 为什么站长专属：敏感词规则影响全站注册/改昵称，权限必须收敛到最高权限。
 */
@RestController
@RequestMapping("/api/admin/sensitive-words")
public class AdminSensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;//敏感词业务

    /** 敏感词列表（管理后台展示） */
    @GetMapping
    @RequireRole(User.ROLE_OWNER)
    public Result list() {
        return Result.success(sensitiveWordService.listAll());
    }

    /** 新增敏感词：body 形如 { "word": "xxx" } */
    @PostMapping
    @RequireRole(User.ROLE_OWNER)
    public Result add(@RequestBody Map<String, String> body) {
        sensitiveWordService.add(body.get("word"));
        return Result.success();
    }

    /** 删除敏感词 */
    @DeleteMapping("/{id}")
    @RequireRole(User.ROLE_OWNER)
    public Result delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.success();
    }
}
