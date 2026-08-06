package com.storysliver.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体：对应 user 表。
 * 干什么用：承载注册/登录用户的全部字段，在 Mapper、Service、Controller 之间传递。
 * 为什么这样设计：字段与表一一对应，驼峰命名由 MyBatis map-underscore-to-camel-case 自动映射。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    /** 角色常量：0 普通用户（默认）、1 管理员、2 站长。用 int 存库简单、可扩展 */
    public static final int ROLE_USER = 0, ROLE_ADMIN = 1, ROLE_OWNER = 2;
    /** 状态常量：0 正常、1 封禁（预留，以后做封号功能时用） */
    public static final int STATUS_NORMAL = 0, STATUS_BANNED = 1;

    private Long id;//主键
    private String username;//登录用户名（唯一）
    private String nickname;//展示昵称
    private String password;//BCrypt 密码哈希，绝不存明文
    private String email;//预留邮箱（暂不验证，以后接 QQ 邮箱验证）
    private Integer role;//角色：0普通 1管理员 2站长
    private Integer status;//状态：0正常 1封禁（预留）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;//创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;//更新时间
}
