package com.storysliver.service;

import com.storysliver.pojo.Fragment.FragmentVO;
import com.storysliver.pojo.PageBean;

/**
 * 管理服务接口：审核、删除、用户管理、配置管理。
 * 为什么权限判断放在 Service 里：Controller 的 @RequireRole 只挡路由入口，
 * Service 里再校验一次是纵深防御——即使有人绕过 Controller 直接调 Service 也拦得住。
 */
public interface AdminService {

    /** 管理列表（按状态筛选：0 待审核 / 1 已发布；null 全部），匿名碎片也显示真实昵称 */
    PageBean manageList(Integer status, int pageNum, int pageSize);

    /** 审核通过：只允许把「待审核」置为「已发布」 */
    void approve(Long fragmentId);

    /** 硬删除：审核不通过 / 删除违规碎片共用（删行并清理点赞） */
    void deleteFragment(Long fragmentId);

    /** 用户分页列表（仅站长） */
    PageBean users(int pageNum, int pageSize);

    /** 指定 / 撤销管理员（仅站长；不能修改站长本人） */
    void updateRole(Integer operatorRole, Long targetUserId, Integer role);

    /** 修改管理员注册特殊密码（仅站长，BCrypt 哈希落库） */
    void updateAdminCode(Integer operatorRole, String newCode);

    /** 待审核头像队列（分页；头像也是内容，需要管理员审核） */
    PageBean pendingAvatars(int pageNum, int pageSize);

    /** 头像审核通过：待审核头像转正为当前头像 */
    void approveAvatar(Long userId);

    /** 头像审核拒绝：清空待审核头像（保留旧头像），记录拒绝原因 */
    void rejectAvatar(Long userId, String reason);

    /** 封禁账号（管理员及以上；站长不可被封禁）。days 为空或 ≤0 = 永久封禁 */
    void banUser(Integer operatorRole, Long userId, Integer days);

    /** 解除封禁（仅站长） */
    void unbanUser(Integer operatorRole, Long userId);
}
