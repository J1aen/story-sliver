package com.storysliver.controller;

import com.storysliver.auth.RequireRole;
import com.storysliver.auth.UserContext;
import com.storysliver.pojo.Admin.AdminRoleRequest;
import com.storysliver.pojo.Admin.UpdateAdminCodeRequest;
import com.storysliver.pojo.Result;
import com.storysliver.pojo.User;
import com.storysliver.service.AdminService;
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

/**
 * 管理接口：审核、删除、用户管理、配置管理。
 * 权限通过 @RequireRole 声明（AuthInterceptor 执行）：
 *   - 管理员及以上（1/2）：审核队列、审核通过、删除碎片
 *   - 仅站长（2）：用户列表、指定/撤销管理员、修改管理员注册密码
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;//管理业务

    /** 管理列表 / 审核队列：status 传 0 看待审核，传 1 看已发布，不传看全部 */
    @GetMapping("/fragments")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result list(@RequestParam(required = false) Integer status,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(adminService.manageList(status, pageNum, pageSize));
    }

    /** 审核通过：待审核 → 已发布（碎片上墙） */
    @PostMapping("/fragments/{id}/approve")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result approve(@PathVariable Long id) {
        adminService.approve(id);
        return Result.success();
    }

    /** 硬删除：审核不通过 / 删除违规碎片（前端弹窗确认不可恢复） */
    @DeleteMapping("/fragments/{id}")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result delete(@PathVariable Long id) {
        adminService.deleteFragment(id);
        return Result.success();
    }

    /** 用户列表：仅站长（用户管理 Tab） */
    @GetMapping("/users")
    @RequireRole(User.ROLE_OWNER)
    public Result users(@RequestParam(defaultValue = "1") int pageNum,
                        @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(adminService.users(pageNum, pageSize));
    }

    /** 指定 / 撤销管理员：仅站长；操作者角色从 UserContext 取（token 里带的） */
    @PutMapping("/users/{id}/role")
    @RequireRole(User.ROLE_OWNER)
    public Result updateRole(@PathVariable Long id, @Valid @RequestBody AdminRoleRequest request) {
        adminService.updateRole(UserContext.getRole(), id, request.getRole());
        return Result.success();
    }

    /** 修改管理员注册特殊密码：仅站长 */
    @PutMapping("/config/admin-register-code")
    @RequireRole(User.ROLE_OWNER)
    public Result updateAdminCode(@Valid @RequestBody UpdateAdminCodeRequest request) {
        adminService.updateAdminCode(UserContext.getRole(), request.getNewCode());
        return Result.success();
    }

    /** 待审核头像队列（管理员及以上）：查看谁的头像在等审核 */
    @GetMapping("/avatars")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result avatars(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(adminService.pendingAvatars(pageNum, pageSize));
    }

    /** 头像审核通过：待审核头像转正 */
    @PostMapping("/avatars/{userId}/approve")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result approveAvatar(@PathVariable Long userId) {
        adminService.approveAvatar(userId);
        return Result.success();
    }

    /** 头像审核拒绝：清空待审核头像，保留旧头像 */
    @DeleteMapping("/avatars/{userId}/reject")
    @RequireRole({User.ROLE_ADMIN, User.ROLE_OWNER})
    public Result rejectAvatar(@PathVariable Long userId) {
        adminService.rejectAvatar(userId);
        return Result.success();
    }
}
