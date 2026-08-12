package com.storysliver.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.FragmentLikeMapper;
import com.storysliver.mapper.StoryFragmentMapper;
import com.storysliver.mapper.SystemConfigMapper;
import com.storysliver.mapper.UserMapper;
import com.storysliver.pojo.Fragment.FragmentVO;
import com.storysliver.pojo.PageBean;
import com.storysliver.pojo.StoryFragment;
import com.storysliver.pojo.SystemConfig;
import com.storysliver.pojo.User;
import com.storysliver.service.AdminService;
import com.storysliver.service.FragmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理服务实现：审核、硬删除、用户角色、管理员注册密码。
 * 所有方法都先校验操作者角色（纵深防御，配合 Controller 的 @RequireRole），再由 Mapper 执行。
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private StoryFragmentMapper fragmentMapper;//碎片表操作：审核列表、改状态、删行

    @Autowired
    private FragmentLikeMapper likeMapper;//点赞表操作：硬删除时清理点赞

    @Autowired
    private UserMapper userMapper;//用户表操作：分页、改角色、查发布者

    @Autowired
    private FragmentService fragmentService;//复用 FragmentService.toVO 转展示对象（匿名逻辑）

    @Autowired
    private SystemConfigMapper systemConfigMapper;//系统配置表：存管理员注册特殊密码

    @Autowired
    private PasswordEncoder passwordEncoder;//BCrypt 加密器：新密码加密后落库

    /**
     * 管理列表：按状态筛选（0 待审核 / 1 已发布 / null 全部）。
     * 为什么匿名也显示真实昵称：管理员审核/追责时要知道是谁发的，匿名只对普通用户生效。
     */
    @Override
    public PageBean manageList(Integer status, int pageNum, int pageSize) {
        // PageHelper：查询前 startPage，给下一条查询自动拼 LIMIT 并返回带 total 的 Page
        PageHelper.startPage(pageNum, pageSize);
        // 动态 SQL（按状态筛选）在 StoryFragmentMapper.xml 里
        // 注意：必须先强转 Page 再 stream——toList() 会生成普通 ArrayList，没法再转 Page
        List<StoryFragment> fragments = fragmentMapper.selectManagePage(status);
        Page<StoryFragment> page = (Page<StoryFragment>) fragments;
        List<FragmentVO> list = page.getResult().stream()
                .map(this::toAdminVO)
                .toList();
        return new PageBean(page.getTotal(), list);
    }

    /**
     * 审核通过：把「待审核（0）」变成「已发布（1）」。
     * 为什么限制只能审核待审核的：防止重复审核、防止把已删除/已隐藏的碎片改上墙。
     */
    @Override
    public void approve(Long fragmentId) {
        StoryFragment f = requireExists(fragmentId);
        if (f.getStatus() != StoryFragment.STATUS_PENDING) {
            throw new BusinessException(ResultCode.FRAGMENT_NOT_PUBLISHED, "只能审核待审核碎片");
        }
        fragmentMapper.updateStatus(fragmentId, StoryFragment.STATUS_PUBLISHED);
    }

    /**
     * 硬删除：审核不通过 / 删除违规碎片共用。
     * 为什么清点赞：避免留下指向不存在碎片的孤儿点赞记录。
     */
    @Override
    public void deleteFragment(Long fragmentId) {
        requireExists(fragmentId);
        fragmentMapper.deleteById(fragmentId);
        // userId 传 null 表示清理该碎片全部点赞（Mapper XML 用 <if> 处理）
        likeMapper.deleteByFragmentAndUser(fragmentId, null);
    }

    /** 用户分页列表（仅站长）：管理页展示所有用户及其角色 */
    @Override
    public PageBean users(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = userMapper.selectPage();
        Page<User> page = (Page<User>) list;
        // 安全：密码哈希绝不能返回给前端（管理员页只需要 id/用户名/昵称/角色）
        page.getResult().forEach(u -> u.setPassword(null));
        return new PageBean(page.getTotal(), page.getResult());
    }

    /**
     * 指定 / 撤销管理员：只有站长能操作。
     * 三层校验：操作者是站长 → 目标角色只能是 0/1 → 目标不是站长本人（防止权限被锁死）。
     */
    @Override
    public void updateRole(Integer operatorRole, Long targetUserId, Integer role) {
        if (operatorRole == null || operatorRole != User.ROLE_OWNER) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (role == null || (role != User.ROLE_USER && role != User.ROLE_ADMIN)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色只能是 0 或 1");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 站长不能被任何人改（包括站长自己），保证系统永远有站长
        if (target.getRole() == User.ROLE_OWNER) {
            throw new BusinessException(ResultCode.CANNOT_MODIFY_OWNER);
        }
        userMapper.updateRole(targetUserId, role);
    }

    /**
     * 修改管理员注册特殊密码：只有站长能操作。
     * 新密码 BCrypt 加密后写入 system_config，注册页「注册为管理员」立即用新密码校验。
     */
    @Override
    public void updateAdminCode(Integer operatorRole, String newCode) {
        if (operatorRole == null || operatorRole != User.ROLE_OWNER) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (newCode == null || newCode.length() < 6) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "管理员注册密码至少 6 位");
        }
        systemConfigMapper.upsert(SystemConfig.KEY_ADMIN_REGISTER_CODE, passwordEncoder.encode(newCode));
    }

    /**
     * 待审核头像队列：查出 avatar_pending 不为空的用户。
     * 为什么密码要置空：管理员页只需要看头像和昵称，密码哈希绝不能返回。
     */
    @Override
    public PageBean pendingAvatars(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = userMapper.selectPendingAvatarUsers();
        Page<User> page = (Page<User>) list;
        page.getResult().forEach(u -> u.setPassword(null));
        return new PageBean(page.getTotal(), page.getResult());
    }

    /** 头像审核通过：待审核头像转正（先确认用户和待审核头像都存在） */
    @Override
    public void approveAvatar(Long userId) {
        requirePendingAvatar(userId);
        userMapper.approveAvatar(userId);
    }

    /** 头像审核拒绝：清空待审核头像，保留旧头像 */
    @Override
    public void rejectAvatar(Long userId) {
        requirePendingAvatar(userId);
        userMapper.clearAvatarPending(userId);
    }

    /** 校验用户存在且有「待审核头像」 */
    private void requirePendingAvatar(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (u.getAvatarPending() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该用户没有待审核头像");
        }
    }

    /** 校验碎片存在：审核 / 删除的前置条件 */
    private StoryFragment requireExists(Long fragmentId) {
        StoryFragment f = fragmentMapper.selectById(fragmentId);
        if (f == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "碎片不存在");
        }
        return f;
    }

    /**
     * 管理端专用转换：无论是否匿名都显示真实昵称。
     * 为什么不用 fragmentService.toVO 直接返回：toVO 对匿名显示「匿名用户」，
     * 管理员需要看到真实发布者，所以这里覆盖 authorName。
     */
    private FragmentVO toAdminVO(StoryFragment f) {
        FragmentVO vo = fragmentService.toVO(f, false);
        User author = userMapper.selectById(f.getUserId());
        vo.setAuthorName(author == null ? "未知用户" : author.getNickname());
        return vo;
    }
}
